package com.agri.cms.system.service.impl;


import com.agri.cms.system.domain.AgriculturalMachineryCrop;
import com.agri.cms.system.mapper.AgriculturalMachineryCropMapper;
import com.agri.cms.system.service.AgriculturalMachineryCropService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class AgriculturalMachineryCropServiceImpl extends ServiceImpl<AgriculturalMachineryCropMapper, AgriculturalMachineryCrop> implements AgriculturalMachineryCropService {
}
