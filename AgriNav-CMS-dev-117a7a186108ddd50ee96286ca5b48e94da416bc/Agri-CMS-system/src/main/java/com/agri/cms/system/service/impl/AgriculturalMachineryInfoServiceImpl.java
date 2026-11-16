package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.AgriculturalMachinery;
import com.agri.cms.system.mapper.AgriculturalMachineryInfoMapper;
import com.agri.cms.system.service.AgriculturalMachineryInfoService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class AgriculturalMachineryInfoServiceImpl extends ServiceImpl<AgriculturalMachineryInfoMapper, AgriculturalMachinery> implements AgriculturalMachineryInfoService {
}
