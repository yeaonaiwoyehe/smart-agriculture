package com.agri.cms.web.controller.trajectory;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.service.playback.TrajectoryPlaybackService;
import com.agri.cms.system.service.playback.PlaybackModels.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/trajectory/playback")
@Tag(name="原始与有效轨迹回放")
public class TrajectoryPlaybackController extends BaseController {
    private final TrajectoryPlaybackService service;
    public TrajectoryPlaybackController(TrajectoryPlaybackService service) {this.service=service;}
    @GetMapping
    @Operation(summary="只读回放：ORIGINAL原始点或EFFECTIVE已保存有效轨迹；支持作业单或终端与时间段")
    public AjaxResult query(@RequestParam String type,@RequestParam(required=false) String trajectoryId,
                            @RequestParam(required=false) String phone,
                            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return success(service.query(new Query(type,trajectoryId,phone,startTime,endTime),
                new Actor(getUserId(),getUsername(),SecurityUtils.isAdmin(getUserId())||SecurityUtils.hasRole("admin"))));
    }
}
