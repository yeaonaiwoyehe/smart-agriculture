package com.agri.cms.ai.agent;

import com.agri.cms.ai.agent.mapper.AgentTaskClaimMapper;
import com.agri.cms.ai.tool.ReceiptGenerator;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.system.domain.FarmWork;
import com.agri.cms.system.mapper.FarmWorkInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务认领服务（A-11，AG-01 任务助手）。
 *
 * 负责认领前的完整校验与认领记录落库：
 * 农活存在 → 状态可认领 → 未过期 → 未被他人认领 → 生成回执并落表。
 * 同一用户重复认领同一农活时幂等返回原回执，不产生重复记录。
 *
 * @author agrinav
 */
@Service
public class TaskClaimService {

    private static final Logger log = LoggerFactory.getLogger(TaskClaimService.class);

    private final FarmWorkInfoMapper farmWorkMapper;
    private final AgentTaskClaimMapper claimMapper;
    private final ReceiptGenerator receiptGenerator;

    public TaskClaimService(FarmWorkInfoMapper farmWorkMapper,
                            AgentTaskClaimMapper claimMapper,
                            ReceiptGenerator receiptGenerator) {
        this.farmWorkMapper = farmWorkMapper;
        this.claimMapper = claimMapper;
        this.receiptGenerator = receiptGenerator;
    }

    /**
     * 认领指定农活。
     *
     * @param farmWorkId 农活 ID
     * @param userId     认领用户 ID
     * @param userName   登录账号（手机号）
     * @param nickName   用户昵称（可为空）
     * @return 认领结果
     */
    public ClaimResult claim(String farmWorkId, String userId, String userName, String nickName) {
        if (farmWorkId == null || farmWorkId.isBlank()) {
            return fail(farmWorkId, null, "缺少要认领的农活编号，请告诉我具体任务。");
        }

        // 1. 农活是否存在
        FarmWork work = farmWorkMapper.selectById(farmWorkId);
        if (work == null) {
            return fail(farmWorkId, null, "未找到编号为 " + farmWorkId + " 的任务，请核对后再试。");
        }

        // 2. 状态是否可认领（status=0）
        if (work.getStatus() == null || work.getStatus() != Constants.AVAILABLE) {
            return fail(farmWorkId, work.getName(), "该任务当前不在可认领状态，可能已结束或被下架。");
        }

        // 3. 是否过期
        if (work.getEndTime() != null && work.getEndTime().isBefore(LocalDateTime.now())) {
            return fail(farmWorkId, work.getName(), "该任务已过报名截止时间，无法认领。");
        }

        // 4. 查该农活已生效的认领记录
        List<AgentTaskClaim> existing = claimMapper.selectList(
                new LambdaQueryWrapper<AgentTaskClaim>()
                        .eq(AgentTaskClaim::getFarmWorkId, farmWorkId)
                        .eq(AgentTaskClaim::getStatus, TaskQueryService.ClaimStatus.CLAIMED.code()));

        if (!existing.isEmpty()) {
            AgentTaskClaim first = existing.get(0);
            boolean mine = userId != null && userId.equals(String.valueOf(first.getUserId()));
            if (mine) {
                // 幂等：本人已认领，返回原回执
                log.info("[A-11] 用户 {} 重复认领农活 {}，幂等返回回执 {}", userId, farmWorkId, first.getReceiptNo());
                return new ClaimResult(true,
                        "您已认领过该任务，这是您的认领回执，无需重复认领。",
                        farmWorkId, work.getName(), first.getReceiptNo(), true);
            }
            // 已被他人认领
            return fail(farmWorkId, work.getName(), "很抱歉，该任务已被其他农机手认领，您可以看看其他可认领任务。");
        }

        // 5. 生成回执并落库
        String receiptNo = receiptGenerator.next();
        AgentTaskClaim claim = new AgentTaskClaim();
        claim.setFarmWorkId(farmWorkId);
        claim.setUserId(userId);
        claim.setUserName(userName);
        claim.setNickName(nickName);
        claim.setReceiptNo(receiptNo);
        claim.setStatus(TaskQueryService.ClaimStatus.CLAIMED.code());
        claim.setCreateTime(LocalDateTime.now());
        claimMapper.insert(claim);

        log.info("[A-11] 用户 {} 成功认领农活 {}，回执 {}", userId, farmWorkId, receiptNo);
        return new ClaimResult(true,
                "认领成功！请按约定时间前往作业，可凭此回执核对任务。",
                farmWorkId, work.getName(), receiptNo, false);
    }

    private ClaimResult fail(String farmWorkId, String workName, String message) {
        log.info("[A-11] 认领失败 farmWorkId={} 原因={}", farmWorkId, message);
        return new ClaimResult(false, message, farmWorkId, workName, null, false);
    }
}
