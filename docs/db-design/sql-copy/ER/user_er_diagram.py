# -*- coding: utf-8 -*-
from graphviz import Digraph
import os

# 设置环境变量确保中文支持
os.environ['GRAPHVIZ_DOT'] = 'dot'
os.environ['LC_ALL'] = 'zh_CN.UTF-8'
os.environ['LANG'] = 'zh_CN.UTF-8'

# 定义多个中文字体备选方案
CHINESE_FONTS = [
    'SimSun',           # 宋体，Windows系统默认，兼容性最好
    'Microsoft YaHei',  # 微软雅黑，现代简洁
    'SimHei',           # 黑体，笔画粗重
    'NSimSun',          # 新宋体
    'Arial Unicode MS'  # Unicode字体，国际通用
]

# 尝试找到可用的字体
def find_available_font():
    """找到系统中可用的中文字体"""
    test_graph = Digraph('test', format='png', encoding='utf-8')
    
    for font in CHINESE_FONTS:
        try:
            # 测试字体
            test_graph.clear()
            test_graph.node('test_node', '测试', fontname=font)
            test_graph.render('font_test', cleanup=True)
            print(f"成功使用字体: {font}")
            return font
        except Exception as e:
            print(f"字体 {font} 不可用: {str(e)}")
            continue
    
    # 如果都不可用，返回默认字体
    print("使用默认字体")
    return 'Arial'

# 找到可用的中文字体
CHINESE_FONT = find_available_font()

# 定义样式常量
ENTITY_STYLE = {'shape': 'box', 'fontname': CHINESE_FONT, 'penwidth': '2.5', 'fontsize': '16', 'style': 'filled', 'fillcolor': 'lightblue'}
ATTRIBUTE_STYLE = {'shape': 'ellipse', 'fontname': CHINESE_FONT, 'penwidth': '2', 'fontsize': '14', 'style': 'filled', 'fillcolor': 'lightyellow'}
RELATIONSHIP_STYLE = {'shape': 'diamond', 'fontname': CHINESE_FONT, 'penwidth': '2.5', 'fontsize': '16', 'style': 'filled', 'fillcolor': 'lightgreen'}
EDGE_STYLE = {'fontname': CHINESE_FONT, 'penwidth': '2.5', 'fontsize': '12'}

# 创建ER图
print(f"使用字体 {CHINESE_FONT} 生成用户实体关系图...")
er = Digraph('用户实体关系图', format='png', encoding='utf-8')

# 设置全局属性
# 使用TB方向（从上到下）以更好地展示一对多关系
er.attr(rankdir='TB', size='20,15', fontname=CHINESE_FONT, penwidth='2', dpi='300', bgcolor='white')

# 确保所有元素都使用中文字体
er.graph_attr['fontname'] = CHINESE_FONT
er.node_attr['fontname'] = CHINESE_FONT
er.edge_attr['fontname'] = CHINESE_FONT

# 定义主实体 - 用户
er.node('用户', **ENTITY_STYLE)

# 定义用户实体的主要属性
er.node('用户ID', **ATTRIBUTE_STYLE)
er.node('用户名称', **ATTRIBUTE_STYLE)
er.node('用户密码', **ATTRIBUTE_STYLE)
er.node('性别', **ATTRIBUTE_STYLE)
er.node('生日', **ATTRIBUTE_STYLE)
er.node('最近登录时间', **ATTRIBUTE_STYLE)
er.node('最近登录IP', **ATTRIBUTE_STYLE)
er.node('用户等级', **ATTRIBUTE_STYLE)
er.node('用户昵称', **ATTRIBUTE_STYLE)
er.node('手机号码', **ATTRIBUTE_STYLE)
er.node('头像图片', **ATTRIBUTE_STYLE)
er.node('微信openid', **ATTRIBUTE_STYLE)
er.node('会话KEY', **ATTRIBUTE_STYLE)
er.node('状态', **ATTRIBUTE_STYLE)
er.node('创建时间', **ATTRIBUTE_STYLE)
er.node('更新时间', **ATTRIBUTE_STYLE)
er.node('逻辑删除', **ATTRIBUTE_STYLE)

