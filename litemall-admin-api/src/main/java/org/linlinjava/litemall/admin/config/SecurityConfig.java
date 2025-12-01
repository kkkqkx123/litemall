package org.linlinjava.litemall.admin.config;

import org.linlinjava.litemall.admin.security.AdminUserDetailsService;
import org.linlinjava.litemall.admin.security.PermissionMethodSecurityExpressionHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private AdminUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PermissionMethodSecurityExpressionHandler permissionMethodSecurityExpressionHandler() {
        return new PermissionMethodSecurityExpressionHandler();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(authz -> authz
                // 公开访问的接口
                .requestMatchers("/admin/auth/kaptcha").permitAll()
                .requestMatchers("/admin/auth/login").permitAll()
                .requestMatchers("/admin/auth/401").permitAll()
                .requestMatchers("/admin/auth/index").permitAll()
                .requestMatchers("/admin/auth/403").permitAll()
                .requestMatchers("/admin/index/**").permitAll()
                // 其他接口需要认证
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/admin/auth/login")
                .permitAll()
                .disable()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/auth/logout")
                .permitAll()
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}