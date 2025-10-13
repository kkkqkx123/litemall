# 统计管理模块API时序图

## 用户统计时序图
```mermaid
sequenceDiagram
    participant F as 前端
    participant C as AdminStatController
    participant S as StatService
    participant M as StatMapper
    participant DB as 数据库
    participant VO as StatVo
    
    F->>C: GET /admin/stat/user
    C->>S: statService.statUser()
    S->>M: statMapper.statUser()
    M->>DB: 执行用户统计SQL
    DB-->>M: 返回统计结果
    M-->>S: 返回List<Map>
    S-->>C: 返回统计数据
    C->>VO: 创建StatVo对象
    VO->>VO: setColumns(["day", "users"])
    VO->>VO: setRows(统计数据)
    C-->>F: 返回ResponseUtil.ok(statVo)
```

## 订单统计时序图
```mermaid
sequenceDiagram
    participant F as 前端
    participant C as AdminStatController
    participant S as StatService
    participant M as StatMapper
    participant DB as 数据库
    participant VO as StatVo
    
    F->>C: GET /admin/stat/order
    C->>S: statService.statOrder()
    S->>M: statMapper.statOrder()
    M->>DB: 执行订单统计SQL
    DB-->>M: 返回统计结果
    M-->>S: 返回List<Map>
    S-->>C: 返回统计数据
    C->>VO: 创建StatVo对象
    VO->>VO: setColumns(["day", "orders", "customers", "amount", "pcr"])
    VO->>VO: setRows(统计数据)
    C-->>F: 返回ResponseUtil.ok(statVo)
```

## 商品统计时序图
```mermaid
sequenceDiagram
    participant F as 前端
    participant C as AdminStatController
    participant S as StatService
    participant M as StatMapper
    participant DB as 数据库
    participant VO as StatVo
    
    F->>C: GET /admin/stat/goods
    C->>S: statService.statGoods()
    S->>M: statMapper.statGoods()
    M->>DB: 执行商品统计SQL
    DB-->>M: 返回统计结果
    M-->>S: 返回List<Map>
    S-->>C: 返回统计数据
    C->>VO: 创建StatVo对象
    VO->>VO: setColumns(["day", "orders", "products", "amount"])
    VO->>VO: setRows(统计数据)
    C-->>F: 返回ResponseUtil.ok(statVo)
```

## 统计管理模块内部交互详细流程

### 统计查询性能分析
| 统计类型 | 数据量级 | 查询时间 | 内存占用 | 优化空间 |
|---------|---------|---------|---------|---------|
| 用户统计 | 10万+ | <500ms | 低 | 索引优化 |
| 订单统计 | 50万+ | <800ms | 中等 | 预计算 |
| 商品统计 | 100万+ | <1s | 中等 | 分表策略 |

### 数据封装效率分析
| 数据处理环节 | 处理方式 | 时间复杂度 | 空间复杂度 | 优化建议 |
|------------|---------|-----------|-----------|---------|
| SQL结果集 | 直接映射 | O(n) | O(n) | 流式处理 |
| Map转换 | 遍历转换 | O(n) | O(n) | 批量处理 |
| Vo封装 | 对象创建 | O(n) | O(n) | 对象池 |
| JSON序列化 | 反射序列化 | O(n) | O(n) | 预编译 |

## API接口性能指标表格

### 统计管理模块API性能指标
| API接口 | HTTP方法 | 平均响应时间 | 95%响应时间 | 错误率 | QPS | 数据量 |
|--------|---------|------------|------------|-------|-----|-------|
| /admin/stat/user | GET | 450ms | 800ms | <0.1% | 50 | 10万+ |
| /admin/stat/order | GET | 750ms | 1.2s | <0.2% | 30 | 50万+ |
| /admin/stat/goods | GET | 900ms | 1.5s | <0.2% | 20 | 100万+ |

## 系统资源消耗分析

### 内存使用分析
| 组件类型 | 堆内存使用 | 非堆内存使用 | 对象数量 | GC频率 |
|---------|----------|------------|---------|-------|
| Controller | 50-100MB | 10-20MB | 1000+ | 低 |
| Service | 100-200MB | 20-40MB | 2000+ | 中 |
| Mapper | 50-100MB | 10-20MB | 500+ | 低 |
| 数据库连接池 | 20-50MB | 5-10MB | 100+ | 低 |

