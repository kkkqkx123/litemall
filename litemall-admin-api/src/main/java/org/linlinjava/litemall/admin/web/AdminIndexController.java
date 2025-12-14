package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.admin.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/index")
public class AdminIndexController {
    private final Log logger = LogFactory.getLog(AdminIndexController.class);

    @PreAuthorize("hasPermission('admin:index:index')")    @RequiresPermissionsDesc(menu = {"系统管理", "系统首页"}, button = "首页")
    @GetMapping("/index")
    public Object index() {
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasPermission('admin:index:guest')")    @RequiresPermissionsDesc(menu = {"系统管理", "访客访问"}, button = "访客")
    @GetMapping("/guest")
    public Object guest() {
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasPermission('admin:index:authn')")    @RequiresPermissionsDesc(menu = {"系统管理", "认证测试"}, button = "认证")
    @GetMapping("/authn")
    public Object authn() {
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasPermission('admin:index:user')")    @RequiresPermissionsDesc(menu = {"系统管理", "用户访问"}, button = "用户")
    @GetMapping("/user")
    public Object user() {
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasPermission('admin:index:admin')")    @RequiresPermissionsDesc(menu = {"系统管理", "管理员访问"}, button = "管理员")
    @GetMapping("/admin")
    public Object admin() {
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasPermission('admin:index:admin2')")    @RequiresPermissionsDesc(menu = {"系统管理", "管理员2访问"}, button = "管理员2")
    @GetMapping("/admin2")
    public Object admin2() {
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasPermission('admin:index:permission:read')")    @RequiresPermissionsDesc(menu = {"系统管理", "权限测试"}, button = "权限读")
    @GetMapping("/read")
    public Object read() {
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasPermission('admin:index:permission:write')")    @RequiresPermissionsDesc(menu = {"系统管理", "权限测试"}, button = "权限写")
    @PostMapping("/write")
    public Object write() {
        return ResponseUtil.ok();
    }

}
