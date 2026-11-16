package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.FarmWorkCrop;
import com.agri.cms.system.mapper.FarmWorkCropMapper;
import com.agri.cms.system.service.FarmWorkCropService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class FarmWorkCropServiceImpl extends ServiceImpl<FarmWorkCropMapper, FarmWorkCrop> implements FarmWorkCropService {
}