# 建立用户实体与属性的连接
er.edge('用户ID', '用户', **EDGE_STYLE)
er.edge('用户名称', '用户', **EDGE_STYLE)
er.edge('用户密码', '用户', **EDGE_STYLE)
er.edge('性别', '用户', **EDGE_STYLE)
er.edge('生日', '用户', **EDGE_STYLE)
er.edge('最近登录时间', '用户', **EDGE_STYLE)
er.edge('最近登录IP', '用户', **EDGE_STYLE)
er.edge('用户等级', '用户', **EDGE_STYLE)
er.edge('用户昵称', '用户', **EDGE_STYLE)
er.edge('手机号码', '用户', **EDGE_STYLE)
er.edge('头像图片', '用户', **EDGE_STYLE)
er.edge('微信openid', '用户', **EDGE_STYLE)
er.edge('会话KEY', '用户', **EDGE_STYLE)
er.edge('状态', '用户', **EDGE_STYLE)
er.edge('创建时间', '用户', **EDGE_STYLE)
er.edge('更新时间', '用户', **EDGE_STYLE)
er.edge('逻辑删除', '用户', **EDGE_STYLE)

# 定义关联实体
# 1. 收货地址实体
er.node('收货地址', **ENTITY_STYLE)

# 收货地址属性
er.node('地址ID', **ATTRIBUTE_STYLE)
er.node('收货人名称', **ATTRIBUTE_STYLE)
er.node('省', **ATTRIBUTE_STYLE)
er.node('市', **ATTRIBUTE_STYLE)
er.node('区县', **ATTRIBUTE_STYLE)
er.node('详细地址', **ATTRIBUTE_STYLE)
er.node('地区编码', **ATTRIBUTE_STYLE)
er.node('邮政编码', **ATTRIBUTE_STYLE)
er.node('收货手机号码', **ATTRIBUTE_STYLE)
er.node('是否默认地址', **ATTRIBUTE_STYLE)

# 建立收货地址实体与属性的连接
er.edge('地址ID', '收货地址', **EDGE_STYLE)
er.edge('收货人名称', '收货地址', **EDGE_STYLE)
er.edge('省', '收货地址', **EDGE_STYLE)
er.edge('市', '收货地址', **EDGE_STYLE)
er.edge('区县', '收货地址', **EDGE_STYLE)
er.edge('详细地址', '收货地址', **EDGE_STYLE)
er.edge('地区编码', '收货地址', **EDGE_STYLE)
er.edge('邮政编码', '收货地址', **EDGE_STYLE)
er.edge('收货手机号码', '收货地址', **EDGE_STYLE)
er.edge('是否默认地址', '收货地址', **EDGE_STYLE)

# 2. 购物车实体
er.node('购物车', **ENTITY_STYLE)

# 购物车属性
er.node('购物车ID', **ATTRIBUTE_STYLE)
er.node('商品ID', **ATTRIBUTE_STYLE)
er.node('商品编号', **ATTRIBUTE_STYLE)
er.node('商品名称', **ATTRIBUTE_STYLE)
er.node('货品ID', **ATTRIBUTE_STYLE)
er.node('商品价格', **ATTRIBUTE_STYLE)
er.node('商品数量', **ATTRIBUTE_STYLE)
er.node('商品规格', **ATTRIBUTE_STYLE)
er.node('是否选中', **ATTRIBUTE_STYLE)
er.node('商品图片', **ATTRIBUTE_STYLE)

# 建立购物车实体与属性的连接
er.edge('购物车ID', '购物车', **EDGE_STYLE)
er.edge('商品ID', '购物车', **EDGE_STYLE)
er.edge('商品编号', '购物车', **EDGE_STYLE)
er.edge('商品名称', '购物车', **EDGE_STYLE)
er.edge('货品ID', '购物车', **EDGE_STYLE)
er.edge('商品价格', '购物车', **EDGE_STYLE)
er.edge('商品数量', '购物车', **EDGE_STYLE)
er.edge('商品规格', '购物车', **EDGE_STYLE)
er.edge('是否选中', '购物车', **EDGE_STYLE)
er.edge('商品图片', '购物车', **EDGE_STYLE)

# 3. 收藏实体
er.node('收藏', **ENTITY_STYLE)

