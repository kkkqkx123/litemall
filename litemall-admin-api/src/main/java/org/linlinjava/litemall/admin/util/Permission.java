package org.linlinjava.litemall.admin.util;

/**
 * 权限信息类，用于存储权限相关数据
 * 这个类在权限控制迁移后主要用于兼容旧版权限列表功能
 */
public class Permission {
    private String permission;
    private String description;
    private String api;
    private String menu;
    private String button;

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public String getApi() {
        return api;
    }

    public String getMenu() {
        return menu;
    }

    public String getButton() {
        return button;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public void setMenu(String menu) {
        this.menu = menu;
    }

    public void setButton(String button) {
        this.button = button;
    }
}
