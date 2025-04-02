package com.anon.anonrpc.proxy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * 异步服务代理工厂
 */
public class AsyncServiceProxyFactory {
    
    /**
     * 获取同步服务代理
     */
    public static <T> T getProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new AsyncServiceProxy()
        );
    }
    
    /**
     * 获取异步服务代理
     * 例如: AsyncService asyncService = AsyncServiceProxyFactory.getAsyncProxy(UserService.class);
     */
    public static <T> AsyncService<T> getAsyncProxy(Class<T> serviceClass) {
        return (AsyncService<T>) Proxy.newProxyInstance(
                AsyncService.class.getClassLoader(),
                new Class[]{AsyncService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("async")) {
                        return getAsyncMethodProxy(serviceClass, (String)args[0], (Class<?>[])args[1], args[2]);
                    }
                    return method.invoke(proxy, args);
                }
        );
    }
    
    private static <T> CompletableFuture<Object> getAsyncMethodProxy(
            Class<T> serviceClass, String methodName, Class<?>[] paramTypes, Object arg) {
        
        T syncProxy = getProxy(serviceClass);
        try {
            java.lang.reflect.Method method = serviceClass.getMethod(methodName, paramTypes);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return method.invoke(syncProxy, arg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * 异步服务接口
     */
    public interface AsyncService<T> {
        <R> CompletableFuture<R> async(String methodName, Class<?>[] paramTypes, Object... args);
    }
} 