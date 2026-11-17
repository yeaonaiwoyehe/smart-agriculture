package com.agri.cms.web.controller.field;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.exception.LandPlotException;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.system.domain.LandPlotExclude;
import com.agri.cms.system.domain.vo.LandPlotExcludeVO;
import com.agri.cms.system.service.ILandPlotExcludeService;
import com.baomidou.dynamic.datasource.annotation.DS;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 无用区域接口
 * @author mfr
 * @since 2024/11/25 */
@DS("pg")
@RestController
@RequestMapping("/landPlotExclude")
@Tag(name = "插花地无用区域接口")
public class LandPlotExcludeController {
    @Resource
    private ILandPlotExcludeService landPlotExcludeService;


    //新增无用区域，接收参数为landPlotExclude
    @PostMapping("/addLandPlotExclude")
    @Operation(summary = "新增无用区域")
    @PreAuthorize("@ss.hasPermi('landplot:exclude:add')")
    public AjaxResult addLandPlotExclude(@RequestBody @Valid LandPlotExclude landPlotExclude) {
        landPlotExcludeService.addLandPlotExclude(landPlotExclude);
        return AjaxResult.success();
    }

    //通过地块id查询无用区域
    @GetMapping("/queryByLandPlotId")
    @Operation(summary = "通过地块id查询无用区域")
    // 演示放开：查询类接口对登录用户可见，写操作权限注解保留
    // @PreAuthorize("@ss.hasPermi('landplot:exclude:search')")
    public AjaxResult queryLandPlotExcludeByLandPlotId(@RequestParam Long landPlotId) {
        if (ObjectUtil.isEmpty(landPlotId)) {
            throw new LandPlotException("地块id为空");
        }
        List<LandPlotExclude> list = landPlotExcludeService.queryByLandPlotId(landPlotId);
        return AjaxResult.success(list);
    }

    @PostMapping("/batchQuery")
    @Operation(summary = "批量查询无用区域")
    @PreAuthorize("@ss.hasPermi('landplot:exclude:batch')")
    public AjaxResult batchQuery(@RequestBody List<Long> landPlotIds) {
        if (CollectionUtil.isEmpty(landPlotIds)) {
            return AjaxResult.error("地块id为空");
        }
        List<LandPlotExcludeVO> landPlotExcludeVO = landPlotExcludeService.queryBatch(landPlotIds);
        return AjaxResult.success(landPlotExcludeVO);
    }
}
