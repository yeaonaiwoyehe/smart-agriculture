package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.service.boundary.BoundaryCoordinates;
import com.agri.cms.system.service.boundary.BoundaryModels.*;
import com.agri.cms.system.service.boundary.BoundaryModels.Position;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.util.*;

/** B-09：线段裁剪只报告累计长度，不替代 B-10 的幅宽覆盖去重面积。 */
@Service
public class BoundaryClippingService {
    private final AreaCalculationServiceImpl area;
    private final GeometryFactory geometries=new GeometryFactory(new PrecisionModel(),4326);
    public BoundaryClippingService(AreaCalculationServiceImpl area) {this.area=area;}

    public ClippedTrack clip(List<Point> points,org.postgis.Polygon boundary,List<org.postgis.Polygon> exclusions) {
        if(points==null||points.size()>10000)throw new IllegalArgumentException("轨迹列表必须提供，每次最多10000点");
        if(exclusions==null)throw new IllegalArgumentException("排除区列表不能为空引用");
        boolean timed=false;LocalDateTime previous=null;
        for(int i=0;i<points.size();i++) {
            Point point=points.get(i);if(point==null)throw new IllegalArgumentException("轨迹中不能有空点");
            BoundaryCoordinates.validate(point.getLongitude(),point.getLatitude());
            if(i==0)timed=point.getTime()!=null;
            if(timed!=(point.getTime()!=null))throw new IllegalArgumentException("轨迹时间须全部提供或全部省略");
            if(timed&&previous!=null&&point.getTime().isBefore(previous))throw new IllegalArgumentException("轨迹时间须升序，不能重新排序后改变行驶路径");
            previous=point.getTime();
        }
        if(timed&&points.size()>1&&Duration.between(points.get(0).getTime(),previous).compareTo(Duration.ofHours(24))>0)
            throw new IllegalArgumentException("单次轨迹最长24小时，请分段提交");
        Geometry field=read(boundary);List<Geometry> excluded=new ArrayList<>();
        for(var polygon:exclusions)excluded.add(read(polygon));
        Geometry mask=excluded.isEmpty()?geometries.createGeometryCollection():UnaryUnionOp.union(excluded);
        var center=field.getInteriorPoint().getCoordinate();
        var projection=new AreaCalculationServiceImpl.Projection(new Point(center.x,center.y));
        // 共用面积核算的输入几何与局部投影检查，拒绝坏边界而非返回正常零长度。
        area.projectPolygon(boundary,projection);for(var polygon:exclusions)area.projectPolygon(polygon,projection);
        List<Position> positions=new ArrayList<>();
        for(Point point:points)positions.add(position(field,mask,geometries.createPoint(new Coordinate(point.getLongitude(),point.getLatitude()))));
        List<Fragment> fragments=new ArrayList<>();double total=0,fieldLength=0,inside=0;
        for(int i=1;i<points.size();i++) {
            Point a=points.get(i-1),b=points.get(i);Coordinate first=new Coordinate(a.getLongitude(),a.getLatitude()),last=new Coordinate(b.getLongitude(),b.getLatitude());
            if(Math.abs(first.x-last.x)>=180)throw new IllegalArgumentException("不支持跨日期变更线的轨迹");
            total+=distance(first,last,projection);if(first.equals2D(last))continue;
            Geometry segment=geometries.createLineString(new Coordinate[]{first,last});
            Geometry inField=segment.intersection(field);fieldLength+=length(inField,projection);
            Geometry usable=mask.isEmpty()?inField:inField.difference(mask);inside+=length(usable,projection);
            List<Fragment> pieces=new ArrayList<>();fragments(usable,new LineSegment(first,last),a,b,i-1,pieces);
            pieces.sort(Comparator.comparingDouble(Fragment::startFraction));fragments.addAll(pieces);
        }
        return new ClippedTrack("WGS84","metre",points.size(),positions,fragments,metres(total),metres(inside),
                metres(Math.max(0,total-fieldLength)),metres(Math.max(0,fieldLength-inside)));
    }

