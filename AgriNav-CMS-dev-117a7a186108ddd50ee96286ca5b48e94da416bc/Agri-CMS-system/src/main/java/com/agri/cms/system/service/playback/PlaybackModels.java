package com.agri.cms.system.service.playback;

import java.time.LocalDateTime;
import java.util.List;

public final class PlaybackModels {
    private PlaybackModels() { }
    public record Actor(long userId,String phone,boolean admin) { }
    public record Query(String type,String trajectoryId,String phone,LocalDateTime startTime,LocalDateTime endTime) { }
    public record Sample(double longitude,double latitude,LocalDateTime time) { }
    public record Point(double longitude,double latitude,String time) { }
    public record Track(String trajectoryId,String plotId,String source,List<List<Point>> segments,int pointCount) { }
    public record Result(String type,String coordinateSystem,String timeZone,String phone,String startTime,String endTime,
                         List<Track> tracks,int pointCount,int removedDuplicates,int skippedInvalid,List<String> warnings) { }
}
