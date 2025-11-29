package org.linlinjava.litemall.admin.security;

import org.linlinjava.litemall.db.domain.LitemallAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class AdminUserDetails extends User {
    
    private final LitemallAdmin admin;

    public AdminUserDetails(LitemallAdmin admin, Collection<? extends GrantedAuthority> authorities) {
        super(admin.getUsername(), 
              admin.getPassword(), 
              true, // enabled
              true, // accountNonExpired
              true, // credentialsNonExpired
              true, // accountNonLocked
              authorities);
        this.admin = admin;
    }

    public LitemallAdmin getAdmin() {
        return admin;
    }

    public Integer getId() {
        return admin.getId();
    }

    public String getAvatar() {
        return admin.getAvatar();
    }

    public Integer[] getRoleIds() {
        return admin.getRoleIds();
    }
}