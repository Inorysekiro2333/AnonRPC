package com.anon.anonrpc.fault;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 熔断器 - 用于防止对故障服务持续发起请求
 */
public class CircuitBreaker {
    // 熔断器状态
    public enum State {
        CLOSED,      // 关闭状态 - 正常请求服务
        OPEN,        // 打开状态 - 服务被熔断
        HALF_OPEN    // 半开状态 - 尝试恢复服务
    }
    
    // 每个服务URL的熔断器状态
    private static final Map<String, State> stateMap = new ConcurrentHashMap<>();
    // 失败计数器
    private static final Map<String, AtomicInteger> failureCountMap = new ConcurrentHashMap<>();
    // 上次熔断时间
    private static final Map<String, AtomicLong> lastBreakTimeMap = new ConcurrentHashMap<>();
    
    // 熔断阈值 - 连续失败次数
    private static final int FAILURE_THRESHOLD = 5;
    // 恢复时间 - 熔断后多久尝试恢复，单位毫秒
    private static final long RECOVERY_TIMEOUT_MS = 5000;
    
    /**
     * 判断服务是否可用（未熔断）
     * @param serviceUrl 服务URL
     * @return 是否可用
     */
    public static boolean isAvailable(String serviceUrl) {
        State state = stateMap.getOrDefault(serviceUrl, State.CLOSED);
        
        if (state == State.CLOSED) {
            return true; // 正常状态，可以访问
        } else if (state == State.OPEN) {
            // 熔断状态，检查是否可以尝试恢复
            long lastBreakTime = lastBreakTimeMap.getOrDefault(serviceUrl, new AtomicLong(0)).get();
            long now = System.currentTimeMillis();
            
            if (now - lastBreakTime > RECOVERY_TIMEOUT_MS) {
                // 进入半开状态
                stateMap.put(serviceUrl, State.HALF_OPEN);
                System.out.println("服务 " + serviceUrl + " 进入半开状态，尝试恢复");
                return true; // 允许一次尝试
            }
            return false; // 仍在熔断期间
        } else { // HALF_OPEN
            return true; // 半开状态允许尝试
        }
    }
    
    /**
     * 记录访问成功
     * @param serviceUrl 服务URL
     */
    public static void recordSuccess(String serviceUrl) {
        State currentState = stateMap.getOrDefault(serviceUrl, State.CLOSED);
        
        if (currentState == State.HALF_OPEN) {
            // 恢复服务
            stateMap.put(serviceUrl, State.CLOSED);
            failureCountMap.put(serviceUrl, new AtomicInteger(0));
            System.out.println("服务 " + serviceUrl + " 已恢复正常");
        } else if (currentState == State.CLOSED) {
            // 重置失败计数
            failureCountMap.put(serviceUrl, new AtomicInteger(0));
        }
    }
    
    /**
     * 记录访问失败
     * @param serviceUrl 服务URL
     */
    public static void recordFailure(String serviceUrl) {
        State currentState = stateMap.getOrDefault(serviceUrl, State.CLOSED);
        
        if (currentState == State.HALF_OPEN) {
            // 半开状态下失败，重新熔断
            stateMap.put(serviceUrl, State.OPEN);
            lastBreakTimeMap.put(serviceUrl, new AtomicLong(System.currentTimeMillis()));
            System.out.println("服务 " + serviceUrl + " 恢复失败，重新熔断");
        } else if (currentState == State.CLOSED) {
            // 增加失败计数
            AtomicInteger count = failureCountMap.computeIfAbsent(serviceUrl, k -> new AtomicInteger(0));
            int failureCount = count.incrementAndGet();
            
            // 判断是否需要熔断
            if (failureCount >= FAILURE_THRESHOLD) {
                stateMap.put(serviceUrl, State.OPEN);
                lastBreakTimeMap.put(serviceUrl, new AtomicLong(System.currentTimeMillis()));
                System.out.println("服务 " + serviceUrl + " 已熔断，连续失败次数: " + failureCount);
            }
        }
    }
} 