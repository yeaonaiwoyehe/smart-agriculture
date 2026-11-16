package com.agri.cms.system.domain.dto;

/**
 * 地址区域
 *
 * @author mfr
 * @since 2024/10/26
 */
public class AddressAreaDTO {
    private Long code;
    // 地址编码
    private String name;
    // 地址名称
    private Short level;
    // 行政级别
    private Long pcode;
    // 上级编码
    private Integer category;
    // 分类
    private Double area;
    // 面积


    public AddressAreaDTO() {
    }

    public AddressAreaDTO(Long code, String name, Short level, Long pcode, Integer category, Double area) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.pcode = pcode;
        this.category = category;
        this.area = area;
    }

    public Long getCode() {
        return code;
    }

    public void setCode(Long code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Short getLevel() {
        return level;
    }

    public void setLevel(Short level) {
        this.level = level;
    }

    public Long getPcode() {
        return pcode;
    }

    public void setPcode(Long pcode) {
        this.pcode = pcode;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }
}