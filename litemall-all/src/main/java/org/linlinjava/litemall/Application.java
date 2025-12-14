package org.linlinjava.litemall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class
})
@MapperScan("org.linlinjava.litemall.db.dao")
@EnableTransactionManagement
@EnableScheduling
@Primary
public class Application {

    @Bean(name = "mainApplication")
    public Application mainApplication() {
        return new Application();
    }

    public static void main(String[] args) throws Exception {
        // 在Spring Boot启动前完全禁用Spring Security 6.x的方法安全机制
        System.setProperty("spring.security.method-security.enabled", "false");
        System.setProperty("spring.security.authorization.method-security.enabled", "false");
        System.setProperty("spring.security.method.interceptor.enabled", "false");
        System.setProperty("spring.security.authorization.method.interceptor.enabled", "false");
        
        SpringApplication.run(Application.class, args);
    }

}