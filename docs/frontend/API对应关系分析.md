# 前端与后端API对应关系分析

## 概述

本文档详细分析litemall项目前端商品管理和统计管理模块与后端API的对应关系，包括接口映射、参数传递、数据格式转换等关键信息。

## 一、商品管理模块API对应关系

### 1.1 商品列表查询API

#### 前端调用代码
```javascript
// api/goods.js
export function listGoods(query) {
  return request({
    url: '/goods/list',
    method: 'get',
    params: query
  })
}

// 调用示例（商品列表页面）
listGoods({
  page: 1,
  limit: 20,
  goodsSn: 'G001',
  name: '手机',
  sort: 'add_time',
  order: 'desc'
})
```

#### 后端接口信息
| 项目 | 详细信息 |
|------|----------|
| **接口路径** | `/admin/goods/list` |
| **HTTP方法** | GET |
| **控制器类** | `AdminGoodsController.list()` |
| **服务类** | `AdminGoodsService.list()` |
| **权限要求** | `GET /admin/goods/list` |

#### 参数映射关系
| 前端参数名 | 后端参数名 | 数据类型 | 是否必需 | 描述 |
|-----------|------------|----------|----------|------|
| `page` | `page` | Integer | 否 | 页码，默认1 |
| `limit` | `limit` | Integer | 否 | 每页数量，默认10 |
| `goodsSn` | `goodsSn` | String | 否 | 商品编号模糊查询 |
| `name` | `name` | String | 否 | 商品名称模糊查询 |
| `sort` | `sort` | String | 否 | 排序字段，默认add_time |
| `order` | `order` | String | 否 | 排序方向，默认desc |

#### 响应数据格式
```javascript
{
  "errno": 0,
  "data": {
    "total": 100,           // 商品总数
    "pages": 5,             // 总页数
    "limit": 20,             // 每页数量
    "page": 1,               // 当前页码
    "list": [                // 商品列表
      {
        "id": 1,
        "name": "小米手机",
        "goodsSn": "G001",
        "categoryId": 1,
        "brandId": 1,
        "gallery": ["url1", "url2"],
        "keywords": "手机,小米",
        "brief": "高性能智能手机",
        "isOnSale": true,
        "sortOrder": 100,
        "picUrl": "pic_url",
        "shareUrl": "share_url",
        "isNew": true,
        "isHot": false,
        "unit": "台",
        "counterPrice": 2999.00,
        "retailPrice": 2599.00,
        "detail": "<p>商品详情HTML</p>",
        "addTime": "2023-01-01 10:00:00",
        "updateTime": "2023-01-01 10:00:00",
        "deleted": false
      }
    ]
  },
  "errmsg": "成功"
}
```

### 1.2 商品删除API

#### 前端调用代码
```javascript
// api/goods.js
export function deleteGoods(data) {
  return request({
    url: '/goods/delete',
    method: 'post',
    data
  })
}

// 调用示例
deleteGoods({ id: 1 })
```

#### 后端接口信息
| 项目 | 详细信息 |
|------|----------|
| **接口路径** | `/admin/goods/delete` |
| **HTTP方法** | POST |
| **控制器类** | `AdminGoodsController.delete()` |
| **服务类** | `AdminGoodsService.delete()` |
| **权限要求** | `POST /admin/goods/delete` |

#### 参数映射关系
| 前端参数名 | 后端参数名 | 数据类型 | 是否必需 | 描述 |
|-----------|------------|----------|----------|------|
| `id` | `id` | Integer | 是 | 商品ID |

### 1.3 商品创建API

#### 前端调用代码
```javascript
// api/goods.js
export function publishGoods(data) {
  return request({
    url: '/goods/create',
    method: 'post',
    data
  })
}

// 调用示例
publishGoods({
  goods: {
    name: "新商品",
    goodsSn: "G002",
    // ... 其他商品字段
  },
  specifications: [...],
  attributes: [...],
  products: [...]
})
```

#### 后端接口信息
| 项目 | 详细信息 |
|------|----------|
| **接口路径** | `/admin/goods/create` |
| **HTTP方法** | POST |
| **控制器类** | `AdminGoodsController.create()` |
| **服务类** | `AdminGoodsService.create()` |
| **权限要求** | `POST /admin/goods/create` |

#### 参数数据结构
```javascript
{
  "goods": {
    "name": "商品名称",
    "goodsSn": "商品编号",
    "categoryId": 1,
    "brandId": 1,
    "gallery": ["图片1", "图片2"],
    "keywords": "关键词",
    "brief": "商品简介",
    "isOnSale": true,
    "picUrl": "主图URL",
    "shareUrl": "分享URL",
    "isNew": true,
    "isHot": false,
    "unit": "单位",
    "counterPrice": 100.00,
    "retailPrice": 80.00,
    "detail": "商品详情HTML"
  },
  "specifications": [
    {
      "specification": "规格名称",
      "value": "规格值",
      "picUrl": "规格图片"
    }
  ],
  "attributes": [
    {
      "attribute": "属性名",
      "value": "属性值"
    }
  ],
  "products": [
    {
      "specifications": ["规格1", "规格2"],
      "price": 80.00,
      "number": 100,
      "url": "货品图片"
    }
  ]
}
```

## 二、统计管理模块API对应关系

### 2.1 用户统计API

#### 前端调用代码
```javascript
// api/stat.js
export function statUser(query) {
  return request({
    url: '/stat/user',
    method: 'get',
    params: query
  })
}

// 调用示例
statUser()
```

