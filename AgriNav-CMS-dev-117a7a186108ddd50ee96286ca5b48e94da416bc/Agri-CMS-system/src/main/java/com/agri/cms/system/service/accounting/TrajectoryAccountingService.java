package com.agri.cms.system.service.accounting;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.mapper.TrajectoryPlaybackMapper;
import com.agri.cms.system.service.boundary.BoundaryCoordinates;
import com.agri.cms.system.service.boundary.BoundaryQueryService;
import com.agri.cms.system.service.boundary.BoundaryQueryService.Actor;
import com.agri.cms.system.service.impl.AreaCalculationServiceImpl;
import com.agri.cms.system.service.playback.PlaybackModels;
import com.agri.cms.system.service.playback.TrajectoryPlaybackService;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import static com.agri.cms.system.service.accounting.AccountingModels.*;

/** B-10：只读幅宽试算，复用现有覆盖模型，不更新归档面积或结算。 */
@Service
@DS("pg")
public class TrajectoryAccountingService {
    public static final int MAX_POINTS = 10000;
    public static final int MAX_SEGMENTS = 1000;
    public static final int MAX_WIDTH_CM = 10000;
    private final BoundaryQueryService boundaries;
    private final AreaCalculationServiceImpl area;
    private final TrajectoryPlaybackMapper mapper;
    private final TrajectoryPlaybackService playback;

    public TrajectoryAccountingService(BoundaryQueryService boundaries, AreaCalculationServiceImpl area,
                                       TrajectoryPlaybackMapper mapper, TrajectoryPlaybackService playback) {
        this.boundaries = boundaries;
        this.area = area;
        this.mapper = mapper;
        this.playback = playback;
    }

    public Result preview(PreviewRequest request, Actor actor) {
        authenticate(actor);
        if (request == null) throw new ServiceException("请提交地块、厘米幅宽和轨迹片段");
        int width = width(request.widthCm());
        List<List<Point>> segments = points(request.segments());
        return calculate(null, request.plotId(), width, segments, "SUBMITTED_SEGMENTS", null, List.of(), actor);
    }

    public Result archived(String trajectoryId, Actor actor) {
        authenticate(actor);
        if (trajectoryId == null || trajectoryId.isBlank() || trajectoryId.length() > 64)
            throw new ServiceException("请指定有效作业单号");
        TrajectoryInfo order = mapper.order(trajectoryId);
        if (order == null || Short.valueOf((short) 2).equals(order.getStatus())
                || (!actor.admin() && !String.valueOf(actor.userId()).equals(order.getCreatedBy())
                && (actor.username() == null || !actor.username().equals(order.getPhoneNumber()))))
            throw new ServiceException("作业单不存在或无权访问", 403);
        int width = width(order.getWidth());
        var saved = playback.query(new PlaybackModels.Query("EFFECTIVE", trajectoryId, null, null, null),
                new PlaybackModels.Actor(actor.userId(), actor.username(), actor.admin()));
        if (saved.skippedInvalid() != 0)
            throw new ServiceException("归档轨迹存在无效定位点，请先复核，不能跳过后直接核算");
        if (saved.tracks().size() != 1 || saved.pointCount() == 0)
            throw new ServiceException("作业单尚无可核算的归档轨迹，不使用暂存点或其他作业单替代");
        var track = saved.tracks().get(0);
        if (!trajectoryId.equals(track.trajectoryId()) || order.getPlotId() == null
                || !order.getPlotId().toString().equals(track.plotId()))
            throw new ServiceException("归档轨迹与作业单地块不一致，请先复核");
        var coordinates = track.segments().stream().map(segment -> segment.stream()
                .map(point -> new Coordinate(point.longitude(), point.latitude())).toList()).toList();
        List<String> warnings = new ArrayList<>(saved.warnings());
        warnings.add("归档回放按超过5分钟的间隔及同刻异位点分段；本次不连接片段，也不替代B-11异常识别");
        warnings.add("使用当前地块边界及当前启用的排除区重算，历史边界可能不同，差值仅供复核");
        return calculate(trajectoryId, order.getPlotId(), width, points(coordinates), "POSTGIS_ARCHIVE",
                order.getArea(), warnings, actor);
    }

    private Result calculate(String id, Long plotId, int width, List<List<Point>> segments,
                             String source, BigDecimal recorded, List<String> notices, Actor actor) {
        var field = boundaries.accountingBoundary(plotId, actor);
        try {
            var calculation = area.calculateSegmentedCoverage(segments, width, field.boundary(), field.exclusions());
            List<String> warnings = new ArrayList<>(notices);
            warnings.add("只读试算：未回填面积、未修改轨迹或结算；幅宽和定位精度仍需B-17实测校准");
            warnings.add("输入须为同一地块、同一幅宽的有效作业片段；不推断幅宽，不自动识别转场、停留、超速或漏采");
            if (segments.stream().allMatch(segment -> segment.size() < 2))
                warnings.add("没有至少两个连续定位点的片段，覆盖面积为零");
            BigDecimal difference = recorded == null ? null : calculation.effectiveAreaMu().subtract(recorded)
                    .setScale(4, RoundingMode.HALF_UP);
            return new Result(id, plotId.toString(), source, "WGS84", true, width, segments.size(),
                    segments.stream().mapToInt(List::size).sum(), calculation, recorded, difference, List.copyOf(warnings));
        } catch (IllegalArgumentException invalid) {
            throw new ServiceException(invalid.getMessage());
        }
    }

    private List<List<Point>> points(List<List<Coordinate>> segments) {
        if (segments == null || segments.size() > MAX_SEGMENTS)
            throw new ServiceException("请提供轨迹片段，每次最多1000个片段");
        int total = 0;
        List<List<Point>> result = new ArrayList<>();
        for (List<Coordinate> segment : segments) {
            if (segment == null) throw new ServiceException("轨迹片段不能为null");
            total += segment.size();
            if (total > MAX_POINTS) throw new ServiceException("幅宽试算每次最多10000点，请缩小范围");
            List<Point> path = new ArrayList<>();
            for (Coordinate coordinate : segment) {
                if (coordinate == null) throw new ServiceException("轨迹中不能包含空定位点");
                try { BoundaryCoordinates.validate(coordinate.longitude(), coordinate.latitude()); }
                catch (IllegalArgumentException invalid) { throw new ServiceException(invalid.getMessage()); }
                path.add(new Point(coordinate.longitude(), coordinate.latitude()));
            }
            result.add(path);
        }
        return result;
    }

    private int width(Integer width) {
        if (width == null || width <= 0 || width > MAX_WIDTH_CM)
            throw new ServiceException("幅宽须为1至10000厘米的整数（例如3米填写300），不能使用缺省值-1");
        return width;
    }

    private void authenticate(Actor actor) {
        if (actor == null || actor.userId() <= 0) throw new ServiceException("请先登录", 401);
    }
}
