package org.yzh.web.model.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
@TableName(value = "boundary")
public class Boundary {
    /**
     * 边界ID
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 左下角经度
     */
    private Long leftDownLongitude;
    /**
     * 左下角纬度
     */
    private Long leftDownLatitude;
    /**
     * 右上角经度
     */
    private Long rightTopLongitude;
    /**
     * 右上角纬度
     */
    private Long rightTopLatitude;
    /**
     * 地块ID
     */
    @TableField(value = "land_plot_id")
    private Long plotId;
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getLeftDownLongitude() {
        return leftDownLongitude;
    }
    public void setLeftDownLongitude(Long leftDownLongitude) {
        this.leftDownLongitude = leftDownLongitude;
    }
    public Long getLeftDownLatitude() {
        return leftDownLatitude;
    }
    public void setLeftDownLatitude(Long leftDownLatitude) {
        this.leftDownLatitude = leftDownLatitude;
    }
    public Long getRightTopLongitude() {
        return rightTopLongitude;
    }
    public void setRightTopLongitude(Long rightTopLongitude) {
        this.rightTopLongitude = rightTopLongitude;
    }
    public Long getRightTopLatitude() {
        return rightTopLatitude;
    }
    public void setRightTopLatitude(Long rightTopLatitude) {
        this.rightTopLatitude = rightTopLatitude;
    }
    public Long getPlotId() {
        return plotId;
    }
    public void setPlotId(Long plotId) {
        this.plotId = plotId;
    }
    @Override
    public String toString() {
        return "Boundary{" +
                "id='" + id + '\'' +
                ", leftDownLongitude=" + leftDownLongitude +
                ", leftDownLatitude=" + leftDownLatitude +
                ", rightTopLongitude=" + rightTopLongitude +
                ", rightTopLatitude=" + rightTopLatitude +
                ", plotId=" + plotId +
                '}';
    }
    public Boundary(Long leftDownLongitude, Long leftDownLatitude, Long rightTopLongitude, Long rightTopLatitude, Long plotId) {
        this.leftDownLongitude = leftDownLongitude;
        this.leftDownLatitude = leftDownLatitude;
        this.rightTopLongitude = rightTopLongitude;
        this.rightTopLatitude = rightTopLatitude;
        this.plotId = plotId;
    }
}