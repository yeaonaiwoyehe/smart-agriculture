package com.agri.cms.web.controller.trajectory;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.service.impl.BoundaryClippingService;
import com.agri.cms.system.service.playback.TrajectoryAnomalyDemo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "agri.demo.trajectory-enabled", havingValue = "true")
@Tag(name = "B-11 轨迹异常识别演示")
public class TrajectoryAnomalyDemoController {
    private final TrajectoryAnomalyDemo demo;

    public TrajectoryAnomalyDemoController(BoundaryClippingService clipping) {
        demo = new TrajectoryAnomalyDemo(clipping);
    }

    @GetMapping("/trajectory/playback/anomalies/demo")
    @Operation(summary = "用合成轨迹演示 B-11，只需选择 ORIGINAL 或 EFFECTIVE")
    public AjaxResult demo(@RequestParam(defaultValue = "ORIGINAL") String type) {
        return AjaxResult.success(demo.analyze(type));
    }

    @GetMapping("/trajectory/playback/anomalies/demo/workorder")
    @Operation(summary = "合成演示作业单、完整轨迹和 B-10 面积核算")
    public AjaxResult workorder() { return AjaxResult.success(demo.workorder()); }
}
