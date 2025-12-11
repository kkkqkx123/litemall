package org.linlinjava.litemall.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(properties = "spring.profiles.active=test")
class CoreConfigTest {
    private final Log logger = LogFactory.getLog(CoreConfigTest.class);
    @Autowired
    Environment environment;

    @Test
    public void test() {
        // 测试获取application-core.yml配置信息
        logger.info(environment.getProperty("litemall.express.appId"));
    }
}
