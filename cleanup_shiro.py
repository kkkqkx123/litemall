#!/usr/bin/env python3
"""
清理Shiro相关导入和注解的脚本
"""

import os
import re

# 需要清理的目录
ADMIN_WEB_DIR = "litemall-admin-api/src/main/java/org/linlinjava/litemall/admin/web"

# Shiro相关导入和注解模式
SHIRO_IMPORT_PATTERN = r'import org\.apache\.shiro\.authz\.annotation\.RequiresPermissions;\n'
SPRING_SECURITY_IMPORT = 'import org.springframework.security.access.prepost.PreAuthorize;\n'
ADMIN_ANNOTATION_IMPORT = 'import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;\n'

# 注解模式
REQUIRES_PERMISSIONS_PATTERN = r'\s*@RequiresPermissions\([^)]+\)\s*\n'

# 获取所有Java控制器文件
def get_controller_files():
    controllers = []
    for root, dirs, files in os.walk(ADMIN_WEB_DIR):
        for file in files:
            if file.endswith('.java') and file.startswith('Admin'):
                controllers.append(os.path.join(root, file))
    return controllers

# 清理单个文件
def cleanup_file(filepath):
    print(f"处理文件: {filepath}")
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # 1. 删除Shiro导入
    content = re.sub(SHIRO_IMPORT_PATTERN, '', content)
    
    # 2. 添加Spring Security导入（如果不存在）
    if '@PreAuthorize' in content and SPRING_SECURITY_IMPORT not in content:
        # 找到最后一个导入语句的位置
        import_matches = list(re.finditer(r'import [^;]+;', content))
        if import_matches:
            last_import = import_matches[-1]
            insert_pos = last_import.end()
            content = content[:insert_pos] + '\n' + SPRING_SECURITY_IMPORT + content[insert_pos:]
    
    # 3. 添加admin注解导入（如果不存在）
    if '@RequiresPermissionsDesc' in content and ADMIN_ANNOTATION_IMPORT not in content:
        # 找到最后一个导入语句的位置
        import_matches = list(re.finditer(r'import [^;]+;', content))
        if import_matches:
            last_import = import_matches[-1]
            insert_pos = last_import.end()
            content = content[:insert_pos] + '\n' + ADMIN_ANNOTATION_IMPORT + content[insert_pos:]
    
    # 4. 删除@RequiresPermissions注解
    content = re.sub(REQUIRES_PERMISSIONS_PATTERN, '', content)
    
    # 如果内容有变化，写回文件
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✓ 已清理: {filepath}")
        return True
    else:
        print(f"  - 无需清理: {filepath}")
        return False

def main():
    print("开始清理Shiro相关代码...")
    
    # 切换到项目根目录
    os.chdir("d:\\项目\\Spring\\litemall")
    
    controllers = get_controller_files()
    print(f"找到 {len(controllers)} 个控制器文件")
    
    cleaned_count = 0
    for controller in controllers:
        if cleanup_file(controller):
            cleaned_count += 1
    
    print(f"\n清理完成！共处理了 {cleaned_count} 个文件")

if __name__ == "__main__":
    main()