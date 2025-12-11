package org.linlinjava.litemall.allinone;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(properties = "spring.profiles.active=test")
class AllinoneConfigTest {
    @Autowired
    private Environment environment;

    @Test
    public void test() {
        // 测试获取配置信息（所有配置已合并到主配置文件中）
        System.out.println(environment.getProperty("litemall.express.appId"));
        System.out.println(environment.getProperty("spring.datasource.druid.url"));
        System.out.println(environment.getProperty("litemall.wx.app-id"));
        System.out.println(environment.getProperty("logging.level.org.linlinjava.litemall.wx"));
        System.out.println(environment.getProperty("logging.level.org.linlinjava.litemall.admin"));
        System.out.println(environment.getProperty("logging.level.org.linlinjava.litemall"));
    }

}
