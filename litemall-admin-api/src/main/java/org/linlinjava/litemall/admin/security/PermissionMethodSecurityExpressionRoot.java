package org.linlinjava.litemall.admin.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;

public class PermissionMethodSecurityExpressionRoot extends org.springframework.security.access.expression.SecurityExpressionRoot implements org.springframework.security.access.expression.method.MethodSecurityExpressionOperations {

    public PermissionMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication object cannot be null");
        }
    }

    public PermissionMethodSecurityExpressionRoot(MethodSecurityExpressionOperations delegate) {
        super(delegate.getAuthentication());
        if (delegate.getAuthentication() == null) {
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
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PermissionMethodSecurityExpressionRoot.class);
        logger.info("Checking permission: {}, user authorities: {}", permission, super.getAuthentication().getAuthorities());
        
        boolean result = checkAuthorityWithWildcard(permission);
        logger.info("Permission check result for '{}': {}", permission, result);
        return result;
    }

    public boolean hasAnyPermission(String... permissions) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PermissionMethodSecurityExpressionRoot.class);
        logger.info("Checking any permission: {}, user authorities: {}", java.util.List.of(permissions), super.getAuthentication().getAuthorities());
        
        boolean result = checkAnyAuthorityWithWildcard(permissions);
        logger.info("Any permission check result for '{}': {}", java.util.List.of(permissions), result);
        return result;
    }
    
    // 自定义权限检查方法，支持通配符权限
    private boolean checkAuthorityWithWildcard(String authority) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PermissionMethodSecurityExpressionRoot.class);
        
        // 如果用户有通配符权限，则允许所有权限
        if (super.hasAuthority("*")) {
            logger.debug("Found wildcard permission '*', granting access");
            return true;
        }
        
        boolean result = super.hasAuthority(authority);
        if (result) {
            logger.debug("Found matching permission: {}", authority);
        } else {
            logger.debug("No matching permission found for: {}", authority);
        }
        return result;
    }
    
    private boolean checkAnyAuthorityWithWildcard(String... authorities) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PermissionMethodSecurityExpressionRoot.class);
        
        // 如果用户有通配符权限，则允许所有权限
        if (super.hasAuthority("*")) {
            logger.debug("Found wildcard permission '*', granting access");
            return true;
        }
        
        boolean result = super.hasAnyAuthority(authorities);
        if (result) {
            logger.debug("Found matching permission in: {}", java.util.List.of(authorities));
        } else {
            logger.debug("No matching permission found for any of: {}", java.util.List.of(authorities));
        }
        return result;
    }
}