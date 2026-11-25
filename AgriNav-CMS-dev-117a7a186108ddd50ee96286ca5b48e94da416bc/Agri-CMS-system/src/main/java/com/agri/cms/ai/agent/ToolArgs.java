package com.agri.cms.ai.agent;

import java.util.Map;

/**
 * 工具参数转换工具（A-12）。
 *
 * 大模型回传的 arguments 经 Jackson 解析后，数字可能是 Integer/Long/Double
 * 或字符串，这里统一做健壮转换，无法解析时返回 null。
 *
 * @author agrinav
 */
public final class ToolArgs {

    private ToolArgs() {
    }

    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    public static Object get(Map<String, Object> args, String key) {
        return args == null ? null : args.get(key);
    }
}
