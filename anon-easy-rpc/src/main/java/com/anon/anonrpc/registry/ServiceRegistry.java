package com.anon.anonrpc.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceRegistry {
    private static final List<String> serviceUrls = new ArrayList<>();
    private static final AtomicInteger index = new AtomicInteger(0);

    // 注册服务
    public static void register(String serviceUrl) {
        serviceUrls.add(serviceUrl);
    }

    // 获取下一个服务实例（轮询）
    public static String getNextServiceUrl() {
        if (serviceUrls.isEmpty()) {
            throw new RuntimeException("没有可用的服务实例");
        }
        int currentIndex = index.getAndUpdate(i -> (i + 1) % serviceUrls.size());
        return serviceUrls.get(currentIndex);
    }
} 