-- 问答功能数据库优化脚本
-- 创建问答会话历史表和统计表

-- 问答会话历史表
CREATE TABLE IF NOT EXISTS `litemall_qa_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `user_id` int(11) DEFAULT NULL COMMENT '用户ID（可选）',
  `question` text NOT NULL COMMENT '用户问题',
  `answer` text COMMENT '系统回答',
  `query_intent` json COMMENT '查询意图JSON',
  `sql_query` text COMMENT '执行的SQL查询',
  `query_result` json COMMENT '查询结果',
  `response_time` int(11) DEFAULT NULL COMMENT '响应时间（毫秒）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1-成功，0-失败',
  `error_msg` varchar(255) DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答会话历史表';

-- 问答会话统计表（可选，用于性能监控）
CREATE TABLE IF NOT EXISTS `litemall_qa_statistics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `query_type` varchar(32) DEFAULT NULL COMMENT '查询类型',
  `total_queries` int(11) DEFAULT '0' COMMENT '总查询次数',
  `avg_response_time` int(11) DEFAULT NULL COMMENT '平均响应时间',
  `success_count` int(11) DEFAULT '0' COMMENT '成功次数',
  `fail_count` int(11) DEFAULT '0' COMMENT '失败次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_query_type` (`query_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答会话统计表';