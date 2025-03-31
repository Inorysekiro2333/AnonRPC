package com.anon.anonrpc.fault;

import java.util.function.Supplier;

/**
 * 重试机制 - 用于自动重试失败的请求
 */
public class RetryMechanism {
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 重试间隔（毫秒）
    private static final int RETRY_INTERVAL_MS = 1000;
    
    /**
     * 执行带有重试的操作
     * @param operation 要执行的操作
     * @param serviceUrl 服务URL
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败则抛出异常
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String serviceUrl) throws Exception {
        int retries = 0;
        Exception lastException = null;
        
        while (retries <= MAX_RETRIES) {
            try {
                if (retries > 0) {
                    System.out.println("尝试重试 " + serviceUrl + "，第 " + retries + " 次");
                }
                
                T result = operation.get();
                
                // 如果成功，记录成功并返回结果
                if (retries > 0) {
                    System.out.println("重试成功");
                }
                CircuitBreaker.recordSuccess(serviceUrl);
                return result;
            } catch (Exception e) {
                lastException = e;
                CircuitBreaker.recordFailure(serviceUrl);
                
                System.out.println("调用 " + serviceUrl + " 失败: " + e.getMessage());
                
                // 如果已达到最大重试次数，则抛出异常
                if (retries >= MAX_RETRIES) {
                    break;
                }
                
                // 等待一段时间后重试
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
                
                retries++;
            }
        }
        
        throw new RuntimeException("服务调用失败，已重试 " + MAX_RETRIES + " 次", lastException);
    }
} 