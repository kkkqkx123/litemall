package org.linlinjava.litemall.admin.security;

import org.aopalliance.intercept.MethodInvocation;
import org.linlinjava.litemall.admin.annotation.RequiresPermissions;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Arrays;

public class PermissionMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, MethodInvocation invocation) {
        PermissionMethodSecurityExpressionRoot root = 
            new PermissionMethodSecurityExpressionRoot(authentication);
        // 基类SecurityExpressionRoot已经通过DefaultMethodSecurityExpressionHandler配置了必要的组件
        return root;
    }
}