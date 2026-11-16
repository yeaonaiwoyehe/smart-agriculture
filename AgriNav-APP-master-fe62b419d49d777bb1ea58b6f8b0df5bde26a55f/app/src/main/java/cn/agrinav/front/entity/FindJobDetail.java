package cn.agrinav.front.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class FindJobDetail implements Serializable {
    /**
     * 任务ID
     */
    private String id;
    /**
     * 任务名
     */
    private String name;
    /**
     * 农机型号
     */
    private String machineType;
    /**
     * 可作业作物
     */
    private String[] workableCrops;
    /**
     * 作业区域
     */
    private String[] workArea;
    /**
     * 配备运粮车
     */
    private boolean hasGrainTransportVehicle;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 任务有效期
     */
    private Date taskValidityPeriod;
    /**
     * 任务创建时间
     */
    private Date taskCreateTime;
    /**
     * 任务状态
     */
    private int taskStatus;
    /**
     * 图片
     * @return
     */
    private String[] image;

    /**
     * 地址前缀
     * @return
     */
    private String addressPrefix;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String[] getWorkableCrops() {
        return workableCrops;
    }

    public void setWorkableCrops(String[] workableCrops) {
        this.workableCrops = workableCrops;
    }

    public String[] getWorkArea() {
        return workArea;
    }

    public void setWorkArea(String[] workArea) {
        this.workArea = workArea;
    }

    public boolean isHasGrainTransportVehicle() {
        return hasGrainTransportVehicle;
    }

    public void setHasGrainTransportVehicle(boolean hasGrainTransportVehicle) {
        this.hasGrainTransportVehicle = hasGrainTransportVehicle;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getTaskValidityPeriod() {
        return taskValidityPeriod;
    }

    public void setTaskValidityPeriod(Date taskValidityPeriod) {
        this.taskValidityPeriod = taskValidityPeriod;
    }

    public Date getTaskCreateTime() {
        return taskCreateTime;
    }

    public void setTaskCreateTime(Date taskCreateTime) {
        this.taskCreateTime = taskCreateTime;
    }

    public int getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(int taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String[] getImage() {
        return image;
    }

    public void setImage(String[] image) {
        this.image = image;
    }

    public String getAddressPrefix() {
        return addressPrefix;
    }

    public void setAddressPrefix(String addressPrefix) {
        this.addressPrefix = addressPrefix;
    }
}
