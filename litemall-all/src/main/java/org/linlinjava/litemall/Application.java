package org.linlinjava.litemall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = {"org.linlinjava.litemall.db", "org.linlinjava.litemall.core", "org.linlinjava.litemall.wx"},
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.linlinjava\\.litemall\\.admin\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.linlinjava\\.litemall\\.admin\\.AdminApplication"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.linlinjava\\.litemall\\.wx\\..*Application")
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
        SpringApplication.run(Application.class, args);
    }

}