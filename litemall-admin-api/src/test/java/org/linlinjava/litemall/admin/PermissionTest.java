package org.linlinjava.litemall.admin;

import org.junit.jupiter.api.Test;

import org.linlinjava.litemall.admin.util.Permission;
import org.linlinjava.litemall.admin.util.PermissionUtil;
import org.linlinjava.litemall.admin.vo.PermVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Set;

@SpringBootTest
class PermissionTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void test() {
        final String basicPackage = "org.linlinjava.litemall.admin";
        List<Permission> permissionList = PermissionUtil.listPermission(context, basicPackage);
        Set<String> permissionStringSet = PermissionUtil.listPermissionString(permissionList);
        List<PermVo> permVoList = PermissionUtil.listPermVo(permissionStringSet);
        permVoList.stream().forEach(System.out::println);
    }
}
