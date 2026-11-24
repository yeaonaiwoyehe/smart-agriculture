package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.TrajectoryPlaybackMapper;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import static com.agri.cms.system.service.playback.PlaybackModels.*;

@Service
public class TrajectoryPlaybackService {
    public static final int MAX_POINTS=50000;
    private final TrajectoryPlaybackMapper mapper;
    private final RawPlaybackReader raw;
    public TrajectoryPlaybackService(TrajectoryPlaybackMapper mapper,RawPlaybackReader raw) {this.mapper=mapper;this.raw=raw;}

    public Result query(Query query,Actor actor) {
        if(actor==null||actor.userId()<=0)throw new ServiceException("请先登录",401);
        if(query==null||!Set.of("ORIGINAL","EFFECTIVE").contains(Objects.toString(query.type(),"")))throw new ServiceException("type须为ORIGINAL或EFFECTIVE");
        String phone=query.phone();LocalDateTime start=query.startTime(),end=query.endTime();
        TrajectoryInfo selected=null;
        if(query.trajectoryId()!=null) {
            if(query.trajectoryId().isBlank()||query.trajectoryId().length()>64)throw new ServiceException("作业单号格式无效");
            selected=mapper.order(query.trajectoryId());
            if(selected==null||Short.valueOf((short)2).equals(selected.getStatus())||(!actor.admin()&&!owned(selected,actor)))throw new ServiceException("作业单不存在或无权访问",403);
            if(phone!=null&&!phone.equals(selected.getPhoneNumber()))throw new ServiceException("手机号与作业单不一致");
            phone=selected.getPhoneNumber();
            if(selected.getStartTime()==null)throw new ServiceException("作业单缺少开始时间，无法定位轨迹");
            LocalDateTime orderEnd=selected.getEndTime()==null?LocalDateTime.now(ZoneId.of("Asia/Shanghai")):selected.getEndTime();
            start=start==null?selected.getStartTime():(start.isBefore(selected.getStartTime())?selected.getStartTime():start);
            end=end==null?orderEnd:(end.isAfter(orderEnd)?orderEnd:end);
        } else {
            if(phone==null)phone=actor.phone();
            if(!actor.admin()&&!Objects.equals(phone,actor.phone()))throw new ServiceException("仅管理员可按手机号查询其他终端的轨迹",403);
        }
        if(phone==null||!phone.matches("[0-9]{5,20}"))throw new ServiceException("请提供5至20位数字的终端手机号");
        if(start==null||end==null||start.isAfter(end)||Duration.between(start,end).compareTo(Duration.ofHours(24))>0)
            throw new ServiceException("请提供先后有序且不超过24小时的起止时间；长作业请分时间段查询");
        List<Track> tracks=new ArrayList<>();List<String> warnings=new ArrayList<>();int[] counts=new int[4];
        if(query.type().equals("ORIGINAL")) {
            tracks.add(track(selected,"ELASTICSEARCH",raw.read(phone,start,end),start,end,counts));
            if(selected!=null)warnings.add("原始点按作业单绑定终端及时间段查询，包含地块外行驶点，未按地块边界裁剪");
        } else {
            List<TrajectoryInfo> orders=selected==null?mapper.orders(phone,start,end):List.of(selected);
            if(orders.size()>200)throw new ServiceException("作业单超过200条，请缩小时间范围");
            for(TrajectoryInfo order:orders) {
                if(Short.valueOf((short)2).equals(order.getStatus()))continue;
                if(!actor.admin()&&!owned(order,actor))throw new ServiceException("作业单数据范围异常",403);
                var archive=mapper.archive(order.getTrajectoryInfoId());
                if(archive==null||archive.geometry()==null) {warnings.add("作业单 "+order.getTrajectoryInfoId()+" 尚无已保存的有效轨迹；暂存点不会被冒充为归档轨迹");continue;}
                List<Sample> points=decodeArchive(archive.geometry(),archive.times());
                if(counts[2]+points.size()>MAX_POINTS)throw new ServiceException("轨迹超过50000点，请缩小时间范围");
                counts[2]+=points.size();
                LocalDateTime from=order.getStartTime()==null?start:(start.isAfter(order.getStartTime())?start:order.getStartTime());
                LocalDateTime to=order.getEndTime()==null?end:(end.isBefore(order.getEndTime())?end:order.getEndTime());
                tracks.add(track(order,"POSTGIS_ARCHIVE",points,from,to,counts));
            }
            warnings.add("有效轨迹为已保存的作业点序列，不代表每个返回点都参与面积核算；面积仍以裁剪去重后的核算结果为准");
        }
        if(counts[0]>0)warnings.add("已移除完全重复的点（坐标与时间均相同）："+counts[0]);
        if(counts[1]>0)warnings.add("已跳过缺少时间或坐标无效的点："+counts[1]);
        if(counts[3]>0)warnings.add("同一时间存在不同位置的点，已隔离展示，未推测行驶先后顺序");
        int total=tracks.stream().mapToInt(Track::pointCount).sum();
        return new Result(query.type(),"WGS84","Asia/Shanghai",phone,format(start),format(end),tracks,total,counts[0],counts[1],warnings);
    }

