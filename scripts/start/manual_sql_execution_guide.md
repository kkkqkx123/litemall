# Litemall 数据库手动执行 SQL 命令指南

## 概述
本文档提供了在 Docker 环境中手动执行 Litemall 项目 SQL 文件的完整命令指南。适用于开发环境搭建、数据库初始化或故障排查场景。

## 环境信息
- **数据库地址**: `localhost:3306`
- **Docker 容器名称**: `mysql`
- **MySQL root 用户名**: `root`
- **MySQL root 密码**: `root`
- **目标数据库**: `litemall`
- **执行方式**: 通过 Docker exec 命令执行

## 执行前准备

### 1. 检查 Docker 状态
```powershell
# 检查 Docker 是否运行
docker ps

# 如果 MySQL 容器未运行，启动它
docker start mysql-container-name
```

### 2. 检查 MySQL 容器状态
```powershell
# 检查 MySQL 容器是否运行
docker ps | findstr mysql

# 如果容器未运行，启动它
docker start mysql
```

## 执行流程

### 快速执行所有 SQL 文件（使用 Docker）
```powershell
# 1. 切换到项目目录
cd d:\项目\Spring\litemall

# 2. 按顺序执行所有 SQL 文件（使用 Docker exec）
docker exec -i mysql mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS litemall;"
Get-Content litemall-db/sql/litemall_schema.sql | docker exec -i mysql mysql -u root -proot litemall
Get-Content litemall-db/sql/litemall_table.sql | docker exec -i mysql mysql -u root -proot litemall
Get-Content litemall-db/sql/litemall_data.sql | docker exec -i mysql mysql -u root -proot litemall
Get-Content litemall-db/sql/insert_test_orders.sql | docker exec -i mysql mysql -u root -proot litemall
Get-Content litemall-db/sql/qa_optimization_indexes_fixed.sql | docker exec -i mysql mysql -u root -proot litemall
Get-Content litemall-db/sql/qa_optimization_tables.sql | docker exec -i mysql mysql -u root -proot litemall
Get-Content litemall-db/sql/supplement_comments.sql | docker exec -i mysql mysql -u root -proot litemall
```

### 验证执行结果
```powershell
# 检查数据库和表
docker exec -i mysql mysql -u root -proot litemall -e "SHOW TABLES;"

# 检查数据量
docker exec -i mysql mysql -u root -proot litemall -e "SELECT '商品数量' as category, COUNT(*) as count FROM litemall_goods UNION ALL SELECT '订单数量', COUNT(*) FROM litemall_order UNION ALL SELECT '用户数量', COUNT(*) FROM litemall_user UNION ALL SELECT '评论数量', COUNT(*) FROM litemall_comment;"

# 检查总表数量
docker exec -i mysql mysql -u root -proot litemall -e "SELECT COUNT(*) as total_tables FROM information_schema.tables WHERE table_schema = 'litemall';"
```

## 常见问题解决

### 1. 连接失败
```powershell
# 检查 MySQL 服务状态
docker ps | grep mysql

# 重启 MySQL 容器
docker restart mysql-container-name
```

### 2. 权限问题
```sql
-- 在 MySQL 中执行
GRANT ALL PRIVILEGES ON litemall.* TO 'kkkqkx'@'%';
FLUSH PRIVILEGES;
```

### 3. 字符集问题
```sql
-- 检查数据库字符集
SHOW VARIABLES LIKE 'character_set_database';
SHOW VARIABLES LIKE 'collation_database';

-- 如果字符集不正确，修改数据库
ALTER DATABASE litemall CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 注意事项

1. **执行顺序很重要**: 必须按照文档中的顺序执行 SQL 文件
2. **备份数据**: 在执行前建议备份现有数据
3. **测试环境**: 先在测试环境验证所有命令
4. **字符集**: 确保使用 utf8mb4 字符集支持 emoji
5. **权限**: 确保用户有足够的权限执行所有操作

## 执行结果验证

### 成功执行后的数据概览
执行完所有 SQL 文件后，数据库应包含以下数据：

```
表数量: 36 张
商品数量: 239 条
订单数量: 15 条  
用户数量: 6 条
评论数量: 1022 条
```

### 验证命令
```powershell
# 查看所有表
docker exec -i mysql mysql -u root -proot litemall -e "SHOW TABLES;"

# 查看数据概览
docker exec -i mysql mysql -u root -proot litemall -e "SELECT '商品数量' as category, COUNT(*) as count FROM litemall_goods UNION ALL SELECT '订单数量', COUNT(*) FROM litemall_order UNION ALL SELECT '用户数量', COUNT(*) FROM litemall_user UNION ALL SELECT '评论数量', COUNT(*) FROM litemall_comment;"
```

## 相关文件路径
- SQL 文件目录: `litemall-db/sql/`
- 执行脚本: `docker/execute_sql_scripts.ps1`
- 项目根目录: `d:\项目\Spring\litemall`