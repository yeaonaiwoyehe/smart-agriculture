package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.AgriculturalMachineryImage;
import com.agri.cms.system.mapper.AgriculturalMachineryImageMapper;
import com.agri.cms.system.service.AgriculturalMachineryImageService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class AgriculturalMachineryImageServiceImpl extends ServiceImpl<AgriculturalMachineryImageMapper, AgriculturalMachineryImage> implements AgriculturalMachineryImageService {
}
