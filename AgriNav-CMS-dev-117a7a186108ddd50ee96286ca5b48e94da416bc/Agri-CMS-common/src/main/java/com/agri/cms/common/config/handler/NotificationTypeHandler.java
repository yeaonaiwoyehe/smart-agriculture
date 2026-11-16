package com.agri.cms.common.config.handler;

import com.agri.cms.common.core.domain.model.NotificationType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-30 18:33
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-30 18:33
 */
@MappedTypes(NotificationType.class)
public class NotificationTypeHandler extends BaseTypeHandler<NotificationType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, NotificationType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getTypeName());
    }

    @Override
    public NotificationType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String typeName = rs.getString(columnName);
        return getEnumByTypeName(typeName);
    }

    @Override
    public NotificationType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String typeName = rs.getString(columnIndex);
        return getEnumByTypeName(typeName);
    }

    @Override
    public NotificationType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String typeName = cs.getString(columnIndex);
        return getEnumByTypeName(typeName);
    }

    private NotificationType getEnumByTypeName(String typeName) {
        if (typeName == null) return null;
        return Arrays.stream(NotificationType.values())
                .filter(type -> type.getTypeName().equals(typeName))
                .findFirst()
                .orElse(null);
    }
}

