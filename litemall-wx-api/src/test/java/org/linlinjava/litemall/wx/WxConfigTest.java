package org.linlinjava.litemall.wx;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class WxConfigTest {
    @Autowired
    private Environment environment;

    @Test
    public void test() {
        // 测试获取application-core.yml配置信息
        System.out.println(environment.getProperty("litemall.express.appId"));
        // 测试获取application-db.yml配置信息
        System.out.println(environment.getProperty("spring.datasource.druid.url"));
        // 测试获取application-wx.yml配置信息
        System.out.println(environment.getProperty("litemall.wx.app-id"));
        // 测试获取application-wx.yml配置信息
        System.out.println(environment.getProperty("litemall.wx.notify-url"));
        // 测试获取application.yml配置信息
        System.out.println(environment.getProperty("logging.level.org.linlinjava.litemall.wx"));
    }

}
