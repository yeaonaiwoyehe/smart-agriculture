package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

/**
 * @author 李鸿麟
 * 安卓app权限类
 */
public class AppPermission {
    /**

     组件ID
     */
    @TableId(type = IdType.AUTO)
    private Integer componentId;
    /**

     app组件名称
     */
    private String permissionName;
    /**

     权限父ID
     */
    private Integer parentId;
    /**

     安卓app组件ID
     */
    private String componentStr;
    /**

     该安卓组件是否能看见 0可以看见 1无法看见
     */
    private Integer isVisible;

    /**

     该安卓组件是否被删除 0未被删除 1已被删除
     */
    private Integer isDeleted;

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Integer getComponentId() {
        return componentId;
    }

    public void setComponentId(Integer componentId) {
        this.componentId = componentId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getComponentStr() {
        return componentStr;
    }

    public void setComponentStr(String componentStr) {
        this.componentStr = componentStr;
    }

    public Integer getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Integer isVisible) {
        this.isVisible = isVisible;
    }
}
