package com.agri.cms.common.config.serializer;

import org.postgis.Point;
import org.springframework.core.convert.converter.Converter;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 10:07
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 10:07
 */
public class PointConverter implements Converter<Point,String> {
    @Override
    public String convert(Point source) {
        StringBuffer sb = new StringBuffer();
        source.outerWKT(sb);
        return sb.toString();
    }
}
