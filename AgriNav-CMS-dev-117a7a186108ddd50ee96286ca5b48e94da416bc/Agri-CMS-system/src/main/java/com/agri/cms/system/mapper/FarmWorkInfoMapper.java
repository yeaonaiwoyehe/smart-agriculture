package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.FarmWork;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 姚锦伦
 */

@Mapper
@DS("pg")
public interface FarmWorkInfoMapper extends BaseMapper<FarmWork> {
}
