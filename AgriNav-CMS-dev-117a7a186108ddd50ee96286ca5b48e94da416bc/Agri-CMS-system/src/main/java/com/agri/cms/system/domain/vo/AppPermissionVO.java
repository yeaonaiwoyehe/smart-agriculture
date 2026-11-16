package com.agri.cms.system.domain.vo;

/**
 * @author 李鸿麟
 * 给安卓端返回的组件VO类
 */
public class AppPermissionVO {
    private String componentStr;
    private Boolean visible;

    public String getComponentStr() {
        return componentStr;
    }

    public void setComponentStr(String componentStr) {
        this.componentStr = componentStr;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
