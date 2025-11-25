package org.linlinjava.litemall.core.llm.parser;

import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 参数绑定器
 * 用于将查询参数绑定到PreparedStatement
 */
@Component
public class ParameterBinder {
    
    /**
     * 绑定参数到PreparedStatement
     * @param stmt PreparedStatement
     * @param parameters 参数列表
     * @throws SQLException SQL异常
     */
    public void bindParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        if (stmt == null || parameters == null) {
            return;
        }
        
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            setParameter(stmt, i + 1, param);
        }
    }
    
    /**
     * 设置单个参数
     * @param stmt PreparedStatement
     * @param index 参数索引（从1开始）
     * @param value 参数值
     * @throws SQLException SQL异常
     */
    private void setParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.NULL);
        } else if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (Double) value);
        } else if (value instanceof Float) {
            stmt.setFloat(index, (Float) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(index, (Boolean) value);
        } else {
            stmt.setObject(index, value);
        }
    }
    
    /**
     * 从查询意图中提取参数列表
     * @param queryIntent 查询意图
     * @return 参数列表
     */
    public List<Object> extractParameters(QueryIntent queryIntent) {
        List<Object> parameters = new ArrayList<>();
        
        if (queryIntent == null || queryIntent.getConditions() == null) {
            return parameters;
        }
        
        Map<String, Object> conditions = queryIntent.getConditions();
        
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            // 处理名称模式匹配
            if ("name".equals(key) && value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nameCondition = (Map<String, Object>) value;
                extractNameParameters(nameCondition, parameters);
            } else {
                // 处理普通条件
                extractConditionParameters(key, value, parameters);
            }
        }
        
        return parameters;
    }
    
    /**
     * 提取名称模式匹配参数
     * @param nameCondition 名称条件
     * @param parameters 参数列表
     */
    private void extractNameParameters(Map<String, Object> nameCondition, List<Object> parameters) {
        String pattern = (String) nameCondition.get("pattern");
        String mode = (String) nameCondition.get("mode");
        Boolean caseSensitive = (Boolean) nameCondition.get("case_sensitive");
        
        if (pattern == null || pattern.trim().isEmpty()) {
            return;
        }
        
        // 默认模式为contains
        if (mode == null || mode.trim().isEmpty()) {
            mode = "contains";
        }
        
        switch (mode) {
            case "exact":
                parameters.add(caseSensitive != null && caseSensitive ? pattern : pattern.toLowerCase());
                break;
            case "contains":
                parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern + "%" : "%" + pattern.toLowerCase() + "%");
                break;
            case "starts_with":
                parameters.add(caseSensitive != null && caseSensitive ? pattern + "%" : pattern.toLowerCase() + "%");
                break;
            case "ends_with":
                parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern : "%" + pattern.toLowerCase());
                break;
            case "regex":
                parameters.add(pattern);
                break;
            default:
                parameters.add(caseSensitive != null && caseSensitive ? "%" + pattern + "%" : "%" + pattern.toLowerCase() + "%");
                break;
        }
    }
    
    /**
     * 提取普通条件参数
     * @param key 条件键
     * @param value 条件值
     * @param parameters 参数列表
     */
    private void extractConditionParameters(String key, Object value, List<Object> parameters) {
        switch (key) {
            case "min_price":
            case "max_price":
            case "min_number":
            case "max_number":
            case "is_on_sale":
                parameters.add(value);
                break;
            case "keyword":
                // 关键词搜索需要三个参数（name, keywords, brief）
                String keyword = "%" + value + "%";
                parameters.add(keyword);
                parameters.add(keyword);
                parameters.add(keyword);
                break;
            default:
                // 其他字段
                parameters.add(value);
                break;
        }
    }
    
    /**
     * 获取参数类型信息
     * @param value 参数值
     * @return 参数类型描述
     */
    public String getParameterType(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "STRING";
        } else if (value instanceof Integer) {
            return "INTEGER";
        } else if (value instanceof Long) {
            return "LONG";
        } else if (value instanceof Double) {
            return "DOUBLE";
        } else if (value instanceof Float) {
            return "FLOAT";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else {
            return "OBJECT(" + value.getClass().getSimpleName() + ")";
        }
    }
    
    /**
     * 验证参数列表
     * @param parameters 参数列表
     * @return true表示参数有效，false表示参数无效
     */
    public boolean validateParameters(List<Object> parameters) {
        if (parameters == null) {
            return true; // 空参数列表是有效的
        }
        
        for (Object param : parameters) {
            if (param != null && !isValidParameterType(param)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查参数类型是否有效
     * @param value 参数值
     * @return true表示类型有效，false表示类型无效
     */
    private boolean isValidParameterType(Object value) {
        return value == null ||
               value instanceof String ||
               value instanceof Integer ||
               value instanceof Long ||
               value instanceof Double ||
               value instanceof Float ||
               value instanceof Boolean;
    }
}