# 收藏属性
er.node('收藏ID', **ATTRIBUTE_STYLE)
er.node('值ID', **ATTRIBUTE_STYLE)
er.node('收藏类型', **ATTRIBUTE_STYLE)

# 建立收藏实体与属性的连接
er.edge('收藏ID', '收藏', **EDGE_STYLE)
er.edge('值ID', '收藏', **EDGE_STYLE)
er.edge('收藏类型', '收藏', **EDGE_STYLE)

# 4. 评论实体
er.node('评论', **ENTITY_STYLE)

# 评论属性
er.node('评论ID', **ATTRIBUTE_STYLE)
er.node('评论值ID', **ATTRIBUTE_STYLE)
er.node('评论类型', **ATTRIBUTE_STYLE)
er.node('评论内容', **ATTRIBUTE_STYLE)
er.node('管理员回复', **ATTRIBUTE_STYLE)
er.node('是否含有图片', **ATTRIBUTE_STYLE)
er.node('图片地址列表', **ATTRIBUTE_STYLE)
er.node('评分', **ATTRIBUTE_STYLE)

# 建立评论实体与属性的连接
er.edge('评论ID', '评论', **EDGE_STYLE)
er.edge('评论值ID', '评论', **EDGE_STYLE)
er.edge('评论类型', '评论', **EDGE_STYLE)
er.edge('评论内容', '评论', **EDGE_STYLE)
er.edge('管理员回复', '评论', **EDGE_STYLE)
er.edge('是否含有图片', '评论', **EDGE_STYLE)
er.edge('图片地址列表', '评论', **EDGE_STYLE)
er.edge('评分', '评论', **EDGE_STYLE)

# 5. 售后实体
er.node('售后', **ENTITY_STYLE)

# 售后属性
er.node('售后ID', **ATTRIBUTE_STYLE)
er.node('售后编号', **ATTRIBUTE_STYLE)
er.node('订单ID', **ATTRIBUTE_STYLE)
er.node('售后类型', **ATTRIBUTE_STYLE)
er.node('退款原因', **ATTRIBUTE_STYLE)
er.node('退款金额', **ATTRIBUTE_STYLE)
er.node('售后状态', **ATTRIBUTE_STYLE)

# 建立售后实体与属性的连接
er.edge('售后ID', '售后', **EDGE_STYLE)
er.edge('售后编号', '售后', **EDGE_STYLE)
er.edge('订单ID', '售后', **EDGE_STYLE)
er.edge('售后类型', '售后', **EDGE_STYLE)
er.edge('退款原因', '售后', **EDGE_STYLE)
er.edge('退款金额', '售后', **EDGE_STYLE)
er.edge('售后状态', '售后', **EDGE_STYLE)

# 6. 优惠券用户使用实体
er.node('优惠券用户使用', **ENTITY_STYLE)

# 优惠券用户使用属性
er.node('用户优惠券ID', **ATTRIBUTE_STYLE)
er.node('优惠券ID', **ATTRIBUTE_STYLE)
er.node('使用状态', **ATTRIBUTE_STYLE)
er.node('使用时间', **ATTRIBUTE_STYLE)
er.node('有效期开始时间', **ATTRIBUTE_STYLE)
er.node('有效期结束时间', **ATTRIBUTE_STYLE)
er.node('优惠券订单ID', **ATTRIBUTE_STYLE)

# 建立优惠券用户使用实体与属性的连接
er.edge('用户优惠券ID', '优惠券用户使用', **EDGE_STYLE)
er.edge('优惠券ID', '优惠券用户使用', **EDGE_STYLE)
er.edge('使用状态', '优惠券用户使用', **EDGE_STYLE)
er.edge('使用时间', '优惠券用户使用', **EDGE_STYLE)
er.edge('有效期开始时间', '优惠券用户使用', **EDGE_STYLE)
er.edge('有效期结束时间', '优惠券用户使用', **EDGE_STYLE)
er.edge('优惠券订单ID', '优惠券用户使用', **EDGE_STYLE)

# 7. 用户足迹实体
er.node('用户足迹', **ENTITY_STYLE)

# 用户足迹属性
er.node('足迹ID', **ATTRIBUTE_STYLE)
er.node('足迹商品ID', **ATTRIBUTE_STYLE)

