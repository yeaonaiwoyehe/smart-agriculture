package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.dto.AgriculturalMachineryDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 姚锦伦
 */
public interface AgriculturalMachineryService {
    /**
     * 农机找活插入
     *
     * @author 姚锦伦
     * @since 2024/10/18
     */
    void insert(AgriculturalMachineryDTO agriculturalMachineryDTO, String phoneNumber);

    void delete(String id);

    void update(AgriculturalMachineryDTO agriculturalMachineryDTO, String phoneNumber);

    AjaxResult list(AgriculturalMachineryDTO agriculturalMachineryDTO, String userId, int pageNum, int pageSize);

    void checkEffect();

    void deleteByUsername(String username);

    AjaxResult add(String agriculturalMachineryDTO, List<MultipartFile> image, String userId);

    AjaxResult updateAgriculturalMachinery(String agriculturalMachineryDTO, List<MultipartFile> image, String userId);
}
