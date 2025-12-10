package org.linlinjava.litemall.db;

import org.junit.jupiter.api.Test;
import org.linlinjava.litemall.db.dao.StatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class StatMapperTest {

    @Autowired
    private StatMapper statMapper;

    @Test
    public void testUser() {
        List<Map<String, Object>> result = statMapper.statUser();
        for (Map<String, Object> m : result) {
            m.forEach((k, v) -> System.out.println("key:value = " + k + ":" + v));
        }
    }

    @Test
    public void testOrder() {
        List<Map<String, Object>> result = statMapper.statOrder();
        for (Map<String, Object> m : result) {
            m.forEach((k, v) -> System.out.println("key:value = " + k + ":" + v));
        }
    }

    @Test
    public void testGoods() {
        List<Map<String, Object>> result = statMapper.statGoods();
        for (Map<String, Object> m : result) {
            m.forEach((k, v) -> System.out.println("key:value = " + k + ":" + v));
        }
    }

}
