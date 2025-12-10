package org.linlinjava.litemall.wx;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class BigDecimalTest {

    @Test
    public void test() {
        BigDecimal a = new BigDecimal(0);
        BigDecimal b = new BigDecimal(1);
        BigDecimal c = a.subtract(b);
        BigDecimal d = c.max(new BigDecimal(0));

        System.out.println(c);
        System.out.println(d);
    }
}
