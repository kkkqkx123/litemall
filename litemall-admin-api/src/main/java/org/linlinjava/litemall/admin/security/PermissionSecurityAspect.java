package org.linlinjava.litemall.admin.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.linlinjava.litemall.admin.annotation.RequiresPermissions;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class PermissionSecurityAspect {

    @Around("@annotation(org.linlinjava.litemall.admin.annotation.RequiresPermissions)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);
        
        if (requiresPermissions != null) {
            String[] permissions = requiresPermissions.value();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseUtil.fail(401, "未授权访问");
            }

            // 检查权限
            boolean hasPermission = false;
            for (String permission : permissions) {
                if (hasPermission(authentication, permission)) {
                    hasPermission = true;
                    break;
                }
            }

            if (!hasPermission) {
                return ResponseUtil.fail(403, "权限不足");
            }
        }

        return joinPoint.proceed();
    }

    private boolean hasPermission(Authentication authentication, String permission) {
        PermissionMethodSecurityExpressionRoot root = 
            new PermissionMethodSecurityExpressionRoot(authentication);
        return root.hasPermission(permission);
    }
}