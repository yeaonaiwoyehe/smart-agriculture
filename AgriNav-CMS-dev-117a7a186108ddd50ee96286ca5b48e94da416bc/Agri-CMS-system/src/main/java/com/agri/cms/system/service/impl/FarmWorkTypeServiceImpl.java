package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.FarmWorkType;
import com.agri.cms.system.mapper.FarmWorkTypeMapper;
import com.agri.cms.system.service.FarmWorkTypeService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class FarmWorkTypeServiceImpl extends ServiceImpl<FarmWorkTypeMapper, FarmWorkType> implements FarmWorkTypeService {
}
