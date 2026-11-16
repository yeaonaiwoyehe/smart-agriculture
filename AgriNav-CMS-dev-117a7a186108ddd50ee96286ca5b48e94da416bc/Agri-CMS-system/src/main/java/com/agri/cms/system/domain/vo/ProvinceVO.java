package com.agri.cms.system.domain.vo;

/**
 * @author 李鸿麟
 * @descrpiton 这个实体类本身是用来封装省份的数据，但是在写大屏代码的时候因为前期研究不够，导致很多VO类都是返回相同的数据类型但是字段名不一样，所以这里就没有写其他的类
 * 下次一定用继承:(
 *
 */
public class ProvinceVO {

    private String name;

    private Integer value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ProvinceVO{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
