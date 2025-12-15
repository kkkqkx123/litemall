package org.linlinjava.litemall.admin.security;

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

    private static final String SUPER_ADMIN_ROLE = "超级管理员";

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // 检查是否为超级管理员
        if (isSuperAdmin(authentication)) {
            return true;
        }

        // 对于非超级管理员，进行具体的权限检查
        if (targetDomainObject instanceof String && permission instanceof String) {
            String targetType = (String) targetDomainObject;
            String requiredPermission = (String) permission;
            
            // 检查用户是否具有所需的权限
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            return authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals(requiredPermission));
        }
        
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // 检查是否为超级管理员
        if (isSuperAdmin(authentication)) {
            return true;
        }

        // 对于非超级管理员，进行具体的权限检查
        if (permission instanceof String) {
            String requiredPermission = (String) permission;
            
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            return authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals(requiredPermission));
        }
        
        return false;
    }

    /**
     * 判断当前用户是否为超级管理员
     */
    private boolean isSuperAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> SUPER_ADMIN_ROLE.equals(authority.getAuthority()));
    }

    /**
     * 检查当前认证用户是否为超级管理员
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
                .anyMatch(authority -> SUPER_ADMIN_ROLE.equals(authority.getAuthority()));
    }
}