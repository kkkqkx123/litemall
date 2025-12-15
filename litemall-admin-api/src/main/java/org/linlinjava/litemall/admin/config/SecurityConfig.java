package org.linlinjava.litemall.admin.config;

import org.linlinjava.litemall.admin.security.JwtAuthenticationFilter;
import org.linlinjava.litemall.admin.security.AdminUserDetailsService;
import org.linlinjava.litemall.admin.security.CustomPermissionEvaluator;
import org.linlinjava.litemall.admin.security.PermissionMethodSecurityExpressionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用Spring Security方法安全机制
public class SecurityConfig {

    @Autowired
    private CustomPermissionEvaluator customPermissionEvaluator;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    /**
     * 配置身份验证管理器
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder, AdminUserDetailsService userDetailsService) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    /**
     * 配置自定义权限评估器到Spring Security表达式处理器
     * 使用不同的Bean名称避免与Spring Security自动配置冲突
     */
    @Bean
    public DefaultWebSecurityExpressionHandler adminWebSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return expressionHandler;
    }

    /**
     * 配置方法安全表达式处理器，确保@PreAuthorize注解使用自定义权限评估器
     * Spring Security 6.x需要配置自定义的PermissionMethodSecurityExpressionHandler
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        PermissionMethodSecurityExpressionHandler expressionHandler = new PermissionMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return expressionHandler;
    }

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        // 配置SecurityContextHolder使用INHERITABLETHREADLOCAL策略，确保认证上下文在方法调用中正确传递
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 允许所有请求通过，权限检查由@PreAuthorize注解处理
                .anyRequest().permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                // 配置访问拒绝处理器，使用项目中的403端点
                .accessDeniedHandler(accessDeniedHandler())
            )
            .authenticationProvider(authenticationProvider)
            // 完全禁用匿名认证，避免AnonymousAuthenticationFilter覆盖认证上下文
            .anonymous(anonymous -> anonymous.disable())
            // 将JWT过滤器添加在AnonymousAuthenticationFilter之后，确保认证上下文不被覆盖
            .addFilterAfter(jwtAuthenticationFilter, org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);

        return http.build();
    }
    
    /**
     * 自定义访问拒绝处理器
     * 当Spring Security拒绝访问时，重定向到项目中的403端点
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, 
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            // 重定向到项目中的403端点
            response.sendRedirect("/admin/auth/403");
        };
    }
}