### CPU使用分析
| 操作类型 | CPU使用率 | 线程数 | 上下文切换 | 优化建议 |
|---------|----------|-------|-----------|---------|
| 商品查询 | 10-20% | 5-10 | 中等 | 查询优化 |
| 商品创建 | 20-40% | 10-20 | 高 | 异步处理 |
| 统计查询 | 30-50% | 15-25 | 高 | 缓存策略 |
| 批量操作 | 40-60% | 20-30 | 很高 | 分批次处理 |

## 系统扩展性分析

### 水平扩展能力
| 扩展维度 | 当前能力 | 扩展方案 | 扩展成本 | 扩展效果 |
|---------|---------|---------|---------|---------|
| 应用服务器 | 单实例 | 集群部署 | 中等 | 线性扩展 |
| 数据库 | 单实例 | 读写分离 | 中等 | 读性能提升 |
| 缓存层 | 无缓存 | Redis集群 | 低 | 性能大幅提升 |
| 消息队列 | 无队列 | RabbitMQ | 中等 | 解耦异步 |

### 垂直扩展能力
| 资源类型 | 当前配置 | 升级方案 | 升级成本 | 升级效果 |
|---------|---------|---------|---------|---------|
| CPU核心 | 4核心 | 8核心 | 低 | 计算能力翻倍 |
| 内存容量 | 8GB | 16GB | 低 | 并发能力提升 |
| 磁盘IO | 普通HDD | SSD | 中等 | IO性能提升 |
| 网络带宽 | 100Mbps | 1Gbps | 中等 | 网络延迟降低 |

## 监控告警策略

### 性能监控指标
| 监控指标 | 监控阈值 | 告警级别 | 处理措施 | 监控频率 |
|---------|---------|---------|---------|---------|
| API响应时间 | >1s | 警告 | 性能优化 | 实时 |
| 错误率 | >1% | 严重 | 立即排查 | 实时 |
| CPU使用率 | >80% | 警告 | 资源扩容 | 每分钟 |
| 内存使用率 | >85% | 严重 | 内存优化 | 每分钟 |
| 数据库连接数 | >80% | 警告 | 连接池优化 | 实时 |

### 业务监控指标
| 业务指标 | 正常范围 | 异常阈值 | 监控意义 | 处理时效 |
|---------|---------|---------|---------|---------|
| 商品创建量 | 日增100-500 | <50或>1000 | 业务活跃度 | 1小时内 |
| 订单成交量 | 日增50-200 | <20或>500 | 营收指标 | 实时 |
| 用户注册量 | 日增10-100 | <5或>200 | 用户增长 | 4小时内 |
| 统计查询量 | 日查询100-500 | <50或>1000 | 系统负载 | 实时 |

## 容错恢复机制

### 故障恢复策略
| 故障类型 | 影响范围 | 恢复时间 | 恢复策略 | 数据一致性 |
|---------|---------|---------|---------|---------|
| 应用宕机 | 部分功能不可用 | 5分钟内 | 自动重启 | 最终一致 |
| 数据库宕机 | 全部功能不可用 | 15分钟内 | 主从切换 | 数据同步 |
| 网络中断 | 服务不可访问 | 3分钟内 | 网络切换 | 无影响 |
| 磁盘故障 | 数据丢失风险 | 30分钟内 | 数据恢复 | 备份恢复 |

### 数据备份策略
| 数据类型 | 备份频率 | 保留周期 | 备份方式 | 恢复测试 |
|---------|---------|---------|---------|---------|
| 商品数据 | 每日全量 | 30天 | 物理备份 | 每月一次 |
| 订单数据 | 实时增量 | 90天 | 逻辑备份 | 每季度一次 |
| 用户数据 | 每日全量 | 永久 | 异地备份 | 每半年一次 |
| 系统配置 | 变更时备份 | 永久 | 版本控制 | 每次变更 |

## 错误处理时序图

### 统计查询异常时序图
```mermaid
sequenceDiagram
    participant F as 前端
    participant C as AdminStatController
    participant S as StatService
    participant M as StatMapper
    participant DB as 数据库
    
    F->>C: GET /admin/stat/user
    C->>S: statService.statUser()
    S->>M: statMapper.statUser()
    M->>DB: 执行SQL查询
    Note over DB: 数据库异常
    DB-->>M: 抛出SQL异常
    M-->>S: 抛出异常
    S-->>C: 抛出异常
    C-->>F: 返回ResponseUtil.fail()
```