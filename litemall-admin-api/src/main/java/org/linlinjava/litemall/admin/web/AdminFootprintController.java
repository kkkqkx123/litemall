package org.linlinjava.litemall.admin.web;

import org.linlinjava.litemall.admin.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.LitemallFootprint;
import org.linlinjava.litemall.db.domain.LitemallFootprintExample;
import org.linlinjava.litemall.db.service.LitemallFootprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/admin/footprint")
@Validated
public class AdminFootprintController {
    @Autowired
    private LitemallFootprintService footprintService;

    @PreAuthorize("hasAuthority('admin:footprint:list')")
    @RequiresPermissions("admin:footprint:list")
    @RequiresPermissionsDesc(menu = {"用户管理", "会员足迹"}, button = "查询")
    @GetMapping("/list")
    public Object list(String userId, String goodsId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LitemallFootprint> footprintList = footprintService.querySelective(userId, goodsId, page, limit, sort,
                order);
        return ResponseUtil.okList(footprintList);
    }

    @PreAuthorize("hasAuthority('admin:footprint:delete')")
    @RequiresPermissions("admin:footprint:delete")
    @RequiresPermissionsDesc(menu = {"用户管理", "会员足迹"}, button = "删除")
    @DeleteMapping("/delete")
    public Object delete(@RequestBody Map<String, Object> request) {
        System.out.println("=== DEBUG: delete request received: " + request);
        Object idObj = request.get("id");
        System.out.println("=== DEBUG: extracted id object: " + idObj + " (type: " + (idObj != null ? idObj.getClass().getName() : "null") + ")");
        
        Integer id = null;
        if (idObj instanceof Integer) {
            id = (Integer) idObj;
        } else if (idObj instanceof Number) {
            id = ((Number) idObj).intValue();
        }
        
        System.out.println("=== DEBUG: converted id: " + id);
        if (id == null) {
            System.out.println("=== DEBUG: id is null, returning badArgument");
            return ResponseUtil.badArgument();
        }
        
        System.out.println("=== DEBUG: calling deleteById with id: " + id);
        footprintService.deleteById(id);
        System.out.println("=== DEBUG: deleteById completed successfully");
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasAuthority('admin:footprint:delete')")
    @RequiresPermissions("admin:footprint:delete")
    @RequiresPermissionsDesc(menu = {"用户管理", "会员足迹"}, button = "批量删除")
    @PostMapping("/batch-delete")
    public Object batchDelete(@RequestBody Map<String, List<Integer>> request) {
        List<Integer> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseUtil.badArgument();
        }
        
        LitemallFootprintExample example = new LitemallFootprintExample();
        example.or().andIdIn(ids);
        
        footprintService.deleteByExample(example);
        return ResponseUtil.ok();
    }
}
