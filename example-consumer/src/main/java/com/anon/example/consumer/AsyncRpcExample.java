package com.anon.example.consumer;

import com.anon.anonrpc.proxy.AsyncServiceProxyFactory;
import com.anon.example.common.model.User;
import com.anon.example.common.service.UserService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AsyncRpcExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1. 使用异步服务代理
        AsyncServiceProxyFactory.AsyncService<UserService> asyncUserService = 
                AsyncServiceProxyFactory.getAsyncProxy(UserService.class);
        
        // 准备参数
        User user = new User();
        user.setName("async-user");
        
        // 异步调用
        CompletableFuture<User> future = asyncUserService.async(
                "getUser", 
                new Class[]{User.class}, 
                user);
        
        // 添加回调
        future.thenAccept(result -> 
                System.out.println("异步调用结果: " + result.getName()))
              .exceptionally(ex -> {
                  System.err.println("调用失败: " + ex.getMessage());
                  return null;
              });
        
        // 等待完成
        User result = future.get();
        System.out.println("最终结果: " + result.getName());
        
        // 2. 并行调用示例
        CompletableFuture<User>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            User u = new User();
            u.setName("batch-user-" + i);
            futures[i] = asyncUserService.async("getUser", new Class[]{User.class}, u);
        }
        
        // 等待所有调用完成
        CompletableFuture.allOf(futures).join();
        
        // 打印结果
        for (int i = 0; i < 10; i++) {
            System.out.println("批量结果 " + i + ": " + futures[i].get().getName());
        }
    }
} 