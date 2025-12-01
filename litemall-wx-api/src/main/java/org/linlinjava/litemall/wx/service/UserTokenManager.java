package org.linlinjava.litemall.wx.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维护用户token - 使用简单的内存存储替代JWT
 */
public class UserTokenManager {
    
    // 简单的内存token存储，生产环境应该使用Redis等外部存储
    private static final Map<String, Integer> tokenUserMap = new ConcurrentHashMap<>();
    private static final Map<Integer, String> userTokenMap = new ConcurrentHashMap<>();
    
    // token有效期24小时（毫秒）
    private static final long TOKEN_EXPIRE_TIME = 24 * 60 * 60 * 1000;
    
    public static String generateToken(Integer id) {
        // 如果用户已有token，先删除旧的
        String existingToken = userTokenMap.get(id);
        if (existingToken != null) {
            tokenUserMap.remove(existingToken);
        }
        
        // 生成新的token
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenUserMap.put(token, id);
        userTokenMap.put(id, token);
        
        return token;
    }
    
    public static Integer getUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return tokenUserMap.get(token);
    }
    
    public static void removeToken(String token) {
        Integer userId = tokenUserMap.remove(token);
        if (userId != null) {
            userTokenMap.remove(userId);
        }
    }
}
