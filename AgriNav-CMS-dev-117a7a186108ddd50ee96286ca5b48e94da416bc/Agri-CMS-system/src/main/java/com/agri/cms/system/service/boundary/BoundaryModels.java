package com.agri.cms.system.service.boundary;

import java.math.BigDecimal;
import java.util.List;

public final class BoundaryModels {
    private BoundaryModels() { }
    public enum Position { INSIDE, BOUNDARY, OUTSIDE, EXCLUDED, UNAVAILABLE }
    public record CheckedPoint(String plotId,Position position,Boolean inBounds) { }
    public record ClippedPoint(double longitude,double latitude,String time) { }
    public record Fragment(int sourceSegmentIndex,double startFraction,double endFraction,ClippedPoint start,ClippedPoint end) { }
    public record ClippedTrack(String coordinateSystem,String lengthUnit,int inputPointCount,List<Position> pointPositions,
                               List<Fragment> fragments,BigDecimal totalLength,BigDecimal insideLength,
                               BigDecimal outsideLength,BigDecimal excludedLength) { }
}
