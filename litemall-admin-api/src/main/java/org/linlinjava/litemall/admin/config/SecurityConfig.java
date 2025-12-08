package org.linlinjava.litemall.admin.config;

import org.linlinjava.litemall.admin.security.JwtAuthenticationFilter;
import org.linlinjava.litemall.admin.security.AdminUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    /**
     * 配置身份验证管理器
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder, AdminUserDetailsService userDetailsService) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    /**
     * 配置AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    /**
     * 自定义方法安全表达式处理器
     */
    @Bean
    @Role(org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE)
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new DefaultMethodSecurityExpressionHandler() {
            @Override
            protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
                    Authentication authentication, MethodInvocation invocation) {
                // 创建自定义权限表达式根
                org.linlinjava.litemall.admin.security.PermissionMethodSecurityExpressionRoot root = 
                    new org.linlinjava.litemall.admin.security.PermissionMethodSecurityExpressionRoot(authentication);
                root.setTrustResolver(getTrustResolver());
                root.setPermissionEvaluator(getPermissionEvaluator());
                root.setRoleHierarchy(getRoleHierarchy());
                root.setDefaultRolePrefix(getDefaultRolePrefix());
                return root;
            }

            @Override
            public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication,
                                                           MethodInvocation mi) {
                return super.createEvaluationContext(authentication, mi);
            }
        };
    }

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 公开访问的接口
                .requestMatchers("/admin/auth/kaptcha").permitAll()
                .requestMatchers("/admin/auth/login").permitAll()
                .requestMatchers("/admin/auth/401").permitAll()
                .requestMatchers("/admin/auth/index").permitAll()
                .requestMatchers("/admin/auth/403").permitAll()
                .requestMatchers("/admin/index/**").permitAll()
                // 确保admin路径能通过JWT过滤器验证
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}