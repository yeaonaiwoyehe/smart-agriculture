package com.agri.cms.system.service.grouping;

/** 固定的两类归组，不能将请求内容作为 SQL 标识符。 */
public enum PlotGroupType {
    VILLAGE, REGION;

    public static PlotGroupType parse(String value) {
        try { return valueOf(value == null ? "" : value); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("分组类型必须为 VILLAGE（行政村）或 REGION（片区）"); }
    }

    public String validateCode(String code) {
        String value = code == null ? "" : code.trim();
        String pattern = this == VILLAGE ? "[0-9]{12}" : "[A-Za-z0-9][A-Za-z0-9_-]{0,63}";
        if (!value.matches(pattern)) throw new IllegalArgumentException(this == VILLAGE
                ? "行政村编号须为数据提供方给出的12位村级区划代码" : "片区编号须为1至64位字母、数字、下划线或短横线，首位为字母或数字");
        return value;
    }

    public static String validateName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty() || value.length() > 100 || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("分组名称须为1至100个字符，不能包含控制字符");
        return value;
    }
}
