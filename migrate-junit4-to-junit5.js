#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// 需要迁移的测试文件列表
const testFiles = [
    'litemall-wx-api/src/test/java/org/linlinjava/litemall/wx/BigDecimalTest.java',
    'litemall-wx-api/src/test/java/org/linlinjava/litemall/wx/WxConfigTest.java',
    'litemall-admin-api/src/test/java/org/linlinjava/litemall/admin/AdminConfigTest.java',
    'litemall-admin-api/src/test/java/org/linlinjava/litemall/admin/BcryptTest.java',
    'litemall-admin-api/src/test/java/org/linlinjava/litemall/admin/CreateShareImageTest.java',
    'litemall-admin-api/src/test/java/org/linlinjava/litemall/admin/PermissionTest.java',
    'litemall-all/src/test/java/org/linlinjava/litemall/allinone/AllinoneConfigTest.java',
    'litemall-core/src/test/java/org/linlinjava/litemall/core/IntegerTest.java',
    'litemall-db/src/test/java/org/linlinjava/litemall/db/DbUtilTest.java'
];

function migrateTestFile(filePath) {
    try {
        const fullPath = path.resolve(filePath);
        
        if (!fs.existsSync(fullPath)) {
            console.log(`文件不存在: ${filePath}`);
            return;
        }

        let content = fs.readFileSync(fullPath, 'utf8');
        
        // 备份原始内容
        const originalContent = content;
        
        // 1. 替换导入语句
        content = content.replace(/import org\.junit\.Test;/g, 'import org.junit.jupiter.api.Test;');
        content = content.replace(/import org\.junit\.runner\.RunWith;/g, '');
        content = content.replace(/import org\.springframework\.test\.context\.junit4\.SpringJUnit4ClassRunner;/g, '');
        content = content.replace(/import org\.springframework\.test\.context\.junit4\.SpringRunner;/g, '');
        content = content.replace(/import org\.springframework\.test\.context\.web\.WebAppConfiguration;/g, '');
        
        // 2. 移除注解
        content = content.replace(/@WebAppConfiguration\s*/g, '');
        content = content.replace(/@RunWith\(SpringJUnit4ClassRunner\.class\)\s*/g, '');
        content = content.replace(/@RunWith\(SpringRunner\.class\)\s*/g, '');
        
        // 3. 修改类声明
        content = content.replace(/public class/g, 'class');
        
        // 4. 处理Assert语句
        content = content.replace(/Assert\.assertEquals/g, 'assertEquals');
        content = content.replace(/Assert\.assertTrue/g, 'assertTrue');
        content = content.replace(/Assert\.assertFalse/g, 'assertFalse');
        content = content.replace(/Assert\.assertNotNull/g, 'assertNotNull');
        content = content.replace(/Assert\.assertNull/g, 'assertNull');
        
        // 5. 添加静态导入（如果文件中有Assert的使用）
        if (content.includes('assertEquals') || content.includes('assertTrue') || 
            content.includes('assertFalse') || content.includes('assertNotNull') || 
            content.includes('assertNull')) {
            
            // 检查是否已经有静态导入
            if (!content.includes('import static org.junit.jupiter.api.Assertions.')) {
                // 在第一个import之后插入静态导入
                content = content.replace(
                    /(import [^;]+;\n)/,
                    '$1import static org.junit.jupiter.api.Assertions.*;\n'
                );
            }
        }
        
        // 6. 清理多余的空行
        content = content.replace(/\n\s*\n\s*\n/g, '\n\n');
        
        // 检查是否有变化
        if (content !== originalContent) {
            fs.writeFileSync(fullPath, content, 'utf8');
            console.log(`✅ 已迁移: ${filePath}`);
        } else {
            console.log(`⚪ 无需更改: ${filePath}`);
        }
        
    } catch (error) {
        console.error(`❌ 处理文件失败 ${filePath}:`, error.message);
    }
}

console.log('开始迁移JUnit 4测试文件到JUnit 5...\n');

testFiles.forEach(file => {
    migrateTestFile(file);
});

console.log('\n迁移完成！');