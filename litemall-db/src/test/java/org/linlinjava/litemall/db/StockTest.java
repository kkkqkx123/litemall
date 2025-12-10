package org.linlinjava.litemall.db;

import org.junit.jupiter.api.Test;
import org.linlinjava.litemall.db.dao.GoodsProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockTest {
    @Autowired
    private GoodsProductMapper goodsProductMapper;

    @Test
    public void testReduceStock() {
        Integer id = 1;
        Short num = 10;
        goodsProductMapper.reduceStock(id, num);
    }

    @Test
    public void testAddStock() {
        Integer id = 1;
        Short num = 10;
        goodsProductMapper.addStock(id, num);
    }
}
