package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.vo.Point;
import org.junit.Test;
import org.postgis.Polygon;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.Assert.*;

public class WidthAccountingTest {
    private final AreaCalculationServiceImpl area = new AreaCalculationServiceImpl();
    public static Point point(double x, double y) { return new Point(lon(x), lon(y)); }
    private static double lon(double metres) { return Math.toDegrees(metres / 6371000.0); }
    public static Polygon box(double west, double south, double east, double north) throws Exception {
        return new Polygon("POLYGON((" + lon(west)+" "+lon(south)+","+lon(east)+" "+lon(south)+","
                +lon(east)+" "+lon(north)+","+lon(west)+" "+lon(north)+","+lon(west)+" "+lon(south)+"))");
    }
    private List<Point> path(double... east) { return Arrays.stream(east).mapToObj(x -> point(x, 0)).toList(); }
    private void number(double expected, BigDecimal actual) { assertEquals(expected, actual.doubleValue(), .01); }

    @Test public void hundredMetresTimesThreeMetresIs300SquareMetresAnd045Mu() throws Exception {
        var r = area.calculateSegmentedCoverage(List.of(path(0, 100)), 300, box(-10,-10,110,10), List.of());
        number(100,r.travelLengthMetres()); number(300,r.grossAreaSquareMetres()); number(300,r.effectiveAreaSquareMetres());
        assertEquals(new BigDecimal("0.4500"),r.effectiveAreaMu());
    }
    @Test public void fullReturnHasDoubleLengthButSingleCoverage() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(path(0,100,0)),300,box(-10,-10,110,10),List.of());
        number(200,r.travelLengthMetres());number(600,r.grossAreaSquareMetres());number(300,r.overlapDeductionSquareMetres());number(300,r.effectiveAreaSquareMetres());
    }
    @Test public void centimetreWidthIsNotAccidentallyInterpretedAsMetres() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(path(0,100)),3,box(-10,-10,110,10),List.of());
        number(3,r.effectiveAreaSquareMetres());
    }
    @Test public void metreBasedWidthWorksAtChineseLatitudes() throws Exception {
        var field=new Polygon("POLYGON((114 37.999,114.01 37.999,114.01 38.001,114 38.001,114 37.999))");
        double east=lon(100)/Math.cos(Math.toRadians(38));
        var r=area.calculateSegmentedCoverage(List.of(List.of(new Point(114.001,38),new Point(114.001+east,38))),300,field,List.of());
        number(100,r.travelLengthMetres());number(300,r.effectiveAreaSquareMetres());
    }
    @Test public void gapsDoNotGenerateImaginaryConnectingCoverage() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(path(0,10),path(90,100)),300,box(-10,-10,110,10),List.of());
        number(20,r.travelLengthMetres());number(60,r.effectiveAreaSquareMetres());
    }
    @Test public void overlapIsRemovedAcrossSeparateSegments() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(path(0,100),path(100,0)),300,box(-10,-10,110,10),List.of());
        number(300,r.overlapDeductionSquareMetres());number(300,r.effectiveAreaSquareMetres());
    }
    @Test public void deductionsReconcileInDeduplicateThenBoundaryThenExclusionOrder() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(path(-20,120,-20)),400,box(0,-10,100,10),List.of(box(40,-10,60,10)));
        number(280,r.travelLengthMetres());number(1120,r.grossAreaSquareMetres());number(560,r.uniqueAreaSquareMetres());
        number(400,r.inBoundaryAreaSquareMetres());number(560,r.overlapDeductionSquareMetres());number(160,r.outsideDeductionSquareMetres());
        number(80,r.exclusionDeductionSquareMetres());number(320,r.effectiveAreaSquareMetres());
        assertEquals(r.grossAreaSquareMetres(),r.overlapDeductionSquareMetres().add(r.outsideDeductionSquareMetres()).add(r.exclusionDeductionSquareMetres()).add(r.effectiveAreaSquareMetres()));
    }
    @Test public void centreLineOnBoundaryOnlyCountsItsInsideHalfWidth() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(path(0,100)),400,box(0,0,100,10),List.of());
        number(200,r.effectiveAreaSquareMetres());number(200,r.outsideDeductionSquareMetres());
    }
    @Test public void outsideCentreLineCanHaveInsideSwathCoverage() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(List.of(point(0,-1),point(100,-1))),400,box(0,0,100,10),List.of());
        number(100,r.effectiveAreaSquareMetres());
    }
    @Test public void overlappingExclusionsAreDeductedOnce() throws Exception {
        var r=area.calculateSegmentedCoverage(List.of(path(0,100)),400,box(0,-10,100,10),List.of(box(40,-10,60,10),box(50,-10,70,10)));
        number(120,r.exclusionDeductionSquareMetres());number(280,r.effectiveAreaSquareMetres());
    }
    @Test public void nativeHoleAndExtraExclusionDoNotDoubleDeduct() throws Exception {
        String outer=box(0,-10,100,10).toString(),hole=box(40,-5,60,5).toString();
        Polygon field=new Polygon(outer.substring(0,outer.length()-1)+","+hole.substring(hole.indexOf("((")+1,hole.length()-1)+")");
        var r=area.calculateSegmentedCoverage(List.of(path(0,100)),400,field,List.of(box(50,-10,70,10)));
        number(80,r.outsideDeductionSquareMetres());number(40,r.exclusionDeductionSquareMetres());number(280,r.effectiveAreaSquareMetres());
    }
    @Test public void stationarySingletonsAndEmptyPathsHaveZeroArea() throws Exception {
        for(List<List<Point>> paths:List.of(List.<List<Point>>of(),List.of(path()),List.of(path(0)),List.of(path(0,0,0))))
            number(0,area.calculateSegmentedCoverage(paths,300,box(-10,-10,110,10),List.of()).effectiveAreaSquareMetres());
    }
    @Test public void emptyPathCanValidateARealChineseFieldWithoutProjectingFromZero() throws Exception {
        var field=new Polygon("POLYGON((114 38,114.001 38,114.001 38.001,114 38.001,114 38))");
        number(0,area.calculateSegmentedCoverage(List.of(),300,field,List.of()).effectiveAreaSquareMetres());
        number(0,area.calculateClippedArea(List.of(),300,field,List.of()));
    }
    @Test public void invalidGeometryFailsEvenWithoutMovingPoints() throws Exception {
        var bad=new Polygon("POLYGON((0 0,1 1,0 1,1 0,0 0))");
        assertThrows(IllegalArgumentException.class,()->area.calculateSegmentedCoverage(List.of(),300,bad,List.of()));
        assertThrows(IllegalArgumentException.class,()->area.calculateSegmentedCoverage(List.of(path(0)),300,box(0,-10,100,10),Arrays.asList((Polygon)null)));
        Polygon wrong=box(0,-10,100,10);wrong.setSrid(3857);
        assertThrows(IllegalArgumentException.class,()->area.calculateSegmentedCoverage(List.of(path(0,100)),300,wrong,List.of()));
    }
    @Test public void invalidSegmentsCoordinatesAndWidthsAreRejected() throws Exception {
        Polygon field=box(-10,-10,110,10);
        assertThrows(IllegalArgumentException.class,()->area.calculateSegmentedCoverage(null,300,field,List.of()));
        assertThrows(IllegalArgumentException.class,()->area.calculateSegmentedCoverage(Arrays.asList((List<Point>)null),300,field,List.of()));
        assertThrows(IllegalArgumentException.class,()->area.calculateSegmentedCoverage(List.of(path(0,100)),0,field,List.of()));
        assertThrows(IllegalArgumentException.class,()->area.calculateSegmentedCoverage(List.of(List.of(point(0,0),new Point(Double.NaN,0))),300,field,List.of()));
    }
    @Test public void calculationDoesNotMutateInputCoordinatesOrAppendPoints() throws Exception {
        var points=new ArrayList<>(path(0,100));double before=points.get(1).getLongitude();
        area.calculateSegmentedCoverage(List.of(points),300,box(-10,-10,110,10),List.of());
        assertEquals(2,points.size());assertEquals(before,points.get(1).getLongitude(),0);
    }
}
