package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.admin.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.security.AdminUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.*;
import org.linlinjava.litemall.db.service.LitemallAdminService;
import org.linlinjava.litemall.db.service.LitemallNoticeAdminService;
import org.linlinjava.litemall.db.service.LitemallNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.linlinjava.litemall.admin.util.AdminResponseCode.NOTICE_UPDATE_NOT_ALLOWED;

@RestController
@RequestMapping("/admin/notice")
@Validated
public class AdminNoticeController {
    private final Log logger = LogFactory.getLog(AdminNoticeController.class);

    @Autowired
    private LitemallNoticeService noticeService;
    @Autowired
    private LitemallAdminService adminService;
    @Autowired
    private LitemallNoticeAdminService noticeAdminService;

    @RequiresPermissionsDesc(menu = {"系统管理", "通知管理"}, button = "查询")
    @GetMapping("/list")
    public Object list(String title, String type,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LitemallNotice> noticeList = noticeService.querySelective(title, type, page, limit, sort, order);
        return ResponseUtil.okList(noticeList);
    }

    private Object validate(LitemallNotice notice) {
        String title = notice.getTitle();
        if (title == null || title.isEmpty()) {
            return ResponseUtil.badArgument();
        }
        return null;
    }

    private Integer getAdminId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        AdminUserDetails adminUserDetails = (AdminUserDetails) authentication.getPrincipal();
        return adminUserDetails.getAdmin().getId();
    }

    @RequiresPermissionsDesc(menu = {"系统管理", "通知管理"}, button = "添加")
    @PostMapping("/create")
    public Object create(@RequestBody LitemallNotice notice) {
        Object error = validate(notice);
        if (error != null) {
            return error;
        }
        noticeService.add(notice);
        return ResponseUtil.ok(notice);
    }

    @RequiresPermissionsDesc(menu = {"系统管理", "通知管理"}, button = "详情")
    @GetMapping("/read")
    public Object read(@NotNull Integer id) {
        LitemallNotice notice = noticeService.findById(id);
        return ResponseUtil.ok(notice);
    }

    @RequiresPermissionsDesc(menu = {"系统管理", "通知管理"}, button = "编辑")
    @PostMapping("/update")
    public Object update(@RequestBody LitemallNotice notice) {
        Object error = validate(notice);
        if (error != null) {
            return error;
        }
        if (noticeService.updateById(notice) == 0) {
            return ResponseUtil.updatedDataFailed();
        }
        return ResponseUtil.ok();
    }

    @RequiresPermissionsDesc(menu = {"系统管理", "通知管理"}, button = "删除")
    @PostMapping("/delete")
    public Object delete(@RequestBody LitemallNotice notice) {
        Integer id = notice.getId();
        if (id == null) {
            return ResponseUtil.badArgument();
        }
        noticeService.deleteById(id);
        return ResponseUtil.ok();
    }

    @RequiresPermissionsDesc(menu = {"系统管理", "通知管理"}, button = "批量删除")
    @PostMapping("/batch-delete")
    public Object batchDelete(@RequestBody String body) {
        List<Integer> ids = JacksonUtil.parseIntegerList(body, "ids");
        noticeService.deleteByIds(ids);
        return ResponseUtil.ok();
    }
}
