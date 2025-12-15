package org.linlinjava.litemall.admin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

/**
 * 自定义权限评估器
 * 用于处理基于权限表达式的权限检查，特别是超级管理员权限
 */
@Component
public class CustomPermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(CustomPermissionEvaluator.class);
    private static final String WILDCARD_PERMISSION = "*";

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        logger.trace("CustomPermissionEvaluator.hasPermission called - targetDomainObject: {}, permission: {}", 
                targetDomainObject, permission);
        
        // 检查是否为超级管理员（拥有通配符权限）
        if (isSuperAdmin(authentication)) {
            logger.trace("User is super admin, permission granted");
            return true;
        }

        // 对于非超级管理员，进行具体的权限检查
        if (permission instanceof String) {
            String requiredPermission = (String) permission;
            logger.trace("Checking permission: {}", requiredPermission);
            
            // 检查用户是否具有所需的权限
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            logger.trace("User authorities: {}", authorities);
            
            boolean hasPermission = authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals(requiredPermission));
            
            logger.trace("Permission check result for '{}': {}", requiredPermission, hasPermission);
            return hasPermission;
        }
        
        logger.trace("Permission is not a String, returning false");
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        logger.trace("CustomPermissionEvaluator.hasPermission called - targetId: {}, targetType: {}, permission: {}", 
                targetId, targetType, permission);
        
        // 检查是否为超级管理员（拥有通配符权限）
        if (isSuperAdmin(authentication)) {
            logger.trace("User is super admin, permission granted");
            return true;
        }

        // 对于非超级管理员，进行具体的权限检查
        if (permission instanceof String) {
            String requiredPermission = (String) permission;
            logger.trace("Checking permission: {}", requiredPermission);
            
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            logger.trace("User authorities: {}", authorities);
            
            boolean hasPermission = authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals(requiredPermission));
            
            logger.trace("Permission check result for '{}': {}", requiredPermission, hasPermission);
            return hasPermission;
        }
        
        logger.trace("Permission is not a String, returning false");
        return false;
    }

    /**
     * 判断当前用户是否为超级管理员（拥有通配符权限）
     */
    private boolean isSuperAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.trace("Authentication is null or not authenticated");
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        logger.trace("Checking super admin - authorities: {}", authorities);
        
        boolean isSuperAdmin = authorities.stream()
                .anyMatch(authority -> WILDCARD_PERMISSION.equals(authority.getAuthority()));
        
        logger.trace("Super admin check result: {}", isSuperAdmin);
        return isSuperAdmin;
    }

    /**
     * 检查当前认证用户是否为超级管理员（拥有通配符权限）
     */
    public static boolean isCurrentUserSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isSuperAdminStatic(authentication);
    }
    
    /**
     * 静态版本的超级管理员检查
     */
    private static boolean isSuperAdminStatic(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> WILDCARD_PERMISSION.equals(authority.getAuthority()));
    }
}