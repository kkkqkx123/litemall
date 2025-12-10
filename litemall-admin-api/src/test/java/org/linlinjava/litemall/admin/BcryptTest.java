package org.linlinjava.litemall.admin;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BcryptTest {

    @Test
    public void test() {
        String rawPassword = "aaaaaa";
        String encodedPassword = "";
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        encodedPassword = bCryptPasswordEncoder.encode(rawPassword);

        System.out.println("rawPassword=" + rawPassword + " encodedPassword=" + encodedPassword);

        assertTrue(bCryptPasswordEncoder.matches(rawPassword, encodedPassword));
    }
}
