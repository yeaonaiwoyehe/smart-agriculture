package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.EffectivePointBuffer;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 实时有效作业点暂存表 Mapper
 *
 * @author AgriNav
 */
@DS("pg")
public interface EffectivePointBufferMapper extends BaseMapper<EffectivePointBuffer> {

}
