# MySQL 8.0 自定义镜像
FROM mysql:8.0

# 设置环境变量
ENV MYSQL_ROOT_PASSWORD=root
ENV MYSQL_DATABASE=litemall
ENV MYSQL_USER=root
ENV MYSQL_PASSWORD=root

# 设置字符集
ENV LANG=C.UTF-8

# 复制配置文件
COPY ./conf.d/my.cnf /etc/mysql/conf.d/my.cnf

# 复制初始化SQL文件
COPY ./init-sql/ /docker-entrypoint-initdb.d/

# 设置权限
RUN chmod -R 644 /docker-entrypoint-initdb.d/*

# 暴露端口
EXPOSE 3306

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD mysqladmin ping -h localhost -u root -p$MYSQL_ROOT_PASSWORD || exit 1