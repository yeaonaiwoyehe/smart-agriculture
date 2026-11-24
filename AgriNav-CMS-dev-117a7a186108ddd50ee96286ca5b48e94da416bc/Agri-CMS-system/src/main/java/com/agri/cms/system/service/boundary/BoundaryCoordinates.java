package com.agri.cms.system.service.boundary;

public final class BoundaryCoordinates {
    private BoundaryCoordinates() { }
    public static void validate(Double longitude,Double latitude) {
        if(longitude==null||latitude==null||!Double.isFinite(longitude)||!Double.isFinite(latitude)
                ||Math.abs(longitude)>180||Math.abs(latitude)>90)
            throw new IllegalArgumentException("请提供有效的 WGS84 经度和纬度");
    }
}
