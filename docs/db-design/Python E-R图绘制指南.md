# Python E-R图绘制指南

## 概述

本文档介绍如何使用Python库来绘制符合litemall系统数据库设计要求的E-R图。根据您的需求，我们推荐使用专门的数据库E-R图工具，这些工具能够更好地满足数据库设计的专业需求。

## 推荐的Python库

### 1. dbdiagram.io (推荐)

**特点：**
- 专门为数据库E-R图设计
- 使用DBML（Database Markup Language）语法
- 支持在线编辑和API调用
- 生成专业美观的E-R图

**安装和使用：**
```python
# 通过API调用dbdiagram.io
import requests

# 创建E-R图的DBML代码
dbml_content = """
// litemall系统E-R图
// 用户相关表
Table litemall_user {
  id integer [primary key]
  username varchar
  password varchar
  gender tinyint
  birthday date
  last_login_time datetime
  last_login_ip varchar
  user_level tinyint
  nickname varchar
  mobile varchar
  avatar varchar
  weixin_openid varchar
  session_key varchar
  status tinyint
  add_time datetime
  update_time datetime
  deleted boolean
}

// 商品相关表
Table litemall_goods {
  id integer [primary key]
  goods_sn varchar
  name varchar
  category_id integer
  brand_id integer
  gallery text
  keywords varchar
  brief varchar
  is_on_sale boolean
  sort_order smallint
  pic_url varchar
  share_url varchar
  is_new boolean
  is_hot boolean
  unit varchar
  counter_price decimal
  retail_price decimal
  detail text
  add_time datetime
  update_time datetime
  deleted boolean
}

// 订单相关表
Table litemall_order {
  id integer [primary key]
  user_id integer
  order_sn varchar
  order_status smallint
  aftersale_status smallint
  consignee varchar
  mobile varchar
  address varchar
  message varchar
  goods_price decimal
  freight_price decimal
  coupon_price decimal
  integral_price decimal
  groupon_price decimal
  order_price decimal
  actual_price decimal
  pay_id varchar
  pay_time datetime
  ship_sn varchar
  ship_channel varchar
  ship_time datetime
  confirm_time datetime
  comments smallint
  end_time datetime
  add_time datetime
  update_time datetime
  deleted boolean
}

// 关系定义
Ref: litemall_order.user_id > litemall_user.id
Ref: litemall_goods.category_id > litemall_category.id
Ref: litemall_goods.brand_id > litemall_brand.id
"""

# 通过API创建图表
api_token = "YOUR_API_TOKEN"  # 需要注册获取
url = "https://api.dbdiagram.io/v1/diagrams"

headers = {
    "dbdiagram-access-token": api_token,
    "Content-Type": "application/json"
}

data = {
    "name": "litemall系统E-R图",
    "content": dbml_content
}

response = requests.post(url, headers=headers, json=data)
print(response.json())
```

### 2. Diagrams库

**特点：**
- 基于Graphviz的Python库
- 支持系统架构图绘制
- 可以自定义节点和连接

**安装：**
```bash
pip install diagrams
```

**使用示例：**
```python
from diagrams import Diagram, Cluster, Edge
from diagrams.custom import Custom

# 创建E-R图风格的数据模型图
with Diagram("litemall系统数据模型", show=False, filename="litemall_er_diagram"):
    
    with Cluster("用户模块"):
        user = Custom("用户表", "user_icon.png")
        address = Custom("地址表", "address_icon.png")
        collect = Custom("收藏表", "collect_icon.png")
        
        user >> address
        user >> collect
    
    with Cluster("商品模块"):
        goods = Custom("商品表", "goods_icon.png")
        category = Custom("分类表", "category_icon.png")
        brand = Custom("品牌表", "brand_icon.png")
        
        category >> goods
        brand >> goods
    
    with Cluster("订单模块"):
        order = Custom("订单表", "order_icon.png")
        order_goods = Custom("订单商品表", "order_goods_icon.png")
        cart = Custom("购物车表", "cart_icon.png")
        
        order >> order_goods
        cart >> order
    
    # 定义模块间关系
    user >> Edge(label="创建") >> order
    goods >> Edge(label="包含") >> order_goods
```

### 3. Mermaid-JS + Python

**特点：**
- 使用Mermaid语法生成E-R图
- 可以在Jupyter Notebook中直接显示
- 语法简洁易学

**安装：**
```bash
pip install mermaid
```

