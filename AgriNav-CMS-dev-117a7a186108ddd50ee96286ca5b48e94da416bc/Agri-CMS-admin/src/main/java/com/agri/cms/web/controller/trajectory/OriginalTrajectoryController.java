package com.agri.cms.web.controller.trajectory;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.service.IOriginalTrajectoryService;
import com.agri.cms.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author mfr
 * @since 2024/9/24
 */
@Tag(name = "原始轨迹")
@RestController
@RequestMapping("/originalTrajectory")

public class OriginalTrajectoryController extends BaseController {

    @Autowired
    private IOriginalTrajectoryService originalTrajectoryService;

    /**
     * 查询原始轨迹
     * @return AjaxResult
     * @author mfr
     * @since 2024/9/27
     */
    @RequestMapping("/queryOriginalTrajectory")
    @Operation(summary = "查询原始轨迹")
    @PreAuthorize("@ss.hasPermi('trajectory:original:search')")
    public AjaxResult queryOriginalTrajectory(@RequestBody SortMessage sortMessage) {
        // 获取当前用户手机号放入查询条件中
        String phone = getLoginUser().getUsername();
        sortMessage.setPhone(phone);
        List<Point> points = originalTrajectoryService.queryOriginalPoint(sortMessage);
        return AjaxResult.success(points);
    }

    /**
     * 根据手机号时间查询原始轨迹
     * @return AjaxResult
     * @author mfr
     * @since 2024/9/27
     */
    @RequestMapping("/queryOriginalTrajectoryByPhoneAndTime")
    @Operation(summary = "根据手机号时间查询原始轨迹")
    @PreAuthorize("@ss.hasPermi('trajectory:original:search')")
    public AjaxResult queryOriginalTrajectoryByPhone(@RequestParam String phone,
                                                     @RequestParam LocalDateTime startTime,
                                                     @RequestParam LocalDateTime endTime) {
        // 判空，如果为空则返回错误信息
        if (phone == null || startTime == null || endTime == null) {
            if (phone == null) {
                return AjaxResult.warn("手机号为空");
            }
            if (startTime == null) {
                return AjaxResult.warn("开始时间为空");
            }
            return AjaxResult.warn("结束时间为空");
        }

        if (!phone.equals(getUsername()) && !SecurityUtils.isAdmin(getUserId()) && !SecurityUtils.hasRole("admin")) {
            return AjaxResult.error(403,"无权查询其他终端的轨迹");
        }
        if(startTime.isAfter(endTime))return AjaxResult.error("开始时间不能晚于结束时间");
        List<Point> points = originalTrajectoryService.queryOriginalPointByPhoneAndTime(phone, startTime, endTime);


        return AjaxResult.success(points);
    }


    @Operation(summary = "删除用户轨迹")
    @DeleteMapping("/deleteByUserName")
    @PreAuthorize("@ss.hasPermi('trajectory:original:delete')")
    public AjaxResult deleteByUserName(@RequestParam String phone) {
        boolean result = originalTrajectoryService.deleteByUserName(phone);
        if (result) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.warn("删除失败");
        }
    }

}
