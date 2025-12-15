package org.linlinjava.litemall.admin.util;

import org.linlinjava.litemall.admin.vo.PermVo;
import org.springframework.stereotype.Controller;

import java.util.*;

/**
 * 权限工具类
 * 在权限控制迁移后，这个类的原有功能已被Spring Security替代
 * 保留部分方法以兼容现有的权限列表功能
 */
public class PermissionUtil {

    /**
     * 根据权限字符串列表构建权限树结构
     * @param permissionList 权限字符串集合
     * @return 权限树结构
     */
    public static List<PermVo> listPermVo(Collection<String> permissionList) {
        List<PermVo> root = new ArrayList<>();
        
        for (String permission : permissionList) {
            // 解析权限字符串格式，例如：admin:category:list
            String[] parts = permission.split(":");
            if (parts.length < 3) {
                continue; // 跳过格式不正确的权限
            }
            
            String menu1 = parts[0]; // 一级菜单
            String menu2 = parts[1]; // 二级菜单  
            String button = parts[2]; // 按钮权限
            
            // 查找或创建一级菜单
            PermVo perm1 = findOrCreatePermVo(root, menu1, menu1);
            
            // 查找或创建二级菜单
            PermVo perm2 = findOrCreatePermVo(perm1.getChildren(), menu2, menu2);
            
            // 添加按钮权限
            PermVo buttonPerm = findOrCreatePermVo(perm2.getChildren(), permission, button);
            buttonPerm.setId(permission);
            buttonPerm.setApi(""); // API信息需要从其他地方获取
        }
        
        return root;
    }

    /**
     * 查找或创建权限节点
     */
    private static PermVo findOrCreatePermVo(List<PermVo> children, String id, String label) {
        for (PermVo permVo : children) {
            if (permVo.getLabel().equals(label)) {
                return permVo;
            }
        }
        
        PermVo newPerm = new PermVo();
        newPerm.setId(id);
        newPerm.setLabel(label);
        newPerm.setChildren(new ArrayList<>());
        children.add(newPerm);
        
        return newPerm;
    }

    /**
     * 转换权限列表为字符串集合
     * @param permissions 权限对象列表
     * @return 权限字符串集合
     */
    public static Set<String> listPermissionString(List<Permission> permissions) {
        Set<String> permissionsString = new HashSet<>();
        for (Permission permission : permissions) {
            permissionsString.add(permission.getPermission());
        }
        return permissionsString;
    }

    /**
     * 获取所有权限字符串（基于Spring Security的@PreAuthorize注解）
     * 这个方法需要在权限控制迁移完成后，通过反射扫描Controller来获取
     */
    public static Set<String> extractPermissionsFromControllers() {
        // TODO: 在权限控制迁移完成后实现
        // 通过反射扫描Controller类，提取@PreAuthorize注解中的权限信息
        return new HashSet<>();
    }

    /**
     * 获取权限列表（兼容性方法）
     * @param context 应用上下文
     * @param packageName 包名
     * @return 权限对象列表
     */
    public static List<Permission> listPermission(Object context, String packageName) {
        // TODO: 实现基于反射的权限提取
        // 目前返回空列表，避免编译错误
        return new ArrayList<>();
    }
}
