package com.agri.cms.web.controller.trajectory;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.service.playback.PlaybackModels.Actor;
import com.agri.cms.system.service.playback.PlaybackModels.Query;
import com.agri.cms.system.service.playback.TrajectoryAnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Tag(name = "B-11 轨迹异常识别")
public class TrajectoryAnomalyController extends BaseController {
    private final TrajectoryAnomalyService service;

    public TrajectoryAnomalyController(TrajectoryAnomalyService service) { this.service = service; }

    @GetMapping("/trajectory/playback/anomalies")
    @Operation(summary = "按作业单识别疑似漏采、归档断点、停留、定位跳点、推断超速及疑似转场")
    public AjaxResult analyze(
            @RequestParam String trajectoryId, @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long userId = getUserId();
        return success(service.analyze(new Query(type, trajectoryId, null, startTime, endTime),
                new Actor(userId, getUsername(), SecurityUtils.isAdmin(userId) || SecurityUtils.hasRole("admin"))));
    }
}
