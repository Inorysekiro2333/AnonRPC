package com.anon.anonrpc.fault;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 降级处理 - 用于服务不可用时提供备选方案
 */
public class FallbackHandler {
    // 保存每个方法的降级处理函数
    private static final Map<String, Function<Object[], Object>> fallbackMap = new HashMap<>();
    
    /**
     * 注册降级处理函数
     * @param serviceClass 服务类
     * @param methodName 方法名
     * @param fallback 降级处理函数
     */
    public static void registerFallback(Class<?> serviceClass, String methodName, Function<Object[], Object> fallback) {
        String key = generateKey(serviceClass, methodName);
        fallbackMap.put(key, fallback);
    }
    
    /**
     * 获取降级处理结果
     * @param method 方法
     * @param args 参数
     * @return 降级结果
     */
    public static Object getFallbackResult(Method method, Object[] args) {
        String key = generateKey(method.getDeclaringClass(), method.getName());
        Function<Object[], Object> fallback = fallbackMap.get(key);
        
        if (fallback != null) {
            System.out.println("使用降级处理方法: " + method.getName());
            return fallback.apply(args);
        }
        
        // 如果没有注册降级函数，返回默认值
        return getDefaultValue(method.getReturnType());
    }
    
    /**
     * 生成方法的唯一键
     */
    private static String generateKey(Class<?> serviceClass, String methodName) {
        return serviceClass.getName() + "." + methodName;
    }
    
    /**
     * 获取默认返回值
     */
    private static Object getDefaultValue(Class<?> returnType) {
        if (returnType.isPrimitive()) {
            if (returnType == boolean.class) return false;
            if (returnType == char.class) return '\u0000';
            if (returnType == byte.class
                    || returnType == short.class
                    || returnType == int.class
                    || returnType == long.class
                    || returnType == float.class
                    || returnType == double.class) {
                return 0;
            }
        }
        return null;
    }
} 