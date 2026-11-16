package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.dto.WorkingLandPlotDTO;

import java.util.List;

/**
 * @author 姚锦伦
 */
public interface WorkingLandPlotService {
    AjaxResult queryWorkingLandPlot(int pageNum, int pageSize, String address);

    AjaxResult queryWorkingTrajectoryOfLandPlot(WorkingLandPlotDTO landPlotId);
}
