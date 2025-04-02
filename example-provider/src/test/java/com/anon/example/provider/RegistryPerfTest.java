package com.anon.example.provider;

import com.anon.anonrpc.registry.ServiceRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegistryPerfTest {
    public static void main(String[] args) throws InterruptedException {
        int numThreads = 100;
        int numOpsPerThread = 10000;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < numOpsPerThread; j++) {
                        String serviceType = "service-" + (j % 10);
                        String serviceUrl = "http://localhost:" + (8000 + j % 1000) + "/service-" + threadId + "-" + j;
                        ServiceRegistry.register(serviceType, serviceUrl);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(); // 等待所有线程完成
        long endTime = System.currentTimeMillis();
        
        long totalOps = numThreads * numOpsPerThread;
        long timeMs = endTime - startTime;
        double opsPerSec = totalOps * 1000.0 / timeMs;
        
        System.out.println("完成 " + totalOps + " 次注册，耗时 " + timeMs + " 毫秒");
        System.out.println("吞吐量: " + opsPerSec + " 注册/秒");
        
        // 测试获取服务
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            ServiceRegistry.getNextServiceUrl("service-" + (i % 10));
        }
        endTime = System.currentTimeMillis();
        System.out.println("完成 1,000,000 次服务获取，耗时 " + (endTime - startTime) + " 毫秒");
        System.out.println("吞吐量: " + (1000000 * 1000.0 / (endTime - startTime)) + " 获取/秒");
        
        executor.shutdown();
    }
} 