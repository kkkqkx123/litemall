package org.linlinjava.litemall.admin.security;

import org.springframework.security.core.Authentication;

public class PermissionMethodSecurityExpressionRoot extends org.springframework.security.access.expression.SecurityExpressionRoot implements org.springframework.security.access.expression.method.MethodSecurityExpressionOperations {

    public PermissionMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication object cannot be null");
        }
    }

    private Object filterObject;
    private Object returnObject;

    // SecurityExpressionRoot基类已经提供了所有标准方法，包括：
    // hasAuthority, hasAnyAuthority, hasRole, hasAnyRole, 
    // permitAll, denyAll, isAnonymous, isAuthenticated, 
    // isRememberMe, isFullyAuthenticated, hasPermission等
    
    // 我们只需要保留自定义的方法实现

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
        return this;
    }

    @Override
    public void setFilterObject(Object obj) {
        this.filterObject = obj;
    }

    @Override
    public void setReturnObject(Object obj) {
        this.returnObject = obj;
    }

    // setThis方法在Spring Security 6.x中可能已弃用或不再需要
    // @Override
    // public void setThis(Object obj) {
    //     // 这个方法在Spring Security 6.x中可能不再需要
    // }

    // 自定义权限检查方法
    public boolean hasPermission(String permission) {
        return checkAuthorityWithWildcard(permission);
    }

    public boolean hasAnyPermission(String... permissions) {
        return checkAnyAuthorityWithWildcard(permissions);
    }
    
    // 自定义权限检查方法，支持通配符权限
    private boolean checkAuthorityWithWildcard(String authority) {
        // 如果用户有通配符权限，则允许所有权限
        if (super.hasAuthority("*")) {
            return true;
        }
        return super.hasAuthority(authority);
    }
    
    private boolean checkAnyAuthorityWithWildcard(String... authorities) {
        // 如果用户有通配符权限，则允许所有权限
        if (super.hasAuthority("*")) {
            return true;
        }
        return super.hasAnyAuthority(authorities);
    }
}