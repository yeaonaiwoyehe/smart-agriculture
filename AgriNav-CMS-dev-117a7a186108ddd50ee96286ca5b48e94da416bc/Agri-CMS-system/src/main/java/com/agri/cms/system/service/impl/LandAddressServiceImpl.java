package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.LandAddress;
import com.agri.cms.system.mapper.LandAddressMapper;
import com.agri.cms.system.service.ILandAddressService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@DS("pg")
@Service
public class LandAddressServiceImpl extends ServiceImpl<LandAddressMapper, LandAddress> implements ILandAddressService {

}
