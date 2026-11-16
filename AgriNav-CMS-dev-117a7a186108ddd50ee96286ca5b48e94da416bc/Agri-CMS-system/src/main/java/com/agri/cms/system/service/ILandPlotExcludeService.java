package com.agri.cms.system.service;

import com.agri.cms.system.domain.LandPlotExclude;
import com.agri.cms.system.domain.vo.LandPlotExcludeVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface ILandPlotExcludeService extends IService<LandPlotExclude> {
    void addLandPlotExclude(LandPlotExclude landPlotExclude);

    List<LandPlotExclude> queryByLandPlotId(Long landPlotId);

    //逻辑删除
    void deleteLandPlotExclude(Long LandPlotId);

    List<LandPlotExcludeVO> queryBatch(List<Long> landPlotIds);

    void batchSave(List<String> landPlotExcludes, Long id);
}
