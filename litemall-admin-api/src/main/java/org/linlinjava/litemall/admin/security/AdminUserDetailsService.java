package org.linlinjava.litemall.admin.security;

import org.linlinjava.litemall.db.domain.LitemallAdmin;
import org.linlinjava.litemall.db.service.LitemallAdminService;
import org.linlinjava.litemall.db.service.LitemallPermissionService;
import org.linlinjava.litemall.db.service.LitemallRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    @Autowired
    private LitemallAdminService adminService;
    
    @Autowired
    private LitemallRoleService roleService;
    
    @Autowired
    private LitemallPermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<LitemallAdmin> adminList = adminService.findAdmin(username);
        if (adminList.isEmpty()) {
            throw new UsernameNotFoundException("管理员不存在: " + username);
        }

        LitemallAdmin admin = adminList.get(0);
        
        // 构建权限列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 获取角色和权限信息
        Integer[] roleIds = admin.getRoleIds();
        if (roleIds != null && roleIds.length > 0) {
            // 添加角色
            Set<String> roles = roleService.queryByIds(roleIds);
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            
            // 添加权限
            Set<String> permissions = permissionService.queryByRoleIds(roleIds);
            for (String permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        
        // 如果没有角色，添加默认的管理员角色
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        
        return new AdminUserDetails(admin, authorities);
    }
}