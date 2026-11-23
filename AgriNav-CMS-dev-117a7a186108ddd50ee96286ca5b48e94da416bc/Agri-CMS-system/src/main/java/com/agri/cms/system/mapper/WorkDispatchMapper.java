package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.WorkDispatch;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 作业调度派单 Mapper（A-19，MP-02）。
 *
 * @author agrinav
 */
@DS("pg")
public interface WorkDispatchMapper extends BaseMapper<WorkDispatch> {
}
