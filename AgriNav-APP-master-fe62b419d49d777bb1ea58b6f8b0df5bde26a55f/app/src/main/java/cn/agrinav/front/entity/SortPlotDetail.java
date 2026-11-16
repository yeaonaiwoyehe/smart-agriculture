package cn.agrinav.front.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableField;

import cn.agrinav.BR;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 11230
 */
@EqualsAndHashCode(callSuper = true)
public class SortPlotDetail extends BaseObservable {
    private String minArea ="";
    private String maxArea ="";
    @Bindable
    public String getMinArea() {
        return minArea;
    }
    public void setMinArea(String minArea) {
        this.minArea = minArea;
        notifyPropertyChanged(BR.minArea);
    }

    @Bindable
    public String getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(String maxArea) {
        this.maxArea = maxArea;
        notifyPropertyChanged(BR.maxArea);
    }
}
