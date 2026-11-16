package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.Boundary;
import com.agri.cms.system.mapper.BoundaryMapper;
import com.agri.cms.system.service.BoundaryService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-26
 * @Description:
 * @Version: 1.0
 */
@Service
@DS("pg")
public class BoundaryServiceImpl extends ServiceImpl<BoundaryMapper, Boundary> implements BoundaryService {
}
