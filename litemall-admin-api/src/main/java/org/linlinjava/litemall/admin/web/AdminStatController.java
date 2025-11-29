package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.admin.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.admin.vo.StatVo;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.service.StatService;
import org.linlinjava.litemall.core.wordcloud.WordCloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/stat")
@Validated
public class AdminStatController {
    private final Log logger = LogFactory.getLog(AdminStatController.class);

    @Autowired
    private StatService statService;

    @Autowired
    private WordCloudService wordCloudService;

    @RequiresPermissions("admin:stat:user")
    @RequiresPermissionsDesc(menu = {"统计管理", "用户统计"}, button = "查询")
    @GetMapping("/user")
    public Object statUser() {
        List<Map> rows = statService.statUser();
        String[] columns = new String[]{"day", "users"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);
        return ResponseUtil.ok(statVo);
    }

    @RequiresPermissions("admin:stat:order")
    @RequiresPermissionsDesc(menu = {"统计管理", "订单统计"}, button = "查询")
    @GetMapping("/order")
    public Object statOrder() {
        List<Map> rows = statService.statOrder();
        String[] columns = new String[]{"day", "orders", "customers", "amount", "pcr"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);

        return ResponseUtil.ok(statVo);
    }

    /**
     * 增强版订单统计接口，支持时间维度和商品类别筛选
     * @param timeDimension 时间维度：day/week/month
     * @param categoryId 商品类别ID，可为null
     * @return 订单统计数据
     */
    @RequiresPermissions("admin:stat:order")
    @RequiresPermissionsDesc(menu = {"统计管理", "订单统计"}, button = "增强查询")
    @GetMapping("/order/enhanced")
    public Object statOrderEnhanced(@RequestParam(value = "timeDimension", defaultValue = "day") String timeDimension,
                                  @RequestParam(value = "categoryId", required = false) Integer categoryId) {
        List<Map> rows = statService.statOrderEnhanced(timeDimension, categoryId);
        String[] columns = new String[]{"period", "orders", "customers", "amount", "pcr"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);

        return ResponseUtil.ok(statVo);
    }

    @RequiresPermissions("admin:stat:goods")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品统计"}, button = "查询")
    @GetMapping("/goods")
    public Object statGoods() {
        List<Map> rows = statService.statGoods();
        String[] columns = new String[]{"day", "orders", "products", "amount"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);
        return ResponseUtil.ok(statVo);
    }

    /**
     * 商品评分统计接口
     * @param categoryId 商品分类ID，可为null
     * @param sort 排序字段：avg_rating
     * @param order 排序方式：asc/desc
     * @param page 页码
     * @param limit 每页条数
     * @return 商品评分统计数据
     */
    @RequiresPermissions("admin:stat:goods")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品评分统计"}, button = "查询")
    @GetMapping("/goods/rating")
    public Object statGoodsRating(@RequestParam(value = "categoryId", required = false) Integer categoryId,
                                @RequestParam(value = "sort", defaultValue = "avg_rating") String sort,
                                @RequestParam(value = "order", defaultValue = "desc") String order,
                                @RequestParam(value = "page", defaultValue = "1") Integer page,
                                @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        
        // 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        
        // 查询数据
        List<Map> rows = statService.statGoodsRating(categoryId, sort, order, page, limit);
        int total = statService.countGoodsRating(categoryId);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);
        result.put("pages", (int) Math.ceil((double) total / limit));
        
        return ResponseUtil.ok(result);
    }
    
    /**
     * 商品分类列表接口（用于筛选）
     * @return 商品分类列表
     */
    @RequiresPermissions("admin:stat:goods")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品评分统计"}, button = "获取分类")
    @GetMapping("/goods/categories")
    public Object statGoodsCategories() {
        List<Map> categories = statService.statGoodsCategories();
        return ResponseUtil.ok(categories);
    }

    /**
     * 商品评论统计接口
     * @param categoryId 商品分类ID，可为null
     * @param page 页码
     * @param limit 每页条数
     * @return 商品评论统计数据
     */
    @RequiresPermissions("admin:stat:goods")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品评论统计"}, button = "查询")
    @GetMapping("/goods/comment")
    public Object statGoodsComment(@RequestParam(value = "categoryId", required = false) Integer categoryId,
                                 @RequestParam(value = "page", defaultValue = "1") Integer page,
                                 @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        
        // 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        
        // 查询数据
        List<Map> rows = statService.statGoodsComment(categoryId, page, limit);
        int total = statService.countGoodsComment(categoryId);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);
        result.put("pages", (int) Math.ceil((double) total / limit));
        
        return ResponseUtil.ok(result);
    }
    
    /**
     * 商品评论词云接口
     * @param goodsId 商品ID
     * @param maxWords 最大词数
     * @return 词云数据
     */
    @RequiresPermissions("admin:stat:goods")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品评论词云"}, button = "生成词云")
    @GetMapping("/goods/wordcloud/{goodsId}")
    public Object generateWordCloud(@PathVariable(value = "goodsId") Integer goodsId,
                                  @RequestParam(value = "maxWords", defaultValue = "50") Integer maxWords) {
        
        // 参数校验
        if (goodsId == null || goodsId < 1) {
            return ResponseUtil.fail(400, "商品ID不能为空");
        }
        if (maxWords == null || maxWords < 1) {
            maxWords = 50;
        }
        if (maxWords > 200) {
            maxWords = 200;
        }
        
        // 获取商品评论内容
        List<Map> comments = statService.getGoodsComments(goodsId);
        if (comments == null || comments.isEmpty()) {
            return ResponseUtil.ok(new ArrayList<>());
        }
        
        // 提取评论文本
        List<String> commentTexts = comments.stream()
            .map(comment -> (String) comment.get("content"))
            .filter(text -> text != null && !text.trim().isEmpty())
            .collect(java.util.stream.Collectors.toList());
        
        if (commentTexts.isEmpty()) {
            return ResponseUtil.ok(new ArrayList<>());
        }
        
        // 生成词云
        List<WordCloudService.WordCloudData> wordCloudData = wordCloudService.generateWordCloud(commentTexts, maxWords);
        
        // 转换为前端期望的格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (WordCloudService.WordCloudData data : wordCloudData) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", data.getText());
            item.put("value", data.getFrequency());
            result.add(item);
        }
        
        return ResponseUtil.ok(result);
    }

}
