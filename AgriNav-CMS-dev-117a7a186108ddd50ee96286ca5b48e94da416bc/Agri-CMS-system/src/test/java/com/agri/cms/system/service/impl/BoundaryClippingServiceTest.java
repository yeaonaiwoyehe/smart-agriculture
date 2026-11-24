package com.agri.cms.system.service.impl;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.service.boundary.BoundaryModels.*;
import org.junit.Test;
import org.postgis.Polygon;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
public class BoundaryClippingServiceTest {
    private final BoundaryClippingService service=new BoundaryClippingService(new AreaCalculationServiceImpl());
    static Point point(double x,double y){return new Point(Math.toDegrees(x/6371000),Math.toDegrees(y/6371000));}
    static Polygon polygon(double... xy)throws Exception{
        StringJoiner coordinates=new StringJoiner(",");for(int i=0;i<xy.length;i+=2){Point p=point(xy[i],xy[i+1]);coordinates.add(p.getLongitude()+" "+p.getLatitude());}
        return new Polygon("POLYGON(("+coordinates+"))");
    }
    static Polygon box(double x1,double y1,double x2,double y2)throws Exception{return polygon(x1,y1,x2,y1,x2,y2,x1,y2,x1,y1);}
    @Test public void classifiesInsideOutsideAndBoundaryIncludingVertex()throws Exception{
        var field=box(0,-10,100,10);assertEquals(Position.INSIDE,service.classify(point(50,0),field,List.of()));
        assertEquals(Position.OUTSIDE,service.classify(point(-10,0),field,List.of()));assertEquals(Position.BOUNDARY,service.classify(point(0,0),field,List.of()));assertEquals(Position.BOUNDARY,service.classify(point(100,10),field,List.of()));
    }
    @Test public void twoOutsideEndpointsStillRetainCrossingInteriorAndFractions()throws Exception{
        var result=service.clip(List.of(point(-50,0),point(150,0)),box(0,-10,100,10),List.of());
        assertEquals(200,result.totalLength().doubleValue(),.001);assertEquals(100,result.insideLength().doubleValue(),.001);assertEquals(100,result.outsideLength().doubleValue(),.001);
        assertEquals(1,result.fragments().size());assertEquals(.25,result.fragments().get(0).startFraction(),1e-10);assertEquals(.75,result.fragments().get(0).endFraction(),1e-10);
        assertEquals(List.of(Position.OUTSIDE,Position.OUTSIDE),result.pointPositions());
    }
    @Test public void outsideLineAndSingleTouchHaveNoCountedLength()throws Exception{
        var field=box(0,-10,100,10);assertEquals(0,service.clip(List.of(point(-50,20),point(150,20)),field,List.of()).insideLength().doubleValue(),.001);
        assertTrue(service.clip(List.of(point(-10,-20),point(0,-10)),field,List.of()).fragments().isEmpty());
    }
    @Test public void boundaryTravelCountsButDoesNotCreateAPolygonClosure()throws Exception{
        var result=service.clip(List.of(point(0,10),point(100,10)),box(0,-10,100,10),List.of());assertEquals(100,result.insideLength().doubleValue(),.001);assertEquals(1,result.fragments().size());
    }
    @Test public void exclusionsSplitAndOverlapsAreDeductedOnlyOnce()throws Exception{
        var exclusions=List.of(box(20,-10,50,10),box(40,-10,60,10));var result=service.clip(List.of(point(0,0),point(100,0)),box(0,-10,100,10),exclusions);
        assertEquals(60,result.insideLength().doubleValue(),.001);assertEquals(40,result.excludedLength().doubleValue(),.001);assertEquals(2,result.fragments().size());
        assertEquals(Position.EXCLUDED,service.classify(point(20,0),box(0,-10,100,10),exclusions));
    }
    @Test public void holeInteriorExcludedButItsEdgeIsABoundary()throws Exception{
        var field=box(0,-10,100,10);String hole=box(40,-5,60,5).toString();String wkt=field.toString();
        Polygon withHole=new Polygon(wkt.substring(0,wkt.length()-1)+","+hole.substring(hole.indexOf("((")+1,hole.length()-1)+")");
        var result=service.clip(List.of(point(0,0),point(100,0)),withHole,List.of());assertEquals(80,result.insideLength().doubleValue(),.001);assertEquals(2,result.fragments().size());
        assertEquals(Position.OUTSIDE,service.classify(point(50,0),withHole,List.of()));assertEquals(Position.BOUNDARY,service.classify(point(40,0),withHole,List.of()));
    }
    @Test public void reverseTravelPreservesDirectionAndTimeAtIntersections()throws Exception{
        var a=point(150,0);var b=point(-50,0);a.setTime(LocalDateTime.of(2026,11,24,8,0));b.setTime(a.getTime().plusSeconds(200));
        var result=service.clip(List.of(a,b),box(0,-10,100,10),List.of(box(40,-10,60,10)));
        assertEquals(2,result.fragments().size());assertTrue(result.fragments().get(0).start().longitude()>result.fragments().get(0).end().longitude());
        assertEquals("2026-11-24T08:00:50+08:00",result.fragments().get(0).start().time());
    }
    @Test public void sourceSegmentsStaySeparateAndRepeatedTraversalIsNotAreaDeduplication()throws Exception{
        var result=service.clip(List.of(point(0,0),point(100,0),point(0,0)),box(0,-10,100,10),List.of());assertEquals(200,result.insideLength().doubleValue(),.001);assertEquals(2,result.fragments().size());assertEquals(1,result.fragments().get(1).sourceSegmentIndex());
    }
    @Test public void emptySingleAndRepeatedPointsProduceZeroLength()throws Exception{
        for(List<Point> points:List.of(List.<Point>of(),List.of(point(20,0)),List.of(point(20,0),point(20,0))))assertEquals(0,service.clip(points,box(0,-10,100,10),List.of()).insideLength().doubleValue(),.001);
    }
    @Test public void malformedBoundaryOrCoordinatesFailRatherThanReturningZero()throws Exception{
        var field=box(0,-10,100,10);assertThrows(IllegalArgumentException.class,()->service.clip(List.of(new Point(Double.NaN,0)),field,List.of()));
        assertThrows(IllegalArgumentException.class,()->service.clip(List.of(),null,List.of()));var wrong=box(0,-10,100,10);wrong.setSrid(3857);assertThrows(IllegalArgumentException.class,()->service.clip(List.of(),wrong,List.of()));
        var invalid=polygon(0,0,100,10,100,0,0,10,0,0);assertThrows(IllegalArgumentException.class,()->service.clip(List.of(),invalid,List.of()));
    }
    @Test public void mixedOrReversedTimesAndOversizedInputAreRejected()throws Exception{
        var field=box(0,-10,100,10);Point a=point(0,0),b=point(100,0);a.setTime(LocalDateTime.now());assertThrows(IllegalArgumentException.class,()->service.clip(List.of(a,b),field,List.of()));
        b.setTime(a.getTime().minusSeconds(1));assertThrows(IllegalArgumentException.class,()->service.clip(List.of(a,b),field,List.of()));assertThrows(IllegalArgumentException.class,()->service.clip(Collections.nCopies(10001,a),field,List.of()));
    }
    @Test public void decimalDegreeIntersectionTimesDoNotExposeNanosecondRoundingNoise()throws Exception{
        var field=new Polygon("POLYGON((114 38,114.001 38,114.001 38.001,114 38.001,114 38))");
        var a=new Point(113.9995,38.0005,LocalDateTime.of(2026,11,24,8,0));
        var b=new Point(114.0015,38.0005,a.getTime().plusMinutes(2));
        var fragment=service.clip(List.of(a,b),field,List.of()).fragments().get(0);
        assertEquals("2026-11-24T08:00:30+08:00",fragment.start().time());
        assertEquals("2026-11-24T08:01:30+08:00",fragment.end().time());
    }
    @Test public void fractionalSourceTimesAreRoundedAtBothEndpoints()throws Exception{
        var a=point(10,0);var b=point(90,0);a.setTime(LocalDateTime.of(2026,11,24,8,0).withNano(900000));b.setTime(LocalDateTime.of(2026,11,24,8,0,1).withNano(100000));
        var fragment=service.clip(List.of(a,b),box(0,-10,100,10),List.of()).fragments().get(0);
        assertEquals("2026-11-24T08:00:00.001+08:00",fragment.start().time());assertEquals("2026-11-24T08:00:01+08:00",fragment.end().time());
    }
}
