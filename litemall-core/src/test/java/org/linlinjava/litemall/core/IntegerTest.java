package org.linlinjava.litemall.core;

import org.junit.jupiter.api.Test;

class IntegerTest {
    @Test
    public void test() {
        Integer a = Integer.valueOf(512);
        int b = 512;
        Integer c = Integer.valueOf(512);
        System.out.println(a==b);
        System.out.println(a.equals(b));
        System.out.println(a == c);
        System.out.println(a.equals(c));
    }
}
