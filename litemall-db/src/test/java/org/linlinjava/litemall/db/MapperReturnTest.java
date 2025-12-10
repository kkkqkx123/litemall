package org.linlinjava.litemall.db;

import org.junit.jupiter.api.Test;
import org.linlinjava.litemall.db.dao.LitemallSystemMapper;
import org.linlinjava.litemall.db.domain.LitemallSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MapperReturnTest {

    @Autowired
    private LitemallSystemMapper systemMapper;

    @Test
    public void test() {
        LitemallSystem system = new LitemallSystem();
        system.setKeyName("test-system-key");
        system.setKeyValue("test-system-value");
        int updates = systemMapper.insertSelective(system);
        assertEquals(1, updates);

        updates = systemMapper.deleteByPrimaryKey(system.getId());
        assertEquals(1, updates);

        updates = systemMapper.updateByPrimaryKey(system);
        assertEquals(0, updates);
    }

}

