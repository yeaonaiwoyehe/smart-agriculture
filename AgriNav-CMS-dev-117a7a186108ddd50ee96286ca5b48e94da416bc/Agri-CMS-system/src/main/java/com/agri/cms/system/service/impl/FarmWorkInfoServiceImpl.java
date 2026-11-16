package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.FarmWork;
import com.agri.cms.system.mapper.FarmWorkInfoMapper;
import com.agri.cms.system.service.FarmWorkInfoService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class FarmWorkInfoServiceImpl extends ServiceImpl<FarmWorkInfoMapper, FarmWork> implements FarmWorkInfoService {
}
