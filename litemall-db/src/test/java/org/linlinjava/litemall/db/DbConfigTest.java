package org.linlinjava.litemall.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class DbConfigTest {
    @Autowired
    Environment environment;

    @Test
    public void test() {
        System.out.println(environment.getProperty("spring.datasource.druid.url"));
    }
}
