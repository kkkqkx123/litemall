package org.linlinjava.litemall.db.dao;

import org.apache.ibatis.annotations.Param;
import org.linlinjava.litemall.db.domain.CommentVO;
import java.util.List;

/**
 * 评论Mapper扩展接口，用于多表关联查询
 */
public interface CommentMapperExt {
    /**
     * 根据条件查询评论VO列表
     * @param userId 用户ID
     * @param goodsName 商品名称
     * @param categoryId 分类ID
     * @param page 页码
     * @param limit 每页数量
     * @param sort 排序字段
     * @param order 排序方式
     * @return 评论VO列表
     */
    List<CommentVO> selectCommentVOSelective(
        @Param("userId") Integer userId,
        @Param("goodsName") String goodsName,
        @Param("categoryId") Integer categoryId,
        @Param("page") Integer page,
        @Param("limit") Integer limit,
        @Param("offset") Integer offset,
        @Param("sort") String sort,
        @Param("order") String order
    );
    
    /**
     * 根据条件统计评论数量
     * @param userId 用户ID
     * @param goodsName 商品名称
     * @param categoryId 分类ID
     * @return 评论数量
     */
    int countCommentVOSelective(
        @Param("userId") Integer userId,
        @Param("goodsName") String goodsName,
        @Param("categoryId") Integer categoryId
    );
}