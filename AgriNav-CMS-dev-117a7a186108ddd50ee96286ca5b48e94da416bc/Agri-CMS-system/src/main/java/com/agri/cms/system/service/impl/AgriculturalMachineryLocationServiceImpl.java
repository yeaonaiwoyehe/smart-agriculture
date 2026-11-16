package com.agri.cms.system.service.impl;


import com.agri.cms.system.domain.AgriculturalMachineryLocation;
import com.agri.cms.system.mapper.AgriculturalMachineryLocationMapper;
import com.agri.cms.system.service.AgriculturalMachineryLocationService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class AgriculturalMachineryLocationServiceImpl extends ServiceImpl<AgriculturalMachineryLocationMapper, AgriculturalMachineryLocation> implements AgriculturalMachineryLocationService {
}
