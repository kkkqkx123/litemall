package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    @GetMapping("/index")
    public Object index() {
        return ResponseUtil.ok();
    }

    @GetMapping("/guest")
    public Object guest() {
        return ResponseUtil.ok();
    }

    @GetMapping("/authn")
    public Object authn() {
        return ResponseUtil.ok();
    }

    @GetMapping("/user")
    public Object user() {
        return ResponseUtil.ok();
    }

    @GetMapping("/admin")
    public Object admin() {
        return ResponseUtil.ok();
    }

    @GetMapping("/admin2")
    public Object admin2() {
        return ResponseUtil.ok();
    }

    @GetMapping("/read")
    public Object read() {
        return ResponseUtil.ok();
    }

    @PostMapping("/write")
    public Object write() {
        return ResponseUtil.ok();
    }

}
