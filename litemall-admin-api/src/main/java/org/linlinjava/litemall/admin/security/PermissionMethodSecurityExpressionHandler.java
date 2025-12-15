package org.linlinjava.litemall.admin.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
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
        // 创建自定义的评估上下文，确保认证信息正确传递
        StandardEvaluationContext ctx = (StandardEvaluationContext) super.createEvaluationContext(authentication, mi);
        
        // 优先使用SecurityContextHolder中的认证信息，这是最可靠的来源
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        // 如果SecurityContextHolder中的认证为空或未认证，尝试使用传入的认证参数
        if (auth == null || !auth.isAuthenticated()) {
            try {
                auth = authentication.get();
            } catch (Exception e) {
                // 如果获取传入认证失败，忽略异常，继续使用SecurityContextHolder中的认证
            }
        }
        
        // 如果仍然为空，创建一个空的认证对象以避免AuthenticationCredentialsNotFoundException
        if (auth == null) {
            auth = new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "anonymousKey", "anonymous", 
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        }
        
        // 设置认证信息到评估上下文
        if (auth != null && auth.isAuthenticated()) {
            ctx.setVariable("authentication", auth);
        }
        
        return ctx;
    }
}