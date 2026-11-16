package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.FarmWorkLandPlot;
import com.agri.cms.system.mapper.FarmWorkLandPlotMapper;
import com.agri.cms.system.service.FarmWorkLandPlotService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class FarmWorkLandPlotServiceImpl extends ServiceImpl<FarmWorkLandPlotMapper, FarmWorkLandPlot> implements FarmWorkLandPlotService {
}