# 建立用户足迹实体与属性的连接
er.edge('足迹ID', '用户足迹', **EDGE_STYLE)
er.edge('足迹商品ID', '用户足迹', **EDGE_STYLE)

# 8. 意见反馈实体
er.node('意见反馈', **ENTITY_STYLE)

# 意见反馈属性
er.node('反馈ID', **ATTRIBUTE_STYLE)
er.node('反馈用户名称', **ATTRIBUTE_STYLE)
er.node('反馈手机号', **ATTRIBUTE_STYLE)
er.node('反馈类型', **ATTRIBUTE_STYLE)
er.node('反馈内容', **ATTRIBUTE_STYLE)
er.node('反馈状态', **ATTRIBUTE_STYLE)
er.node('反馈是否含有图片', **ATTRIBUTE_STYLE)
er.node('反馈图片地址列表', **ATTRIBUTE_STYLE)

# 建立意见反馈实体与属性的连接
er.edge('反馈ID', '意见反馈', **EDGE_STYLE)
er.edge('反馈用户名称', '意见反馈', **EDGE_STYLE)
er.edge('反馈手机号', '意见反馈', **EDGE_STYLE)
er.edge('反馈类型', '意见反馈', **EDGE_STYLE)
er.edge('反馈内容', '意见反馈', **EDGE_STYLE)
er.edge('反馈状态', '意见反馈', **EDGE_STYLE)
er.edge('反馈是否含有图片', '意见反馈', **EDGE_STYLE)
er.edge('反馈图片地址列表', '意见反馈', **EDGE_STYLE)

# 9. 搜索历史实体
er.node('搜索历史', **ENTITY_STYLE)

# 搜索历史属性
er.node('搜索历史ID', **ATTRIBUTE_STYLE)
er.node('搜索关键字', **ATTRIBUTE_STYLE)
er.node('搜索来源', **ATTRIBUTE_STYLE)

# 建立搜索历史实体与属性的连接
er.edge('搜索历史ID', '搜索历史', **EDGE_STYLE)
er.edge('搜索关键字', '搜索历史', **EDGE_STYLE)
er.edge('搜索来源', '搜索历史', **EDGE_STYLE)

# 定义关系（一对多关系）
# 由于所有关系都是一对多，我们使用简单的连线表示

# 用户与各实体的关系
# 使用不同的颜色区分不同类型的实体
PERSONAL_INFO_COLOR = 'blue'  # 个人信息类
SHOPPING_COLOR = 'green'      # 购物行为类
TRANSACTION_COLOR = 'red'     # 交易相关类
FEEDBACK_COLOR = 'purple'     # 反馈互动类

# 建立用户与各实体的连接（一对多关系）
er.edge('用户', '收货地址', label='拥有多个', color=PERSONAL_INFO_COLOR, **EDGE_STYLE)
er.edge('用户', '购物车', label='拥有多个', color=SHOPPING_COLOR, **EDGE_STYLE)
er.edge('用户', '收藏', label='收藏多个', color=SHOPPING_COLOR, **EDGE_STYLE)
er.edge('用户', '评论', label='发表多个', color=TRANSACTION_COLOR, **EDGE_STYLE)
er.edge('用户', '售后', label='申请多个', color=TRANSACTION_COLOR, **EDGE_STYLE)
er.edge('用户', '优惠券用户使用', label='拥有多个', color=TRANSACTION_COLOR, **EDGE_STYLE)
er.edge('用户', '用户足迹', label='留下多个', color=SHOPPING_COLOR, **EDGE_STYLE)
er.edge('用户', '意见反馈', label='提交多个', color=FEEDBACK_COLOR, **EDGE_STYLE)
er.edge('用户', '搜索历史', label='拥有多个', color=SHOPPING_COLOR, **EDGE_STYLE)

# 渲染并保存图形
er.render('用户实体关系图', view=True)
print("用户实体关系图生成完成！")

# 清理测试文件
import glob
for f in glob.glob('font_test*'):
    os.remove(f)

print("生成的图片文件: 用户实体关系图.png")
print("生成的DOT文件: 用户实体关系图")
print("\n实体分类说明:")
print("蓝色连线: 个人信息管理类实体")
print("绿色连线: 购物行为类实体") 
print("红色连线: 交易相关类实体")
print("紫色连线: 反馈互动类实体")