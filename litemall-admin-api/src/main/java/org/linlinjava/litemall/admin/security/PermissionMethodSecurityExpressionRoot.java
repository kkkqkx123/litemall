package org.linlinjava.litemall.admin.security;

import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Collection;

public class PermissionMethodSecurityExpressionRoot implements MethodSecurityExpressionOperations {

    protected final Authentication authentication;
    private Object filterObject;
    private Object returnObject;
    private Object target;

    public PermissionMethodSecurityExpressionRoot(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication object cannot be null");
        }
        this.authentication = authentication;
    }

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public boolean hasAuthority(String authority) {
        return hasAnyAuthority(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        return hasAnyAuthorityName(null, authorities);
    }

    @Override
    public boolean hasRole(String role) {
        return hasAnyRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        return hasAnyAuthorityName("ROLE_", roles);
    }

    private boolean hasAnyAuthorityName(String prefix, String... roles) {
        for (String role : roles) {
            String defaultedRole = getRoleWithDefaultPrefix(prefix, role);
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(defaultedRole))) {
                return true;
            }
        }
        return false;
    }

    private String getRoleWithDefaultPrefix(String prefix, String role) {
        if (role == null) {
            return role;
        }
        if (prefix == null || role.startsWith(prefix)) {
            return role;
        }
        return prefix + role;
    }

    @Override
    public boolean permitAll() {
        return true;
    }

    @Override
    public boolean denyAll() {
        return false;
    }

    @Override
    public boolean isAnonymous() {
        return !authentication.isAuthenticated();
    }

    @Override
    public boolean isAuthenticated() {
        return authentication.isAuthenticated();
    }

    @Override
    public boolean isRememberMe() {
        return false; // 暂时不支持记住我功能
    }

    @Override
    public boolean isFullyAuthenticated() {
        return !isRememberMe() && isAuthenticated();
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        return false; // 暂时不支持对象级别的权限
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        return false; // 暂时不支持对象级别的权限
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }

    @Override
    public void setFilterObject(Object obj) {
        this.filterObject = obj;
    }

    @Override
    public void setReturnObject(Object obj) {
        this.returnObject = obj;
    }

    @Override
    public void setThis(Object obj) {
        this.target = obj;
    }

    // 自定义权限检查方法
    public boolean hasPermission(String permission) {
        return hasAuthority(permission);
    }

    public boolean hasAnyPermission(String... permissions) {
        return hasAnyAuthority(permissions);
    }
}