#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复DBML文件中的Ref关系错误
原文件中所有的Ref关系都错误地指向了goods表
"""

import re

def fix_dbml_relationships(input_file, output_file):
    """修复DBML文件中的Ref关系"""
    
    # 正确的Ref关系映射
    correct_relationships = [
        # 用户相关
        "Ref: litemall_address.user_id > litemall_user.id",
        "Ref: litemall_cart.user_id > litemall_user.id",
        "Ref: litemall_collect.user_id > litemall_user.id",
        "Ref: litemall_comment.user_id > litemall_user.id",
        "Ref: litemall_coupon_user.user_id > litemall_user.id",
        "Ref: litemall_feedback.user_id > litemall_user.id",
        "Ref: litemall_footprint.user_id > litemall_user.id",
        "Ref: litemall_groupon.user_id > litemall_user.id",
        "Ref: litemall_groupon.creator_user_id > litemall_user.id",
        "Ref: litemall_order.user_id > litemall_user.id",
        "Ref: litemall_search_history.user_id > litemall_user.id",
        
        # 商品相关
        "Ref: litemall_cart.goods_id > litemall_goods.id",
        "Ref: litemall_cart.goods_sn > litemall_goods.goods_sn",
        "Ref: litemall_cart.product_id > litemall_goods_product.id",
        "Ref: litemall_collect.value_id > litemall_goods.id",
        "Ref: litemall_comment.value_id > litemall_goods.id",
        "Ref: litemall_footprint.goods_id > litemall_goods.id",
        "Ref: litemall_goods.category_id > litemall_category.id",
        "Ref: litemall_goods.brand_id > litemall_brand.id",
        "Ref: litemall_goods_attribute.goods_id > litemall_goods.id",
        "Ref: litemall_goods_product.goods_id > litemall_goods.id",
        "Ref: litemall_goods_specification.goods_id > litemall_goods.id",
        "Ref: litemall_groupon_rules.goods_id > litemall_goods.id",
        "Ref: litemall_order_goods.goods_id > litemall_goods.id",
        "Ref: litemall_order_goods.goods_sn > litemall_goods.goods_sn",
        "Ref: litemall_order_goods.product_id > litemall_goods_product.id",
        "Ref: litemall_order_goods.order_id > litemall_order.id",
        
        # 订单相关
        "Ref: litemall_aftersale.order_id > litemall_order.id",
        "Ref: litemall_aftersale.user_id > litemall_user.id",
        "Ref: litemall_coupon_user.order_id > litemall_order.id",
        "Ref: litemall_coupon_user.coupon_id > litemall_coupon.id",
        "Ref: litemall_groupon.order_id > litemall_order.id",
        "Ref: litemall_groupon.rules_id > litemall_groupon_rules.id",
        "Ref: litemall_groupon.groupon_id > litemall_groupon.id",
        
        # 管理员相关
        "Ref: litemall_admin.role_ids > litemall_role.id",
        "Ref: litemall_notice.admin_id > litemall_admin.id",
        "Ref: litemall_notice_admin.notice_id > litemall_notice.id",
        "Ref: litemall_notice_admin.admin_id > litemall_admin.id",
        "Ref: litemall_permission.role_id > litemall_role.id",
        
        # 其他
        "Ref: litemall_address.province > litemall_region.id",
        "Ref: litemall_address.city > litemall_region.id",
        "Ref: litemall_address.county > litemall_region.id",
        "Ref: litemall_category.pid > litemall_category.id",
        "Ref: litemall_region.pid > litemall_region.id",
        
        # 支付和物流
        "Ref: litemall_order.pay_id > litemall_order.id",
        "Ref: litemall_order.ship_sn > litemall_order.id",
    ]
    
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 移除所有错误的Ref行
        lines = content.split('\n')
        fixed_lines = []
        ref_section_started = False
        
        for line in lines:
            # 检查是否是Ref行
            if line.strip().startswith('Ref:'):
                if not ref_section_started:
                    ref_section_started = True
                    # 添加正确的Ref关系
                    fixed_lines.append('// 表关系定义')
                    for rel in correct_relationships:
                        fixed_lines.append(rel)
                    fixed_lines.append('')  # 空行分隔
                # 跳过原来的错误Ref行
                continue
            else:
                fixed_lines.append(line)
        
        # 如果没有找到Ref部分，在最后添加
        if not ref_section_started:
            fixed_lines.append('')
            fixed_lines.append('// 表关系定义')
            for rel in correct_relationships:
                fixed_lines.append(rel)
        
        # 写入修复后的内容
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write('\n'.join(fixed_lines))
        
        print(f"修复完成！输出文件: {output_file}")
        print(f"修复了 {len(correct_relationships)} 个Ref关系")
        
    except Exception as e:
        print(f"修复过程中出现错误: {e}")

if __name__ == "__main__":
    input_file = "complete_er_diagram.dbml"
    output_file = "complete_er_diagram_fixed.dbml"
    
    fix_dbml_relationships(input_file, output_file)