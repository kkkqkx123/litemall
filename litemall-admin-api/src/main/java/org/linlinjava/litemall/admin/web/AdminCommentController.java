package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.db.domain.CommentVO;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.dao.CommentMapperExt;
import org.linlinjava.litemall.db.domain.LitemallComment;
import org.linlinjava.litemall.db.service.LitemallCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/comment")
@Validated
public class AdminCommentController {
    private final Log logger = LogFactory.getLog(AdminCommentController.class);

    @Autowired
    private LitemallCommentService commentService;
    
    @Autowired
    private CommentMapperExt commentMapperExt;

    @GetMapping("/list")
    public Object list(String userId, String goodsName, String categoryId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        
        // 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1) {
            limit = 20;
        }
        // 设置分页限制为100
        if (limit > 100) {
            limit = 100;
        }
        
        // 转换参数
        Integer userIdInt = null;
        Integer categoryIdInt = null;
        
        if (userId != null && !userId.isEmpty()) {
            try {
                userIdInt = Integer.valueOf(userId);
            } catch (NumberFormatException e) {
                // 如果不是数字，可能是商品名称，将其赋值给goodsName
                if (goodsName == null || goodsName.isEmpty()) {
                    goodsName = userId;
                    userIdInt = null;
                }
            }
        }
        
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                categoryIdInt = Integer.valueOf(categoryId);
            } catch (NumberFormatException e) {
                categoryIdInt = null;
            }
        }
        
        // 计算offset
        Integer offset = (page - 1) * limit;
        
        // 查询数据
        List<CommentVO> commentList = commentMapperExt.selectCommentVOSelective(
            userIdInt, goodsName, categoryIdInt, page, limit, offset, sort, order);
        int total = commentMapperExt.countCommentVOSelective(
            userIdInt, goodsName, categoryIdInt);
        
        // 构建返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("list", commentList);
        data.put("total", total);
        data.put("page", page);
        data.put("limit", limit);
        data.put("pages", (int) Math.ceil((double) total / limit));
        
        return ResponseUtil.ok(data);
    }

    @PostMapping("/delete")
    public Object delete(@RequestBody LitemallComment comment) {
        Integer id = comment.getId();
        if (id == null) {
            return ResponseUtil.badArgument();
        }
        commentService.deleteById(id);
        return ResponseUtil.ok();
    }
}
