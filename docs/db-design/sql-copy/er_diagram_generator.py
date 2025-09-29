#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
增强版E-R图生成器 - 支持解析所有表和外键关系
"""

import re
import os

def extract_all_tables(sql_content):
    """提取SQL文件中所有的表名"""
    table_pattern = r'CREATE TABLE `([^`]+)`'
    tables = re.findall(table_pattern, sql_content)
    return tables

def parse_table_enhanced(sql_content: str, target_table: str):
    """增强版表结构解析 - 支持外键识别"""
    # 查找目标表的CREATE TABLE语句
    table_pattern = rf'CREATE TABLE `{re.escape(target_table)}`\s*\((.*?)ENGINE'
    table_match = re.search(table_pattern, sql_content, re.DOTALL)
    
    if not table_match:
        print(f"未找到表 {target_table}")
        return None
    
    table_definition = table_match.group(0)
    
    # 查找表注释
    comment_match = re.search(r"COMMENT\s*'([^']+)'", table_definition)
    comment = comment_match.group(1) if comment_match else ""
    
    table_info = {
        'name': target_table,
        'comment': comment,
        'columns': [],
        'primary_keys': [],
        'foreign_keys': [],
        'indexes': []
    }
    
    # 提取列定义部分（括号内的内容）
    # 找到第一个左括号和最后一个右括号
    brace_start = table_definition.find('(')
    brace_end = table_definition.rfind(')')
    
    if brace_start == -1 or brace_end == -1:
        print(f"未找到表 {target_table} 的列定义范围")
        return None
    
    column_section = table_definition[brace_start + 1:brace_end]
    
    # 按行分割列定义
    lines = [line.strip() for line in column_section.split('\n') if line.strip()]
    
    for line in lines:
        # 跳过空行和注释行
        if not line or line.startswith('--'):
            continue
            
        # 解析列定义
        if line.startswith('`'):
            # 解析列定义 - 更精确的正则表达式
            column_match = re.match(r'`(\w+)`\s+([^,]+?)(?:,\s*)?(?:--.*)?$', line.strip())
            if column_match:
                column_name = column_match.group(1)
                column_def = column_match.group(2).strip()
                
                # 提取数据类型
                type_match = re.match(r'(\w+(?:\([^)]+\))?)\s*(.*)', column_def)
                if type_match:
                    column_type = type_match.group(1)
                    constraints = type_match.group(2)
                else:
                    column_type = column_def
                    constraints = ""
                
                # 提取注释
                comment_match = re.search(r"COMMENT\s*'([^']+)'", line)
                column_comment = comment_match.group(1) if comment_match else ""
                
                # 检查是否为主键
                is_primary = 'PRIMARY KEY' in line or 'AUTO_INCREMENT' in column_def
                is_not_null = 'NOT NULL' in column_def
                has_default = 'DEFAULT' in column_def
                
                # 检查外键约束
                is_foreign_key = False
                referenced_table = None
                referenced_column = None
                
                # 查找外键引用（通过KEY和FOREIGN KEY）
                if 'KEY' in line and not 'PRIMARY KEY' in line:
                    # 这可能是外键索引
                    key_match = re.search(r'KEY\s+`?(\w+)`?\s*\(`(\w+)`\)', line)
                    if key_match and key_match.group(2) == column_name:
                        # 记录索引信息，后续可能关联到外键
                        table_info['indexes'].append({
                            'name': key_match.group(1),
                            'column': column_name
                        })
                
                column_info = {
                    'name': column_name,
                    'type': column_type,
                    'comment': column_comment,
                    'is_primary': is_primary,
                    'is_not_null': is_not_null,
                    'has_default': has_default,
                    'is_foreign_key': is_foreign_key,
                    'referenced_table': referenced_table,
                    'referenced_column': referenced_column
                }
                
                table_info['columns'].append(column_info)
                
                if is_primary:
                    table_info['primary_keys'].append(column_name)
        
        # 解析表级约束（主键、外键、唯一键等）
        elif 'PRIMARY KEY' in line and '(`' in line:
            # 解析复合主键
            pk_columns = re.findall(r'`([^`]+)`', line)
            for col in pk_columns:
                if col not in table_info['primary_keys']:
                    table_info['primary_keys'].append(col)
        
        elif 'KEY' in line and not 'PRIMARY KEY' in line:
            # 解析索引
            key_match = re.search(r'(?:KEY|INDEX)\s+`?(\w+)`?\s*\(([^)]+)\)', line)
            if key_match:
                key_name = key_match.group(1)
                key_columns = re.findall(r'`([^`]+)`', key_match.group(2))
                table_info['indexes'].append({
                    'name': key_name,
                    'columns': key_columns
                })
    
    # 基于列名和索引推断外键关系
    infer_foreign_keys(table_info, sql_content)
    
    return table_info

def infer_foreign_keys(table_info, sql_content):
    """基于列名和索引推断外键关系"""
    foreign_key_patterns = [
        (r'(\w+)_id$', 'user', 'id'),
        (r'(\w+)_id$', 'goods', 'id'),
        (r'(\w+)_id$', 'order', 'id'),
        (r'(\w+)_id$', 'category', 'id'),
        (r'(\w+)_id$', 'brand', 'id'),
        (r'(\w+)_id$', 'coupon', 'id'),
        (r'parent_(\w+)_id$', 'category', 'id'),
        (r'(\w+)_sn$', 'goods', 'goods_sn'),
    ]
    
    for column in table_info['columns']:
        if column['name'].endswith('_id') or column['name'].endswith('_sn'):
            for pattern, ref_table, ref_column in foreign_key_patterns:
                if re.match(pattern, column['name']):
                    # 检查引用的表是否存在
                    if f'`{ref_table}`' in sql_content:
                        column['is_foreign_key'] = True
                        column['referenced_table'] = ref_table
                        column['referenced_column'] = ref_column
                        
                        table_info['foreign_keys'].append({
                            'column': column['name'],
                            'referenced_table': ref_table,
                            'referenced_column': ref_column
                        })
                        break

def generate_mermaid_diagram_enhanced(table_info):
    """生成增强版Mermaid格式的E-R图"""
    if not table_info or not table_info['columns']:
        return ""
    
    mermaid_code = f"""erDiagram
    {table_info['name']} {{"""
    
    for col in table_info['columns']:
        # 简化数据类型显示
        simple_type = col['type'].split('(')[0] if '(' in col['type'] else col['type']
        
        # 添加主键和外键标记
        indicators = []
        if col['is_primary']:
            indicators.append("PK")
        if col['is_foreign_key']:
            indicators.append("FK")
        if col['is_not_null']:
            indicators.append("NOT NULL")
        
        indicator_str = f" [{', '.join(indicators)}]" if indicators else ""
        
        mermaid_code += f"\n        {col['type']} {col['name']}{indicator_str}"
    
    mermaid_code += "\n    }"
    
    return mermaid_code

def generate_complete_mermaid_diagram(all_tables_info):
    """生成完整的Mermaid ER图，包含所有表和关系"""
    if not all_tables_info:
        return ""
    
    mermaid_code = "erDiagram\n"
    
    # 生成所有表的定义
    for table_info in all_tables_info:
        if not table_info or not table_info['columns']:
            continue
            
        mermaid_code += f"\n    {table_info['name']} {{"
        
        for col in table_info['columns']:
            # 简化数据类型显示
            simple_type = col['type'].split('(')[0] if '(' in col['type'] else col['type']
            
            # 添加主键和外键标记
            indicators = []
            if col['is_primary']:
                indicators.append("PK")
            if col['is_foreign_key']:
                indicators.append("FK")
            
            indicator_str = f" [{', '.join(indicators)}]" if indicators else ""
            
            mermaid_code += f"\n        {simple_type} {col['name']}{indicator_str}"
        
        mermaid_code += "\n    }\n"
    
    # 生成表之间的关系
    relationships_added = set()
    
    for table_info in all_tables_info:
        if not table_info:
            continue
            
        for fk in table_info['foreign_keys']:
            relationship_key = f"{table_info['name']}_{fk['referenced_table']}"
            reverse_relationship_key = f"{fk['referenced_table']}_{table_info['name']}"
            
            # 避免重复添加关系
            if relationship_key not in relationships_added and reverse_relationship_key not in relationships_added:
                mermaid_code += f"    {table_info['name']} ||--o{{ {fk['referenced_table']} : \"{fk['column']} -> {fk['referenced_column']}\"\n"
                relationships_added.add(relationship_key)
    
    return mermaid_code

def generate_dbml_diagram_enhanced(table_info):
    """生成增强版DBML格式的E-R图"""
    if not table_info or not table_info['columns']:
        return ""
    
    dbml_code = f"""Table {table_info['name']} {{"""
    
    for col in table_info['columns']:
        # 转换数据类型
        dbml_type = convert_to_dbml_type_simple(col['type'])
        
        # 添加约束
        constraints = []
        if col['is_primary']:
            constraints.append("pk")
        if col['is_foreign_key']:
            constraints.append("ref")
        if col['is_not_null']:
            constraints.append("not null")
        if col['has_default']:
            constraints.append("default")
        
        constraint_str = f" [{', '.join(constraints)}]" if constraints else ""
        
        # 添加注释
        comment_str = f" [note: '{col['comment']}']" if col['comment'] else ""
        
        dbml_code += f"\n  {col['name']} {dbml_type}{constraint_str}{comment_str}"
    
    # 添加表注释
    if table_info['comment']:
        dbml_code += f"\n  Note: '{table_info['comment']}'"
    
    dbml_code += "\n}"
    
    return dbml_code

def generate_complete_dbml_diagram(all_tables_info):
    """生成完整的DBML ER图，包含所有表和关系"""
    if not all_tables_info:
        return ""
    
    dbml_code = ""
    
    # 生成所有表的定义
    for table_info in all_tables_info:
        if not table_info or not table_info['columns']:
            continue
            
        dbml_code += f"\nTable {table_info['name']} {{"
        
        for col in table_info['columns']:
            # 转换数据类型
            dbml_type = convert_to_dbml_type_simple(col['type'])
            
            # 添加约束
            constraints = []
            if col['is_primary']:
                constraints.append("pk")
            if col['is_foreign_key']:
                constraints.append("ref")
            if col['is_not_null']:
                constraints.append("not null")
            
            constraint_str = f" [{', '.join(constraints)}]" if constraints else ""
            
            # 添加注释
            comment_str = f" [note: '{col['comment']}']" if col['comment'] else ""
            
            dbml_code += f"\n  {col['name']} {dbml_type}{constraint_str}{comment_str}"
        
        # 添加表注释
        if table_info['comment']:
            dbml_code += f"\n  Note: '{table_info['comment']}'"
        
        dbml_code += "\n}\n"
    
    # 生成表之间的关系
    for table_info in all_tables_info:
        if not table_info:
            continue
            
        for fk in table_info['foreign_keys']:
            dbml_code += f"Ref: {table_info['name']}.{fk['column']} > {fk['referenced_table']}.{fk['referenced_column']}\n"
    
    return dbml_code

def convert_to_dbml_type_simple(mysql_type: str) -> str:
    """将MySQL数据类型转换为DBML类型"""
    type_mapping = {
        'int': 'integer',
        'tinyint': 'boolean',
        'smallint': 'smallint',
        'varchar': 'varchar',
        'char': 'char',
        'text': 'text',
        'decimal': 'decimal',
        'datetime': 'datetime',
        'date': 'date',
        'time': 'time'
    }
    
    base_type = mysql_type.split('(')[0].lower()
    return type_mapping.get(base_type, 'varchar')

def generate_detailed_text_diagram_enhanced(table_info):
    """生成增强版详细的文本格式E-R图"""
    if not table_info or not table_info['columns']:
        return ""
    
    text_diagram = f"""========================================
表结构详情: {table_info['name']}
表注释: {table_info['comment']}
总列数: {len(table_info['columns'])}
主键: {', '.join(table_info['primary_keys']) if table_info['primary_keys'] else '无'}
外键数: {len(table_info['foreign_keys'])}
索引数: {len(table_info['indexes'])}
========================================

列信息:
"""
    
    for i, col in enumerate(table_info['columns'], 1):
        indicators = []
        if col['is_primary']:
            indicators.append("PK")
        if col['is_foreign_key']:
            indicators.append("FK")
        if col['is_not_null']:
            indicators.append("NOT NULL")
        if col['has_default']:
            indicators.append("DEFAULT")
        
        indicator_str = f" [{', '.join(indicators)}]" if indicators else ""
        
        # 外键信息
        fk_info = ""
        if col['is_foreign_key'] and col['referenced_table']:
            fk_info = f" -> {col['referenced_table']}.{col['referenced_column']}"
        
        comment = f" - {col['comment']}" if col['comment'] else ""
        
        # 限制类型显示长度
        type_display = col['type'][:25] + "..." if len(col['type']) > 25 else col['type']
        
        text_diagram += f"{i:2d}. {col['name']:20} {type_display:30}{indicator_str}{fk_info}{comment}\n"
    
    # 外键详情
    if table_info['foreign_keys']:
        text_diagram += "\n外键关系:\n"
        for fk in table_info['foreign_keys']:
            text_diagram += f"  {fk['column']} -> {fk['referenced_table']}.{fk['referenced_column']}\n"
    
    # 索引详情
    if table_info['indexes']:
        text_diagram += "\n索引信息:\n"
        for idx in table_info['indexes']:
            if 'columns' in idx:
                columns_str = ', '.join(idx['columns'])
            else:
                columns_str = idx.get('column', 'unknown')
            text_diagram += f"  {idx['name']}: ({columns_str})\n"
    
    return text_diagram

def generate_database_summary(all_tables_info):
    """生成数据库摘要信息"""
    if not all_tables_info:
        return ""
    
    total_tables = len(all_tables_info)
    total_columns = sum(len(table['columns']) for table in all_tables_info if table)
    total_foreign_keys = sum(len(table['foreign_keys']) for table in all_tables_info if table)
    total_indexes = sum(len(table['indexes']) for table in all_tables_info if table)
    
    summary = f"""
========================================
数据库摘要报告
========================================
总表数: {total_tables}
总列数: {total_columns}
总外键数: {total_foreign_keys}
总索引数: {total_indexes}

表列表:
"""
    
    for table_info in all_tables_info:
        if not table_info:
            continue
        summary += f"  - {table_info['name']}: {len(table_info['columns'])}列, {len(table_info['foreign_keys'])}外键, {table_info['comment']}\n"
    
    return summary

def main():
    """增强版主函数 - 处理整个SQL文件"""
    sql_file_path = "litemall_table.sql"
    
    print("🚀 开始解析SQL文件...")
    
    # 读取SQL文件内容
    try:
        with open(sql_file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        print(f"✅ 成功读取SQL文件: {len(sql_content)} 字符")
    except FileNotFoundError:
        print(f"❌ 找不到文件: {sql_file_path}")
        return
    except Exception as e:
        print(f"❌ 读取文件失败: {e}")
        return
    
    # 提取所有表名
    all_table_names = extract_all_tables(sql_content)
    print(f"📊 发现 {len(all_table_names)} 个表")
    
    # 解析所有表结构
    all_tables_info = []
    failed_tables = []
    
    for table_name in all_table_names:
        print(f"🔍 正在解析表: {table_name}")
        table_info = parse_table_enhanced(sql_content, table_name)
        
        if table_info and table_info['columns']:
            all_tables_info.append(table_info)
            print(f"  ✅ 成功解析: {len(table_info['columns'])} 列, {len(table_info['foreign_keys'])} 外键")
        else:
            failed_tables.append(table_name)
            print(f"  ❌ 解析失败")
    
    print(f"\n📈 解析结果:")
    print(f"  ✅ 成功: {len(all_tables_info)} 个表")
    print(f"  ❌ 失败: {len(failed_tables)} 个表")
    
    if failed_tables:
        print(f"  失败的表: {', '.join(failed_tables)}")
    
    if not all_tables_info:
        print("❌ 没有成功解析任何表")
        return
    
    # 生成数据库摘要
    summary = generate_database_summary(all_tables_info)
    print("\n" + summary)
    
    # 生成完整的Mermaid ER图
    print("📝 正在生成完整的Mermaid ER图...")
    complete_mermaid = generate_complete_mermaid_diagram(all_tables_info)
    
    if complete_mermaid:
        with open("complete_er_diagram.mmd", "w", encoding="utf-8") as f:
            f.write(complete_mermaid)
        print("✅ 已生成完整的Mermaid ER图: complete_er_diagram.mmd")
        
        # 显示部分内容（前100行）
        print("\n" + "="*80)
        print("生成的完整Mermaid ER图 (前50行):")
        lines = complete_mermaid.split('\n')
        print('\n'.join(lines[:50]))
        print("...")
    
    # 生成完整的DBML ER图
    print("📝 正在生成完整的DBML ER图...")
    complete_dbml = generate_complete_dbml_diagram(all_tables_info)
    
    if complete_dbml:
        with open("complete_er_diagram.dbml", "w", encoding="utf-8") as f:
            f.write(complete_dbml)
        print("✅ 已生成完整的DBML ER图: complete_er_diagram.dbml")
    
    # 生成详细的文本报告
    print("📝 正在生成详细的文本报告...")
    with open("complete_er_diagram.txt", "w", encoding="utf-8") as f:
        f.write(summary + "\n")
        
        for table_info in all_tables_info:
            if table_info:
                text_diagram = generate_detailed_text_diagram_enhanced(table_info)
                f.write(text_diagram + "\n")
    
    print("✅ 已生成详细的文本报告: complete_er_diagram.txt")
    
    # 生成单个表的示例（第一个表）
    if all_tables_info:
        sample_table = all_tables_info[0]
        print(f"\n📋 示例 - 表 {sample_table['name']} 的详细信息:")
        sample_text = generate_detailed_text_diagram_enhanced(sample_table)
        print(sample_text)
    
    print("\n🎉 所有ER图生成完成!")
    print("\n生成的文件:")
    print("  - complete_er_diagram.mmd (Mermaid格式)")
    print("  - complete_er_diagram.dbml (DBML格式)")
    print("  - complete_er_diagram.txt (详细文本报告)")

if __name__ == "__main__":
    main()