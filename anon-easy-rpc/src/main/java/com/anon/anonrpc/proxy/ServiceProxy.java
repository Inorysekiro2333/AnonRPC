package com.anon.anonrpc.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.anon.anonrpc.model.RpcRequest;
import com.anon.anonrpc.model.RpcResponse;
import com.anon.anonrpc.serializer.JdkSerializer;
import com.anon.anonrpc.serializer.Serializer;
import com.anon.anonrpc.registry.ServiceRegistry;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 服务代理（JDK动态代理）
 */
public class ServiceProxy implements InvocationHandler {

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
        
        // 指定序列化器
        Serializer serializer = new JdkSerializer();
        // 构造请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        try {
            // 序列化
            byte[] bodyBytes = serializer.serialize(rpcRequest);
            
            System.out.println("正在连接RPC服务：" + serviceUrl);
            
            // 发送请求
            try (HttpResponse httpResponse = HttpRequest.post(serviceUrl)
                    .body(bodyBytes)
                    .execute()) {
                // 检查HTTP状态码
                int status = httpResponse.getStatus();
                System.out.println("HTTP响应状态码: " + status);
                
                // 获取响应内容
                byte[] result = httpResponse.bodyBytes();
                if (result == null || result.length == 0) {
                    System.err.println("警告: 服务器返回了空响应");
                    return null;
                }
                
                System.out.println("收到响应数据大小: " + result.length + " 字节");
                
                // 反序列化
                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
                return rpcResponse.getData();
            }
        } catch (IOException e) {
            System.err.println("RPC调用异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
