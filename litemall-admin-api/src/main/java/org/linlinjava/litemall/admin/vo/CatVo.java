package org.linlinjava.litemall.admin.vo;

import java.util.List;

public class CatVo {
    private Integer value = null;
    private String label = null;
    private List<CatVo> children = null;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<CatVo> getChildren() {
        return children;
    }

    public void setChildren(List<CatVo> children) {
        this.children = children;
    }
}
