package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.vo.Point;
import org.junit.Test;
import org.postgis.Polygon;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class AreaClippingTest {
    private final AreaCalculationServiceImpl service = new AreaCalculationServiceImpl();
    static Polygon polygon(double... xy) throws Exception {
        StringBuilder wkt = new StringBuilder("POLYGON((");
        for (int i = 0; i < xy.length; i += 2) {
            if (i > 0) wkt.append(',');
            Point p = AreaCalculationServiceImplTest.point(xy[i], xy[i+1]);
            wkt.append(p.getLongitude()).append(' ').append(p.getLatitude());
        }
        return new Polygon(wkt.append("))").toString());
    }
    static Polygon rectangle(double x1, double y1, double x2, double y2) throws Exception {
        return polygon(x1,y1,x2,y1,x2,y2,x1,y2,x1,y1);
    }
    private void area(double expected, List<Point> points, Polygon boundary, List<Polygon> exclusions) {
        assertEquals(expected, service.calculateClippedArea(points,300,boundary,exclusions).doubleValue(),0.02);
    }
    @Test public void clipsBothOutsideEndpoints() throws Exception {
        area(180, AreaCalculationServiceImplTest.points(0,100), rectangle(20,-10,80,10),Collections.emptyList());
    }
    @Test public void clipsSwathAtFieldEdge() throws Exception {
        area(150, AreaCalculationServiceImplTest.points(0,100),rectangle(0,0,100,20),Collections.emptyList());
    }
    @Test public void clipsConcaveGapEvenWhenBothEndpointsAreInside() throws Exception {
        Polygon field=polygon(0,0,100,0,100,100,80,100,80,20,20,20,20,100,0,100,0,0);
        area(60,Arrays.asList(AreaCalculationServiceImplTest.point(10,30),AreaCalculationServiceImplTest.point(90,30)),field,Collections.emptyList());
    }
    @Test public void exclusionsAreUnionedBeforeSubtracting() throws Exception {
        area(180,AreaCalculationServiceImplTest.points(0,100),rectangle(-10,-10,110,10),
            Arrays.asList(rectangle(20,-5,50,5),rectangle(40,-5,60,5),rectangle(20,-5,50,5)));
    }
    @Test public void exclusionsOutsideFieldDoNotMakeAreaNegative() throws Exception {
        area(0,AreaCalculationServiceImplTest.points(0,100),rectangle(200,-10,300,10),Collections.singletonList(rectangle(-1,-5,101,5)));
    }
    @Test public void interiorRingIsExcluded() throws Exception {
        Polygon outer=rectangle(-10,-10,110,10),hole=rectangle(40,-5,60,5);
        String wkt=outer.toString();
        Polygon field=new Polygon(wkt.substring(0,wkt.length()-1)+","+hole.toString().substring("POLYGON(".length()));
        area(240,AreaCalculationServiceImplTest.points(0,100),field,Collections.emptyList());
    }
    @Test public void invalidBoundaryAndCoordinateSystemAreRejected() throws Exception {
        Polygon bowtie=polygon(0,0,100,100,0,100,100,0,0,0);
        assertThrows(IllegalArgumentException.class,()->service.calculateClippedArea(AreaCalculationServiceImplTest.points(0,100),300,bowtie,Collections.emptyList()));
        Polygon wrong=rectangle(0,-5,100,5); wrong.setSrid(3857);
        assertThrows(IllegalArgumentException.class,()->service.calculateClippedArea(AreaCalculationServiceImplTest.points(0,100),300,wrong,Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,()->service.calculateClippedArea(AreaCalculationServiceImplTest.points(0,100),300,null,Collections.emptyList()));
    }
    @Test public void widthChangesCoverageAndReversingOrderDoesNot() {
        List<Point> route=AreaCalculationServiceImplTest.points(0,100,50,150);
        assertEquals(150,service.calculateTrajectoryAreaAndDuplicateRemoval(route,100).doubleValue(),0.02);
        Collections.reverse(route);
        assertEquals(450,service.calculateTrajectoryAreaAndDuplicateRemoval(route,300).doubleValue(),0.02);
    }
    @Test public void localProjectionWorksAwayFromEquator() {
        double lat=38,lon=114;
        double dLon=Math.toDegrees(100/(6371000*Math.cos(Math.toRadians(lat))));
        assertEquals(300,service.calculateTrajectoryAreaAndDuplicateRemoval(Arrays.asList(new Point(lon,lat),new Point(lon+dLon,lat)),300).doubleValue(),0.02);
    }
}