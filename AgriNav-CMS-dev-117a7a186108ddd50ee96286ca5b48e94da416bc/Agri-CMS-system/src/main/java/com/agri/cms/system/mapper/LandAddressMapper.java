package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.LandAddress;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 李鸿麟
 */

@DS("pg")
public interface LandAddressMapper extends BaseMapper<LandAddress> {
}
