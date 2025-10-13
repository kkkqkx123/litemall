# Physical Model Design

## User Table (litemall_user)

| Field Name | Type | Nullable | Description |
|------------|------|----------|-------------|
| id | int | No | Primary key |
| username | varchar(63) | No | User name |
| password | varchar(63) | No | User password |
| gender | tinyint | No | Gender: 0 - unknown, 1 - male, 2 - female |
| birthday | date | Yes | Birthday |
| last_login_time | datetime | Yes | Last login time |
| last_login_ip | varchar(63) | No | Last login IP |
| user_level | tinyint | No | User level |
| nickname | varchar(63) | No | Nickname |
| mobile | varchar(20) | No | Mobile phone number |
| avatar | varchar(255) | No | Avatar image URL |
| weixin_openid | varchar(63) | No | WeChat openid |
| session_key | varchar(100) | No | WeChat session key |
| status | tinyint | No | Status: 0 - available, 1 - disabled, 2 - deleted |
| add_time | datetime | Yes | Creation time |
| update_time | datetime | Yes | Update time |
| deleted | tinyint | No | Logical deletion: 0 - not deleted, 1 - deleted |

## Goods Table (litemall_goods)

| Field Name | Type | Nullable | Description |
|------------|------|----------|-------------|
| id | int | No | Primary key |
| goods_sn | varchar(63) | No | Goods serial number |
| name | varchar(127) | No | Goods name |
| category_id | int | No | Category ID (foreign key -> litemall_category.id) |
| brand_id | int | No | Brand ID (foreign key -> litemall_brand.id) |
| gallery | varchar(1023) | Yes | Gallery images URLs |
| keywords | varchar(255) | Yes | Keywords |
| brief | varchar(255) | Yes | Brief description |
| is_on_sale | tinyint | No | Is on sale: 0 - not on sale, 1 - on sale |
| sort_order | smallint | No | Sort order |
| pic_url | varchar(255) | Yes | Picture URL |
| share_url | varchar(255) | Yes | Share URL |
| is_new | tinyint | No | Is new: 0 - not new, 1 - new |
| is_hot | tinyint | No | Is hot: 0 - not hot, 1 - hot |
| unit | varchar(31) | No | Unit |
| counter_price | decimal(10,2) | No | Counter price |
| retail_price | decimal(10,2) | No | Retail price |
| detail | text | Yes | Detailed description |
| add_time | datetime | Yes | Creation time |
| update_time | datetime | Yes | Update time |
| deleted | tinyint | No | Logical deletion: 0 - not deleted, 1 - deleted |