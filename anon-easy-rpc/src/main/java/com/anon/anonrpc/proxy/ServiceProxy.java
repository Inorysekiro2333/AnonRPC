package com.anon.anonrpc.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.anon.anonrpc.fault.CircuitBreaker;
import com.anon.anonrpc.fault.FallbackHandler;
import com.anon.anonrpc.fault.RetryMechanism;
import com.anon.anonrpc.model.RpcRequest;
import com.anon.anonrpc.model.RpcResponse;
import com.anon.anonrpc.serializer.JdkSerializer;
import com.anon.anonrpc.serializer.Serializer;
import com.anon.anonrpc.registry.ServiceRegistry;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 服务代理（JDK动态代理）
 */
public class ServiceProxy implements InvocationHandler {

    // 调用超时时间（毫秒）
    private static final int TIMEOUT_MS = 3000;

    /**
     * 调用代理
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 从服务注册中心获取下一个服务实例
        String serviceUrl = ServiceRegistry.getNextServiceUrl();
        
        // 如果熔断器显示服务不可用，直接返回降级结果
        if (!CircuitBreaker.isAvailable(serviceUrl)) {
            System.out.println("服务 " + serviceUrl + " 已熔断，使用降级处理");
            return FallbackHandler.getFallbackResult(method, args);
        }
        
        try {
            // 使用重试机制执行RPC调用
            return RetryMechanism.executeWithRetry(() -> {
                try {
                    return doInvoke(serviceUrl, method, args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, serviceUrl);
        } catch (Exception e) {
            System.err.println("所有重试都失败，使用降级处理");
            return FallbackHandler.getFallbackResult(method, args);
        }
    }
    
    /**
     * 执行实际的RPC调用
     */
    private Object doInvoke(String serviceUrl, Method method, Object[] args) throws Exception {
        // 指定序列化器
        Serializer serializer = new JdkSerializer();
        // 构造请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        
        System.out.println("正在连接RPC服务：" + serviceUrl);
        
        // 序列化
        byte[] bodyBytes = serializer.serialize(rpcRequest);
        
        // 发送请求，设置超时时间
        try (HttpResponse httpResponse = HttpRequest.post(serviceUrl)
                .body(bodyBytes)
                .timeout(TIMEOUT_MS) // 设置超时时间
                .execute()) {
            
            // 检查HTTP状态码
            int status = httpResponse.getStatus();
            System.out.println("HTTP响应状态码: " + status);
            
            // 获取响应内容
            byte[] result = httpResponse.bodyBytes();
            if (result == null || result.length == 0) {
                System.err.println("警告: 服务器返回了空响应");
                throw new IOException("服务器返回空响应");
            }
            
            System.out.println("收到响应数据大小: " + result.length + " 字节");
            
            // 反序列化
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
            return rpcResponse.getData();
        }
    }
}
