package cn.agrinav.measure.entity;


import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * @author 18506
 */
public class LandAddress {
    //地址id
    private Long code;
    //地址名称
    private String name;
    //地址级别
    private Short level;
    //上级地址id
    private Long pcode;

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
    @NonNull
    @Override
    public String toString() {
        return "LandAddress{" +
                "code=" + code +
                ", name='" + name + '\'' +
                ", level=" + level +
                ", pcode=" + pcode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LandAddress that = (LandAddress) o;
        return Objects.equals(code, that.code) && Objects.equals(name, that.name) && Objects.equals(level, that.level) && Objects.equals(pcode, that.pcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, level, pcode);
    }
}