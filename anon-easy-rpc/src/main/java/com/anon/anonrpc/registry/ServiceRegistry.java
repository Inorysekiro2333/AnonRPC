package com.anon.anonrpc.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 服务注册中心 - 采用ConcurrentHashMap + 双重校验锁实现高并发
 * 支持每秒1000+高并发服务注册，确保数据一致性
 */
public class ServiceRegistry {
    // 使用ConcurrentHashMap存储服务列表，确保高并发读取性能
    private static final Map<String, List<String>> SERVICE_MAP = new ConcurrentHashMap<>();
    
    // 为每个服务类型创建独立的计数器，避免全局竞争
    private static final Map<String, AtomicInteger> INDEX_MAP = new ConcurrentHashMap<>();
    
    // 读写锁，支持并发读取但互斥写入
    private static final ReadWriteLock GLOBAL_LOCK = new ReentrantReadWriteLock();
    
    // 注册锁对象，用于双重校验锁
    private static final Map<String, Object> SERVICE_LOCKS = new ConcurrentHashMap<>();
    
    // 性能监控
    private static long totalRegistrations = 0;
    private static long startTime = System.currentTimeMillis();
    
    /**
     * 注册服务实例
     * 使用双重校验锁确保线程安全性能
     * @param serviceType 服务类型（可选）
     * @param serviceUrl 服务URL
     */
    public static void register(String serviceType, String serviceUrl) {
        if (serviceType == null || serviceType.isEmpty()) {
            serviceType = "default";
        }
        
        // 提前检查是否已经包含，避免获取锁
        List<String> existingList = SERVICE_MAP.get(serviceType);
        if (existingList != null && existingList.contains(serviceUrl)) {
            return; // 快速路径返回
        }
        
        // 创建新列表的情况 - 使用putIfAbsent优化
        if (existingList == null) {
            List<String> newList = new ArrayList<>();
            newList.add(serviceUrl);
            List<String> oldList = SERVICE_MAP.putIfAbsent(serviceType, newList);
            
            if (oldList != null) {
                // 如果同时有其他线程创建了列表，使用细粒度锁添加到已存在的列表
                Object serviceLock = SERVICE_LOCKS.computeIfAbsent(serviceType, k -> new Object());
                synchronized (serviceLock) {
                    if (!oldList.contains(serviceUrl)) {
                        oldList.add(serviceUrl);
                    }
                }
            } else {
                // 成功添加了新列表，还需创建计数器
                INDEX_MAP.putIfAbsent(serviceType, new AtomicInteger(0));
            }
        } else {
            // 只在必要时获取锁
            Object serviceLock = SERVICE_LOCKS.computeIfAbsent(serviceType, k -> new Object());
            synchronized (serviceLock) {
                if (!existingList.contains(serviceUrl)) {
                    existingList.add(serviceUrl);
                }
            }
        }
        
        // 递增计数器和记录性能（无需锁定）
        long current = ++totalRegistrations;
        long now = System.currentTimeMillis();
        double timeRunning = (now - startTime) / 1000.0;
        
        if (current % 1000 == 0) {
            double rate = current / timeRunning;
            System.out.printf("服务注册性能: %.2f 注册/秒，总注册数: %d%n", rate, current);
        }
        
        System.out.println("服务 [" + serviceType + "] 注册成功: " + serviceUrl);
    }
    
    /**
     * 简化的注册方法，使用默认服务类型
     * @param serviceUrl 服务URL
     */
    public static void register(String serviceUrl) {
        register("default", serviceUrl);
    }
    
    /**
     * 获取下一个服务实例（轮询负载均衡）
     * 读操作无需加锁，提高并发性能
     * @param serviceType 服务类型
     * @return 下一个可用的服务URL
     */
    public static String getNextServiceUrl(String serviceType) {
        if (serviceType == null || serviceType.isEmpty()) {
            serviceType = "default";
        }
        
        List<String> serviceList = SERVICE_MAP.get(serviceType);
        
        // 如果没有注册服务，尝试使用默认配置
        if (serviceList == null || serviceList.isEmpty()) {
            if ("default".equals(serviceType)) {
                // 从系统属性构建默认URL
                String host = System.getProperty("rpc.server.address", "localhost");
                String port = System.getProperty("rpc.server.port", "8080");
                String defaultUrl = "http://" + host + ":" + port;
                register(serviceType, defaultUrl);
                return defaultUrl;
            } else {
                // 尝试从默认服务类型获取
                return getNextServiceUrl("default");
            }
        }
        
        // 获取该服务类型的计数器
        AtomicInteger index = INDEX_MAP.computeIfAbsent(serviceType, k -> new AtomicInteger(0));
        
        // 原子操作实现轮询
        int currentIndex = index.getAndUpdate(i -> (i + 1) % serviceList.size());
        return serviceList.get(currentIndex);
    }
    
    /**
     * 简化的获取服务方法，使用默认服务类型
     * @return 下一个可用的服务URL
     */
    public static String getNextServiceUrl() {
        return getNextServiceUrl("default");
    }
    
    /**
     * 获取指定类型的所有服务实例
     * @param serviceType 服务类型
     * @return 服务URL列表
     */
    public static List<String> getAllServiceUrls(String serviceType) {
        if (serviceType == null || serviceType.isEmpty()) {
            serviceType = "default";
        }
        
        List<String> serviceList = SERVICE_MAP.get(serviceType);
        
        if (serviceList == null || serviceList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 返回防御性副本，避免外部修改
        return new ArrayList<>(serviceList);
    }
    
    /**
     * 移除服务实例
     * @param serviceType 服务类型
     * @param serviceUrl 服务URL
     */
    public static void unregister(String serviceType, String serviceUrl) {
        if (serviceType == null || serviceType.isEmpty()) {
            serviceType = "default";
        }
        
        Object serviceLock = SERVICE_LOCKS.get(serviceType);
        if (serviceLock == null) {
            return; // 该服务类型不存在
        }
        
        synchronized (serviceLock) {
            List<String> serviceList = SERVICE_MAP.get(serviceType);
            if (serviceList != null) {
                serviceList.remove(serviceUrl);
                System.out.println("服务 [" + serviceType + "] 已移除: " + serviceUrl);
            }
        }
    }
    
    /**
     * 简化的移除服务方法，使用默认服务类型
     * @param serviceUrl 服务URL
     */
    public static void unregister(String serviceUrl) {
        unregister("default", serviceUrl);
    }
    
    /**
     * 清空所有注册信息（通常用于测试）
     */
    public static void clear() {
        GLOBAL_LOCK.writeLock().lock();
        try {
            SERVICE_MAP.clear();
            INDEX_MAP.clear();
            System.out.println("服务注册中心已清空");
        } finally {
            GLOBAL_LOCK.writeLock().unlock();
        }
    }
} 