package com.agri.cms.web.controller.system;

import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.annotation.Anonymous;
import com.agri.cms.common.annotation.AsyncNotification;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.domain.entity.SysDictData;
import com.agri.cms.common.core.domain.model.NotificationType;
import com.agri.cms.common.utils.DictUtils;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.domain.Feedback;
import com.agri.cms.system.domain.SysFeedbackLabel;
import com.agri.cms.system.domain.dto.FeedbackDTO;
import com.agri.cms.system.service.ISysFeedbackService;
import com.agri.cms.system.service.SysFeedbackLabelService;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @author simon
 * @since 2024/10/4
 */
@RestController
@Anonymous
@RequestMapping("/system/feedback")
@Tag(name = "用户反馈管理")
public class SysFeedbackController extends BaseController {
    @Resource
    private ISysFeedbackService iSysFeedbackService;
    @Resource
    private SysFeedbackLabelService sysFeedbackLabelService;

    /**
     * 提交反馈信息
     *
     * @return AjaxResult
     * @author simon
     * @since 2024/10/4
     */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "提交反馈信息")
    public AjaxResult commitFeedback(@RequestBody Feedback feedback) {
        String uuid = IdWorker.get32UUID();
        feedback.setId(uuid)
                .setIsChecked(Constants.NOT_CHECKED)
                .setPhoneNumber(getLoginUser().getUser().getPhonenumber())
                .setUsername(getLoginUser().getUser().getNickName())
                .setCommitTime(LocalDateTime.now());
        if (ObjectUtil.isNotEmpty(feedback.getLabels())){
            feedback.getLabels()
                    .stream()
                    .map(SysDictData::getDictCode)
                    .map(code->new SysFeedbackLabel(uuid, code))
                    .forEach(sysFeedbackLabelService::save);
        }
        return AjaxResult.success(iSysFeedbackService.save(feedback) ? "提交成功" : "提交失败");
    }
    @GetMapping("/myFeedback")
    @Operation(summary = "获取我的反馈")
    public AjaxResult getMyFeedback(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize){
        //获取用户名
        String username = getLoginUser().getUsername();
        return iSysFeedbackService.getMyFeedback(pageNum, pageSize, username);
    }

    @PostMapping("/list")
    @Operation(summary = "获取反馈列表")
    public AjaxResult getFeedbackList(
            Page<Feedback> page,
            @RequestBody FeedbackDTO feedback) {
        return iSysFeedbackService.queryData(page, feedback);
    }
    @PostMapping("/check")
    @AsyncNotification(
            type = NotificationType.FEEDBACK_REPLY,
            receiver = "#feedback.phoneNumber",
            relationIdentity = "#feedback.id"
    )
    @Operation(summary = "确认反馈")
    public AjaxResult checkFeedback(@RequestBody Feedback feedback
            ,@NotBlank(message = "反馈回复不能为空") String reply) {
        return success(iSysFeedbackService
                .updateById(feedback
                        .setIsChecked(Constants.CHECKED)
                        .setCheckedBy(SecurityUtils.getUsername())
                        .setCheckedTime(LocalDateTime.now())
                        .setReply(reply)));
    }
    @GetMapping("/labels")
    public AjaxResult getLabels() {
        return AjaxResult.success(DictUtils.getDictLabels(Constants.FEEDBACK_LABEL));
    }
}
