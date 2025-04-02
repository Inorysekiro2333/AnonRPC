package com.anon.anonrpc.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.anon.anonrpc.config.RpcConfig;
import com.anon.anonrpc.fault.CircuitBreaker;
import com.anon.anonrpc.fault.FallbackHandler;
import com.anon.anonrpc.model.RpcRequest;
import com.anon.anonrpc.model.RpcResponse;
import com.anon.anonrpc.registry.ServiceRegistry;
import com.anon.anonrpc.serializer.JdkSerializer;
import com.anon.anonrpc.serializer.Serializer;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 异步服务代理
 */
public class AsyncServiceProxy implements InvocationHandler {

    // 线程池处理异步任务
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    
    // 序列化器
    private final Serializer serializer = new JdkSerializer();
    
    /**
     * 异步调用代理
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 检查方法返回类型是否为CompletableFuture
        boolean isAsyncMethod = CompletableFuture.class.isAssignableFrom(method.getReturnType());
        
        // 对于同步方法，等待异步调用完成
        if (!isAsyncMethod) {
            CompletableFuture<Object> future = invokeAsync(method, args);
            try {
                return future.get(RpcConfig.getTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                return FallbackHandler.getFallbackResult(method, args);
            }
        }
        
        // 对于异步方法，直接返回CompletableFuture
        return invokeAsync(method, args);
    }
    
    /**
     * 执行异步RPC调用
     */
    private CompletableFuture<Object> invokeAsync(Method method, Object[] args) {
        // 从注册中心获取服务URL
        String serviceUrl = ServiceRegistry.getNextServiceUrl();
        
        // 检查熔断器状态
        if (!CircuitBreaker.isAvailable(serviceUrl)) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.complete(FallbackHandler.getFallbackResult(method, args));
            return future;
        }
        
        // 构造RPC请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        
        // 返回异步CompletableFuture
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 序列化请求
                byte[] bodyBytes = serializer.serialize(rpcRequest);
                
                // 异步HTTP请求
                try (HttpResponse httpResponse = HttpRequest.post(serviceUrl)
                        .body(bodyBytes)
                        .timeout(RpcConfig.getTimeoutMs())
                        .execute()) {
                    
                    int status = httpResponse.getStatus();
                    if (status != 200) {
                        throw new IOException("HTTP请求失败，状态码: " + status);
                    }
                    
                    // 获取响应
                    byte[] result = httpResponse.bodyBytes();
                    if (result == null || result.length == 0) {
                        throw new IOException("服务器返回空响应");
                    }
                    
                    // 反序列化响应
                    RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
                    
                    // 记录成功
                    CircuitBreaker.recordSuccess(serviceUrl);
                    
                    return rpcResponse.getData();
                }
            } catch (Exception e) {
                // 记录失败
                CircuitBreaker.recordFailure(serviceUrl);
                throw new RuntimeException("RPC调用失败: " + e.getMessage(), e);
            }
        }, EXECUTOR);
    }
} 