**使用示例：**
```python
import mermaid

# Mermaid E-R图语法
er_diagram = """
erDiagram
    USER ||--o{ ADDRESS : has
    USER ||--o{ COLLECT : has
    USER ||--o{ ORDER : creates
    
    CATEGORY ||--o{ GOODS : contains
    BRAND ||--o{ GOODS : produces
    
    ORDER ||--o{ ORDER_GOODS : includes
    GOODS ||--o{ ORDER_GOODS : belongs_to
    
    USER {
        int id PK
        string username
        string password
        tinyint gender
        date birthday
        datetime add_time
    }
    
    GOODS {
        int id PK
        string name
        int category_id FK
        int brand_id FK
        decimal retail_price
        boolean is_on_sale
    }
    
    ORDER {
        int id PK
        int user_id FK
        string order_sn
        smallint order_status
        decimal actual_price
        datetime add_time
    }
"""

# 生成图表
mermaid.diagram(er_diagram, "litemall_er_diagram")
```

## 符合要求的E-R图绘制规范

### 1. 图形连接方法
根据您的要求，E-R图应使用以下连接方式：

```
实体 -- 联系 -- 实体
```

**示例：**
```
用户 -- 购买 -- 商品
用户 -- 拥有 -- 地址
商品 -- 属于 -- 分类
```

### 2. 关系模式转换原则

使用Markdown表格格式列出关系模式：

| 关系模式名 | 属性列表 | 主键 | 外键 |
|-----------|---------|------|------|
| 用户(USER) | id, username, password, gender, birthday, last_login_time | id | 无 |
| 商品(GOODS) | id, name, category_id, brand_id, retail_price, is_on_sale | id | category_id, brand_id |
| 订单(ORDER) | id, user_id, order_sn, order_status, actual_price, add_time | id | user_id |

### 3. 物理模型要求

主要表的物理模型示例：

**用户表物理模型：**
```sql
CREATE TABLE litemall_user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(63) NOT NULL,
    password VARCHAR(63) NOT NULL,
    gender TINYINT DEFAULT 0,
    birthday DATE,
    last_login_time DATETIME,
    last_login_ip VARCHAR(63),
    user_level TINYINT DEFAULT 0,
    nickname VARCHAR(63),
    mobile VARCHAR(20),
    avatar VARCHAR(255),
    weixin_openid VARCHAR(63),
    session_key VARCHAR(100),
    status TINYINT DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_username (username),
    INDEX idx_mobile (mobile),
    INDEX idx_weixin_openid (weixin_openid)
);
```

**商品表物理模型：**
```sql
CREATE TABLE litemall_goods (
    id INT PRIMARY KEY AUTO_INCREMENT,
    goods_sn VARCHAR(63) NOT NULL,
    name VARCHAR(127) NOT NULL,
    category_id INT NOT NULL,
    brand_id INT,
    gallery TEXT,
    keywords VARCHAR(255),
    brief VARCHAR(255),
    is_on_sale BOOLEAN DEFAULT TRUE,
    sort_order SMALLINT DEFAULT 100,
    pic_url VARCHAR(255),
    share_url VARCHAR(255),
    is_new BOOLEAN DEFAULT FALSE,
    is_hot BOOLEAN DEFAULT FALSE,
    unit VARCHAR(31) DEFAULT '件',
    counter_price DECIMAL(10,2) DEFAULT 0.00,
    retail_price DECIMAL(10,2) DEFAULT 0.00,
    detail TEXT,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_category_id (category_id),
    INDEX idx_brand_id (brand_id),
    INDEX idx_is_on_sale (is_on_sale),
    UNIQUE KEY uk_goods_sn (goods_sn)
);
```

## 实践建议

### 1. 针对litemall系统的优化建议

1. **模块化设计**：将34张表按功能模块分组绘制
2. **简化表示**：用户和管理员实体在其他实体较多时可以不单独画出
3. **属性选择**：每个关系模式列出5-6个主要属性
4. **关系清晰**：确保实体间的关系连线不交叉、不拐弯

### 2. 工具选择建议

- **初学者**：推荐使用dbdiagram.io，界面友好，学习成本低
- **开发人员**：推荐使用Diagrams库，可以集成到Python项目中
- **文档编写**：推荐使用Mermaid，便于在Markdown文档中嵌入

### 3. 符合要求的注意事项

1. **非空约束**：无需在图中标注非空约束
2. **属性表示**：属性无需在图中画出，在关系模式表格中说明
3. **连线规范**：确保关系连线不拐弯
4. **重复内容**：如有重复内容，单独说明

## 总结

通过使用上述Python库，您可以高效地绘制符合要求的litemall系统E-R图。建议根据具体需求选择合适的工具，并遵循文档中提到的规范和最佳实践。