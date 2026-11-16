package com.agri.cms.system.service;

import com.agri.cms.system.domain.FieldWork;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IFieldWorkService extends IService<FieldWork> {
    String getWorkTypeNames(Long workTypeId);
}
