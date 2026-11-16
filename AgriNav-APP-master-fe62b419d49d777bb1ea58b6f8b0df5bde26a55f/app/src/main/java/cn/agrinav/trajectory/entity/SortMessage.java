package cn.agrinav.trajectory.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

import lombok.Data;

/**
 * @author 11230
 * 这个对象只要new出来里面的值都不允许为null
 */
@Data
public class SortMessage implements Serializable{
    /**选择的开始日期*/
    @Expose
    private String localDate;
    /**选择的开始时间*/
    @Expose
    private String beginTime;
    /**选择的结束时间*/
    @Expose
    private String endTime;
    /**作物*/
    @Expose(serialize = false)
    private String corn;
    /**作业类型*/
    @Expose
    private String workType;
    /**作业地址*/
    @Expose
    private String address;
    /**填入的手机号*/
    @Expose
    private String phone;
    @Expose(serialize = false)
    private String workTypeContent;
    @Expose(serialize = false)
    private String province;
    @Expose(serialize = false)
    private String city;
    @Expose(serialize = false)
    private String country;
    @Expose(serialize = false)
    private String townShip;
    @Expose(serialize = false)
    private String village;
    public SortMessage(){}
    public SortMessage(String phone){
        this.localDate = "";
        this.beginTime = "";
        this.endTime = "";
        this.corn = "";
        this.workType = "";
        this.phone = phone;
        this.address = "";
        this.workTypeContent = "";
        this.province = "";
        this.city = "";
        this.country = "";
        this.townShip = "";
        this.village = "";
    }
    public boolean isEmpty(){
        //当所有数据都空的时候才是判断空
        //要么所有数据都有，要么所有数据都没有，因为如果要填充但是又不完全填充我不让更新内容
        return localDate.isEmpty() && beginTime.isEmpty() && endTime.isEmpty() && workType.isEmpty() && corn.isEmpty() && address.isEmpty() && workTypeContent.isEmpty()&& province.isEmpty()&& city.isEmpty()&& country.isEmpty()&& townShip.isEmpty()&& village.isEmpty();
    }
    public void clear(){
        this.localDate = "";
        this.beginTime = "";
        this.endTime = "";
        this.corn = "";
        this.workType = "";
        this.workTypeContent= "";
        this.phone = phone;
        this.address = "";
        this.workTypeContent = "";
        this.province = "";
        this.city = "";
        this.country = "";
        this.townShip = "";
        this.village = "";
    }
}