    public Position classify(Point point,org.postgis.Polygon boundary,List<org.postgis.Polygon> exclusions) {
        if(point==null)throw new IllegalArgumentException("定位点不能为空");
        return clip(List.of(point),boundary,exclusions).pointPositions().get(0);
    }
    private Position position(Geometry field,Geometry excluded,org.locationtech.jts.geom.Point point) {
        if(!field.covers(point))return Position.OUTSIDE;
        if(excluded.covers(point))return Position.EXCLUDED;
        return field.getBoundary().covers(point)?Position.BOUNDARY:Position.INSIDE;
    }
    private Geometry read(org.postgis.Polygon polygon) {
        if(polygon==null||(polygon.getSrid()!=0&&polygon.getSrid()!=4326))throw new IllegalArgumentException("边界须为WGS84 / SRID 4326");
        try {
            Geometry geometry=new WKTReader(geometries).read(polygon.toString().replaceFirst("^SRID=[0-9]+;",""));
            if(!(geometry instanceof Polygon)||geometry.isEmpty()||!geometry.isValid()||geometry.getEnvelopeInternal().getWidth()>=180)
                throw new IllegalArgumentException("地块边界为空、无效或跨日期变更线");
            for(Coordinate c:geometry.getCoordinates())BoundaryCoordinates.validate(c.x,c.y);
            return geometry;
        } catch(org.locationtech.jts.io.ParseException invalid){throw new IllegalArgumentException("无法解析地块边界",invalid);}
    }
    private double distance(Coordinate a,Coordinate b,AreaCalculationServiceImpl.Projection projection) {
        return projection.project(a.x,a.y).distance(projection.project(b.x,b.y));
    }
    private double length(Geometry geometry,AreaCalculationServiceImpl.Projection projection) {
        if(geometry instanceof LineString line){double result=0;var c=line.getCoordinates();for(int i=1;i<c.length;i++)result+=distance(c[i-1],c[i],projection);return result;}
        double result=0;if(geometry instanceof GeometryCollection)for(int i=0;i<geometry.getNumGeometries();i++)result+=length(geometry.getGeometryN(i),projection);return result;
    }
    private void fragments(Geometry geometry,LineSegment original,Point a,Point b,int index,List<Fragment> output) {
        if(geometry instanceof LineString line&&!line.isEmpty()) {
            double start=original.projectionFactor(line.getCoordinateN(0)),end=original.projectionFactor(line.getCoordinateN(line.getNumPoints()-1));
            if(start>end){double swap=start;start=end;end=swap;}start=Math.max(0,Math.min(1,start));end=Math.max(0,Math.min(1,end));
            if(end-start>1e-12)output.add(new Fragment(index,start,end,point(original.pointAlong(start),a,b,start),point(original.pointAlong(end),a,b,end)));
        } else if(geometry instanceof GeometryCollection)for(int i=0;i<geometry.getNumGeometries();i++)fragments(geometry.getGeometryN(i),original,a,b,index,output);
    }
    private ClippedPoint point(Coordinate point,Point a,Point b,double fraction) {
        String time=null;
        if(a.getTime()!=null) {
            // 线段比例来自浮点交点，避免将坐标舍入误差放大成不稳定的纳秒尾数。
            LocalDateTime interpolated=a.getTime().plusNanos(Math.round(Duration.between(a.getTime(),b.getTime()).toNanos()*fraction));
            long millis=Math.round(interpolated.getNano()/1_000_000d);
            time=interpolated.withNano(0).plusNanos(millis*1_000_000L).atOffset(ZoneOffset.ofHours(8)).toString();
        }
        return new ClippedPoint(point.x,point.y,time);
    }
    private BigDecimal metres(double length) {return BigDecimal.valueOf(length).setScale(3,RoundingMode.HALF_UP);}
}
