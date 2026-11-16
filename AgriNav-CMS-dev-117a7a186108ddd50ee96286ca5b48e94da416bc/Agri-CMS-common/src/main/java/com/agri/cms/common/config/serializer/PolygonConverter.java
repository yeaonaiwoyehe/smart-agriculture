package com.agri.cms.common.config.serializer;
import org.postgis.Polygon;
import org.springframework.core.convert.converter.Converter;
/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 09:59
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 09:59
 */
public class PolygonConverter implements Converter<Polygon,String>{
    @Override
    public String convert(Polygon source) {
        StringBuffer sb = new StringBuffer();
        source.outerWKT(sb);
        return sb.toString();
    }
}
