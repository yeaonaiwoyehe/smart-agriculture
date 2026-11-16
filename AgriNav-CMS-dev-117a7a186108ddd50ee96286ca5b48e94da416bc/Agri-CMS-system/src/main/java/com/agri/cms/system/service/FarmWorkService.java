package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.dto.FarmWorkDTO;
import org.springframework.stereotype.Service;


/**
 * @author 姚锦伦
 */
public interface FarmWorkService {
    void insert(FarmWorkDTO farmWorkDTO, String userId);

    void deleteById(String id);

    void update(FarmWorkDTO farmWorkDTO, String username);

    AjaxResult list(FarmWorkDTO farmWorkDTO, String userId, int pageNum, int pageSize);

    void checkEffect();

    void deleteByUsername(String username);
}