    private boolean owned(TrajectoryInfo order,Actor actor) {
        return String.valueOf(actor.userId()).equals(order.getCreatedBy())||(actor.phone()!=null&&actor.phone().equals(order.getPhoneNumber()));
    }
    static String format(LocalDateTime time) {return time.atOffset(ZoneOffset.ofHours(8)).toString();}
    static List<Sample> decodeArchive(String geometry,String times) {
        try {
            if(times==null||times.isBlank())throw new IllegalArgumentException();
            var parsed=new WKTReader().read(geometry);
            if(!(parsed instanceof Polygon polygon)||polygon.getNumInteriorRing()!=0)throw new IllegalArgumentException();
            var coordinates=polygon.getExteriorRing().getCoordinates();String[] timestamps=times.split(",",-1);
            int size=coordinates.length;
            if(size==timestamps.length+1&&coordinates[0].equals2D(coordinates[size-1]))size--;
            if(size!=timestamps.length)throw new IllegalArgumentException();
            if(size>MAX_POINTS)throw new ServiceException("轨迹超过50000点，请缩小时间范围");
            List<Sample> points=new ArrayList<>();
            for(int i=0;i<size;i++)points.add(new Sample(coordinates[i].x,coordinates[i].y,LocalDateTime.parse(timestamps[i].trim())));
            return points;
        } catch(ServiceException limit){throw limit;}
        catch(Exception invalid){throw new ServiceException("已保存轨迹的坐标与时间数据不匹配或格式损坏，无法可靠回放");}
    }
    private Track track(TrajectoryInfo order,String source,List<Sample> input,LocalDateTime start,LocalDateTime end,int[] counts) {
        if(input.size()>MAX_POINTS)throw new ServiceException("轨迹超过50000点，请缩小时间范围");
        Set<Sample> unique=new HashSet<>();List<Sample> points=new ArrayList<>();
        for(Sample point:input) {
            if(point==null||point.time()==null||!Double.isFinite(point.longitude())||!Double.isFinite(point.latitude())
                    ||Math.abs(point.longitude())>180||Math.abs(point.latitude())>90){counts[1]++;continue;}
            if(point.time().isBefore(start)||point.time().isAfter(end))continue;
            if(!unique.add(point)){counts[0]++;continue;}points.add(point);
        }
        points.sort(Comparator.comparing(Sample::time).thenComparingDouble(Sample::longitude).thenComparingDouble(Sample::latitude));
        List<List<Point>> segments=new ArrayList<>();List<Point> segment=null;Sample previous=null;
        boolean previousAmbiguous=false;
        for(int i=0;i<points.size();i++) {
            Sample point=points.get(i);
            boolean ambiguous=(i>0&&points.get(i-1).time().equals(point.time()))||(i+1<points.size()&&points.get(i+1).time().equals(point.time()));
            if(ambiguous)counts[3]++;
            if(previous==null||Duration.between(previous.time(),point.time()).compareTo(Duration.ofMinutes(5))>0||ambiguous||previousAmbiguous) {
                segment=new ArrayList<>();segments.add(segment);
            }
            segment.add(new Point(point.longitude(),point.latitude(),format(point.time())));previous=point;
            previousAmbiguous=ambiguous;
        }
        return new Track(order==null?null:order.getTrajectoryInfoId(),order==null||order.getPlotId()==null?null:order.getPlotId().toString(),source,segments,points.size());
    }
}
