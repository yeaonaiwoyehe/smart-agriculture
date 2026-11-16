package com.agri.cms.system.service;


import com.agri.cms.system.domain.AppPermission;
import com.agri.cms.system.domain.vo.AppPermissionVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 李鸿麟 
 * app权限服务类接口
 */
@Service
public interface IAppPermissionService extends IService<AppPermission> {
     List<AppPermissionVO> checkPermission();

     int updatePermission(AppPermission permission);

     int addPermission(AppPermission permission);

     int deletePermission(AppPermission permission);

    AppPermission getComponentById(Long id);
}
