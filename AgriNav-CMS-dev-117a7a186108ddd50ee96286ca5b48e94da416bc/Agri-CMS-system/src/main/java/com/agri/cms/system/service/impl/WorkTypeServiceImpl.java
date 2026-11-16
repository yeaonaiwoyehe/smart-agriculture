package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.WorkType;
import com.agri.cms.system.mapper.WorkTypeMapper;
import com.agri.cms.system.service.WorkTypeService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 姚锦伦
 */
@Service
@DS("pg")
public class WorkTypeServiceImpl extends ServiceImpl<WorkTypeMapper, WorkType> implements WorkTypeService {
    @Autowired
    private WorkTypeMapper workTypeMapper;

    @Override
    public String getNameById(Long id) {
        return workTypeMapper.selectById(id).getWorkName();
    }
}
