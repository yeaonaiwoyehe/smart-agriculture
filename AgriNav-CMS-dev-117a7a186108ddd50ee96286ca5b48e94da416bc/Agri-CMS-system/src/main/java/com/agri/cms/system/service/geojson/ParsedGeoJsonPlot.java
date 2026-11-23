package com.agri.cms.system.service.geojson;

import org.locationtech.jts.geom.Polygon;

/** 校验后的单地块；几何坐标为 WGS84，经度在前。 */
public record ParsedGeoJsonPlot(int index, String plotCode, String landName, String address,
                                String landType, Polygon geometry, String sourceFormat) {
    public ParsedGeoJsonPlot(int index, String plotCode, String landName, String address,
                             String landType, Polygon geometry) {
        this(index, plotCode, landName, address, landType, geometry, "GeoJSON");
    }
}
