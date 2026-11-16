package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.AgriculturalMachineryType;
import com.agri.cms.system.mapper.AgriculturalMachineryTypeMapper;
import com.agri.cms.system.service.AgriculturalMachineryTypeService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class AgriculturalMachineryTypeServiceImpl extends ServiceImpl<AgriculturalMachineryTypeMapper, AgriculturalMachineryType> implements AgriculturalMachineryTypeService {
}
