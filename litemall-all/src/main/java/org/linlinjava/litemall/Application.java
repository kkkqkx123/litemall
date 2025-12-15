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
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
    exclude = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    },
    scanBasePackages = {"org.linlinjava.litemall", "org.linlinjava.litemall.admin"}
)
@MapperScan("org.linlinjava.litemall.db.dao")
@EnableTransactionManagement
@EnableScheduling
@EnableAspectJAutoProxy
@Primary
public class Application {

    @Bean(name = "mainApplication")
    public Application mainApplication() {
        return new Application();
    }

    public static void main(String[] args) throws Exception {
        // 禁用Spring Security方法安全机制，让自定义权限切面能够正常工作
        System.setProperty("spring.security.method-security.enabled", "false");
        
        SpringApplication.run(Application.class, args);
    }

}