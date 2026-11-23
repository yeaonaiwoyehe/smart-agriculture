package com.agri.cms.ai.agent;

import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.TrajectoryInfoMapper;
import org.springframework.stereotype.Component;

/**
 * 数据归属校验器（A-09，数据范围过滤）。
 *
 * 落实"仅本人数据"范围：管理员可访问全部作业单；普通用户只能访问本人创建
 * 或绑定手机号的作业单。工具在按单号取数前调用本组件，越权访问直接拒绝。
 *
 * @author agrinav
 */
@Component
public class DataOwnershipChecker {

    private final TrajectoryInfoMapper trajectoryInfoMapper;

    public DataOwnershipChecker(TrajectoryInfoMapper trajectoryInfoMapper) {
        this.trajectoryInfoMapper = trajectoryInfoMapper;
    }

    /**
     * 当前登录用户是否为管理员。
     */
    public boolean isCurrentUserAdmin() {
        return SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    /**
     * 当前用户能否访问指定作业单。
     *
     * 管理员直接放行；普通用户仅当作业单创建人或绑定手机号为本人时放行。
     * 作业单不存在时返回 true，由具体查询逻辑返回"不存在"，不在这里暴露存在性。
     */
    public boolean canAccessWorkOrder(String workOrderId) {
        if (isCurrentUserAdmin()) {
            return true;
        }
        TrajectoryInfo info = trajectoryInfoMapper.selectById(workOrderId);
        if (info == null) {
            return true;
        }
        Long currentUserId = SecurityUtils.getUserId();
        String currentUsername = SecurityUtils.getUsername();

        boolean ownedByUserId = currentUserId != null
                && String.valueOf(currentUserId).equals(info.getCreatedBy());
        boolean ownedByPhone = currentUsername != null
                && currentUsername.equals(info.getPhoneNumber());
        return ownedByUserId || ownedByPhone;
    }
}
