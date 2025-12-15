package org.linlinjava.litemall.admin.web;
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

    @PostMapping("/delete")
    public Object delete(@RequestBody LitemallFootprint footprint) {
        Integer id = footprint.getId();
        if (id == null) {
            return ResponseUtil.badArgument();
        }
        footprintService.deleteById(id);
        return ResponseUtil.ok();
    }

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
