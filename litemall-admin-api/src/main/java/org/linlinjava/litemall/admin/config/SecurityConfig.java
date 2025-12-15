package org.linlinjava.litemall.admin.config;

import org.linlinjava.litemall.admin.security.JwtAuthenticationFilter;
import org.linlinjava.litemall.admin.security.AdminUserDetailsService;
import org.linlinjava.litemall.admin.security.PermissionSecurityAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
// @EnableMethodSecurity(prePostEnabled = false) // 禁用Spring Security方法安全机制，使用自定义权限切面
@EnableAspectJAutoProxy // 启用AOP切面支持，让自定义权限切面生效
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
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    /**
     * 手动注册权限切面Bean，确保权限切面被Spring容器正确识别
     */
    @Bean
    public PermissionSecurityAspect permissionSecurityAspect() {
        return new PermissionSecurityAspect();
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
                // 允许所有请求通过，由自定义权限切面进行权限检查
                .anyRequest().permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                // 配置访问拒绝处理器，使用项目中的403端点
                .accessDeniedHandler(accessDeniedHandler())
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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