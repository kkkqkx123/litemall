package org.linlinjava.litemall.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 异步测试
 */
@SpringBootTest
class AsyncTest {

    @Autowired
    AsyncTask task;

    @Test
    public void test() {
        task.asyncMethod();
        task.nonasyncMethod();
    }
}
