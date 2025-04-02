package com.anon.example.provider.controller;

import com.anon.anonrpc.registry.ServiceRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registry")
public class RegistryController {
    
    @PostMapping("/register")
    public Map<String, String> registerService(
            @RequestParam String serviceType,
            @RequestParam String serviceUrl) {
        
        ServiceRegistry.register(serviceType, serviceUrl);
        
        return Map.of(
            "status", "success",
            "message", "服务注册成功",
            "serviceType", serviceType,
            "serviceUrl", serviceUrl
        );
    }
    
    @GetMapping("/services")
    public List<String> getServices(@RequestParam(required = false) String serviceType) {
        return ServiceRegistry.getAllServiceUrls(serviceType != null ? serviceType : "default");
    }
    
    @DeleteMapping("/unregister")
    public Map<String, String> unregisterService(
            @RequestParam String serviceType,
            @RequestParam String serviceUrl) {
        
        ServiceRegistry.unregister(serviceType, serviceUrl);
        
        return Map.of(
            "status", "success",
            "message", "服务已移除"
        );
    }
} 