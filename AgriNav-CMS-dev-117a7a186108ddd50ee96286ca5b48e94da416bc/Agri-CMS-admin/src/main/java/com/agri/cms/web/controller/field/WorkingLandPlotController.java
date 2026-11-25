package com.agri.cms.web.controller.field;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.dto.LandPlotQueryDTO;
import com.agri.cms.system.domain.dto.WorkingLandPlotDTO;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.service.WorkingLandPlotService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * @author 姚锦伦
 */
@RestController
@RequestMapping("/workingLandPlot")
@Tag(name = "作业地块关联")
public class WorkingLandPlotController {
    @Resource
    private WorkingLandPlotService workingLandPlotService;

    /**
     * 查询所有正在作业的地块
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/12/9
     */
    @GetMapping
    public AjaxResult queryWorkingLandPlot(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = Constants.PAGE_SIZE + "") int pageSize,
                                           @RequestParam(required = false) String address){
        return workingLandPlotService.queryWorkingLandPlot(pageNum, pageSize, address);
    }
    @PostMapping("/workingTrajectory")
    public AjaxResult queryWorkingTrajectoryOfLandPlot(@RequestBody WorkingLandPlotDTO workingLandPlotDTO){
        return workingLandPlotService.queryWorkingTrajectoryOfLandPlot(workingLandPlotDTO);
    }
}