#### 后端接口信息
| 项目 | 详细信息 |
|------|----------|
| **接口路径** | `/admin/stat/user` |
| **HTTP方法** | GET |
| **控制器类** | `AdminStatController.statUser()` |
| **服务类** | `StatService.statUser()` |
| **权限要求** | `GET /admin/stat/user` |

#### 响应数据格式
```javascript
{
  "errno": 0,
  "data": {
    "columns": ["day", "users"],  // 数据列定义
    "rows": [                       // 数据行
      {
        "day": "2023-01-01",       // 日期
        "users": 10                 // 新增用户数
      },
      {
        "day": "2023-01-02",
        "users": 15
      }
    ]
  },
  "errmsg": "成功"
}
```

### 2.2 订单统计API

#### 前端调用代码
```javascript
// api/stat.js
export function statOrder(query) {
  return request({
    url: '/stat/order',
    method: 'get',
    params: query
  })
}
```

#### 后端接口信息
| 项目 | 详细信息 |
|------|----------|
| **接口路径** | `/admin/stat/order` |
| **HTTP方法** | GET |
| **控制器类** | `AdminStatController.statOrder()` |
| **服务类** | `StatService.statOrder()` |
| **权限要求** | `GET /admin/stat/order` |

#### 响应数据格式
```javascript
{
  "errno": 0,
  "data": {
    "columns": ["day", "orders", "customers", "amount", "pcr"],
    "rows": [
      {
        "day": "2023-01-01",
        "orders": 5,           // 订单数
        "customers": 3,        // 客户数
        "amount": 1000.00,     // 订单金额
        "pcr": 333.33          // 客单价
      }
    ]
  },
  "errmsg": "成功"
}
```

### 2.3 商品统计API

#### 前端调用代码
```javascript
// api/stat.js
export function statGoods(query) {
  return request({
    url: '/stat/goods',
    method: 'get',
    params: query
  })
}
```

#### 后端接口信息
| 项目 | 详细信息 |
|------|----------|
| **接口路径** | `/admin/stat/goods` |
| **HTTP方法** | GET |
| **控制器类** | `AdminStatController.statGoods()` |
| **服务类** | `StatService.statGoods()` |
| **权限要求** | `GET /admin/stat/goods` |

#### 响应数据格式
```javascript
{
  "errno": 0,
  "data": {
    "columns": ["day", "orders", "products", "amount"],
    "rows": [
      {
        "day": "2023-01-01",
        "orders": 5,           // 订单数
        "products": 10,        // 商品数
        "amount": 2000.00     // 销售金额
      }
    ]
  },
  "errmsg": "成功"
}
```

## 三、API调用流程分析

### 3.1 请求拦截器处理

```javascript
// utils/request.js
service.interceptors.request.use(
  config => {
    // 添加认证token
    if (store.getters.token) {
      config.headers['X-Litemall-Admin-Token'] = getToken()
    }
    return config
  }
)
```

### 3.2 响应拦截器处理

```javascript
// utils/request.js
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 业务成功处理
    if (res.errno === 0) {
      return res
    } else {
      // 业务错误处理
      Message.error(res.errmsg || 'Error')
      return Promise.reject(new Error(res.errmsg || 'Error'))
    }
  },
  error => {
    // 网络错误处理
    Message.error(error.message)
    return Promise.reject(error)
  }
)
```

## 四、错误处理机制

### 4.1 统一错误码处理

| 错误码 | 含义 | 前端处理方式 |
|--------|------|-------------|
| 0 | 成功 | 正常处理数据 |
| 401 | 未授权 | 跳转到登录页面 |
| 403 | 权限不足 | 显示权限不足提示 |
| 500 | 服务器错误 | 显示服务器错误提示 |

### 4.2 前端错误处理示例

```javascript
// 商品列表页面错误处理
listGoods(this.listQuery).then(response => {
  // 成功处理
  this.list = response.data.data.list
  this.total = response.data.data.total
}).catch(error => {
  // 错误处理
  this.$notify.error({
    title: '失败',
    message: error.message || '请求失败'
  })
  this.list = []
  this.total = 0
})
```

## 五、数据格式转换

### 5.1 前端到后端数据转换

#### 商品创建数据转换
```javascript
// 前端数据结构
{
  name: "商品名称",
  price: "100.00",  // 字符串格式
  isOnSale: true
}

// 后端期望结构
{
  "name": "商品名称",
  "price": 100.00,   // 数字格式
  "isOnSale": true
}
```

### 5.2 后端到前端数据转换

#### 统计数据处理
```javascript
// 后端返回数据
{
  "columns": ["day", "users"],
  "rows": [
    {"day": "2023-01-01", "users": 10}
  ]
}

// 前端图表组件需要的数据格式
{
  "columns": ["日期", "用户数"],  // 中文列名
  "rows": [
    {"日期": "2023-01-01", "用户数": 10}
  ]
}
```

## 六、总结

通过以上分析，可以看出litemall项目的前端与后端API对应关系设计合理：

1. **接口设计规范**：遵循RESTful API设计原则
2. **参数映射清晰**：前后端参数命名一致
3. **错误处理完善**：统一的错误码和错误信息
4. **数据格式统一**：前后端数据格式转换规范
5. **权限控制严格**：接口级别的权限验证

这种设计使得前端开发人员能够清晰地了解每个API的用途、参数和返回值，提高了开发效率和代码质量。