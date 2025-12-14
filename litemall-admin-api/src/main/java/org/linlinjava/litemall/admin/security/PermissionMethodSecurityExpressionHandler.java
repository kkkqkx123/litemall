package org.linlinjava.litemall.admin.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import java.util.function.Supplier;


public class PermissionMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, MethodInvocation invocation) {
        PermissionMethodSecurityExpressionRoot root = 
            new PermissionMethodSecurityExpressionRoot(authentication);
        // 配置必要的组件
        root.setTrustResolver(getTrustResolver());
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setDefaultRolePrefix(getDefaultRolePrefix());
        return root;
    }

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication,
                                                     MethodInvocation mi) {
        StandardEvaluationContext context = (StandardEvaluationContext) super.createEvaluationContext(authentication, mi);
        MethodSecurityExpressionOperations delegate = (MethodSecurityExpressionOperations) context.getRootObject().getValue();
        PermissionMethodSecurityExpressionRoot root = new PermissionMethodSecurityExpressionRoot(delegate);
        root.setTrustResolver(getTrustResolver());
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setDefaultRolePrefix(getDefaultRolePrefix());
        context.setRootObject(root);
        return context;
    }
}