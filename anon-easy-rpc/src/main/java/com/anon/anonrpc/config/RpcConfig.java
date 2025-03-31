package com.anon.anonrpc.config;

/**
 * RPC配置类
 */
public class RpcConfig {
    // 默认超时时间（毫秒）
    private static int timeoutMs = 3000;
    // 默认最大重试次数
    private static int maxRetries = 3;
    // 默认重试间隔（毫秒）
    private static int retryIntervalMs = 1000;
    // 默认熔断阈值（连续失败次数）
    private static int circuitBreakerThreshold = 5;
    // 默认熔断恢复时间（毫秒）
    private static long circuitBreakerRecoveryMs = 5000;
    
    // Getter和Setter方法
    public static int getTimeoutMs() {
        return timeoutMs;
    }
    
    public static void setTimeoutMs(int timeoutMs) {
        RpcConfig.timeoutMs = timeoutMs;
    }
    
    public static int getMaxRetries() {
        return maxRetries;
    }
    
    public static void setMaxRetries(int maxRetries) {
        RpcConfig.maxRetries = maxRetries;
    }
    
    public static int getRetryIntervalMs() {
        return retryIntervalMs;
    }
    
    public static void setRetryIntervalMs(int retryIntervalMs) {
        RpcConfig.retryIntervalMs = retryIntervalMs;
    }
    
    public static int getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }
    
    public static void setCircuitBreakerThreshold(int circuitBreakerThreshold) {
        RpcConfig.circuitBreakerThreshold = circuitBreakerThreshold;
    }
    
    public static long getCircuitBreakerRecoveryMs() {
        return circuitBreakerRecoveryMs;
    }
    
    public static void setCircuitBreakerRecoveryMs(long circuitBreakerRecoveryMs) {
        RpcConfig.circuitBreakerRecoveryMs = circuitBreakerRecoveryMs;
    }
} 