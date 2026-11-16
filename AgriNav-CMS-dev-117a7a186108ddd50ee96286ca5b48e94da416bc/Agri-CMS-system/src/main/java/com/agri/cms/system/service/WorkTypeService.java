package com.agri.cms.system.service;

import com.agri.cms.system.domain.WorkType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 姚锦伦
 */
public interface WorkTypeService extends IService<WorkType> {
    String getNameById(Long id);

}
