package com.anon.anonrpc.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceRegistry {
    private static final List<String> serviceUrls = new ArrayList<>();
    private static final AtomicInteger index = new AtomicInteger(0);

    // 注册服务
    public static void register(String serviceUrl) {
        if (!serviceUrls.contains(serviceUrl)) {
            serviceUrls.add(serviceUrl);
            System.out.println("服务注册成功: " + serviceUrl);
        }
    }

    // 获取下一个服务实例（轮询）
    public static String getNextServiceUrl() {
        if (serviceUrls.isEmpty()) {
            // 从系统属性构建默认URL
            String host = System.getProperty("rpc.server.address", "localhost");
            String port = System.getProperty("rpc.server.port", "8080");
            String defaultUrl = "http://" + host + ":" + port;
            register(defaultUrl);
            System.out.println("没有注册的服务，使用默认URL: " + defaultUrl);
        }
        int currentIndex = index.getAndUpdate(i -> (i + 1) % serviceUrls.size());
        return serviceUrls.get(currentIndex);
    }
} 