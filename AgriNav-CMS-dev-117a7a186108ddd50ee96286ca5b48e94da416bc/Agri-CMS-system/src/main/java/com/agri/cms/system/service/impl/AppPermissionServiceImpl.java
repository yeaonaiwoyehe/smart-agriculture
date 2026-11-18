package com.agri.cms.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.entity.SysRole;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.AppPermission;
import com.agri.cms.system.domain.vo.AppPermissionVO;
import com.agri.cms.system.mapper.AppPermissionMapper;
import com.agri.cms.system.service.IAppPermissionService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 李鸿麟
 */
@Service
@DS("pg")
public class AppPermissionServiceImpl extends ServiceImpl<AppPermissionMapper, AppPermission> implements IAppPermissionService {

    @Resource
    private AppPermissionMapper appPermissionMapper;

    @Override
    public List<AppPermissionVO> checkPermission() {
        List<AppPermission> appPermissions = getAppPermissionList();

        List<AppPermissionVO> appPermissionVOS = new ArrayList<>();
        if (CollUtil.isEmpty(appPermissions)) {
            throw new ServiceException("当前表中无可用组件，请检查数据库");
        }
        appPermissions.forEach(appPermission -> {
            AppPermissionVO appPermissionVO = new AppPermissionVO();
            appPermissionVO.setComponentStr(appPermission.getComponentStr());
            appPermissionVO.setVisible(!appPermission.getIsVisible().equals(Constants.INVISIBLE));
            appPermissionVOS.add(appPermissionVO);
        });
        return appPermissionVOS;
    }

    private List<AppPermission> getAppPermissionList() {
        LambdaQueryWrapper<AppPermission> wrapper= new LambdaQueryWrapper<>();
        //wrapper.ne(AppPermission::getIsDeleted, Constants.DELETED);
        List<AppPermission> appPermissions = appPermissionMapper.selectList(wrapper);
        return appPermissions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePermission(AppPermission permission) {
        if (ObjUtil.isEmpty(permission)) {
            throw new IllegalArgumentException("传入参数为空");
        }

        if (permission.getComponentId() == null) {
            throw new IllegalArgumentException("传入组件id为空");
        }

        // 直接根据 componentId 更新，无需先获取全部列表
        int res = appPermissionMapper.updateById(permission);


        if (res == 0) {
            throw new ServiceException("未找到匹配的组件记录");
        }

        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addPermission(AppPermission permission) {
        if (ObjUtil.isEmpty(permission)) {
            throw new IllegalArgumentException("传入参数为空");
        }

        // 检查必要字段
        if (StrUtil.isBlank(permission.getPermissionName()) || StrUtil.isBlank(permission.getComponentStr())) {
            throw new IllegalArgumentException("必要参数不能为空");
        }

        // 检查是否已存在相同的 componentId 或 componentStr
        AppPermission existingPermission = appPermissionMapper.selectOne(
                new LambdaQueryWrapper<AppPermission>()
                        .eq(AppPermission::getComponentId, permission.getComponentId())
                        .or()
                        .eq(AppPermission::getComponentStr, permission.getComponentStr())
                        .eq(AppPermission::getIsDeleted, Constants.NOT_DELETED)
        );

        if (existingPermission != null) {
            throw new ServiceException("已存在相同的组件");
        }

        // 设置默认值
        if (permission.getParentId() == null) {
            permission.setParentId(0);
        }
        if (permission.getIsVisible() == null) {
            permission.setIsVisible(0);
        }
        permission.setIsDeleted(0);

        // 执行插入操作
        int result = appPermissionMapper.insert(permission);

        if (result == 0) {
            throw new ServiceException("插入组件记录失败");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePermission(AppPermission permission) {
        if (ObjUtil.isEmpty(permission) || permission.getComponentId() == null) {
            throw new IllegalArgumentException("传入参数无效或 componentId 为空");
        }

        // 检查权限是否存在且未被删除
        AppPermission existingPermission = appPermissionMapper.selectOne(
                new LambdaQueryWrapper<AppPermission>()
                        .eq(AppPermission::getComponentId, permission.getComponentId())
                        .eq(AppPermission::getIsDeleted, 0)
        );

        if (existingPermission == null) {
            throw new ServiceException("指定的权限不存在或已被删除");
        }

        // 检查是否有子权限
        List<AppPermission> childPermissions = appPermissionMapper.selectList(
                new LambdaQueryWrapper<AppPermission>()
                        .eq(AppPermission::getParentId, permission.getComponentId())
                        .eq(AppPermission::getIsDeleted, 0)
        );

        if (!childPermissions.isEmpty()) {
            throw new ServiceException("该组件存在子组件，无法删除");
        }

        // 执行逻辑删除
        AppPermission updatePermission = new AppPermission();
        updatePermission.setComponentId(permission.getComponentId());
        updatePermission.setIsDeleted(Constants.DELETED);

        int result = appPermissionMapper.updateById(updatePermission);

        if (result == 0) {
            throw new ServiceException("删除组件失败");
        }

        return result;
    }

    @Override
    public AppPermission getComponentById(Long id) {
        if (StrUtil.isEmpty(id.toString())) {
            throw new ServiceException("传入参数为空");
        }

        return appPermissionMapper.selectById(id);
    }
}
