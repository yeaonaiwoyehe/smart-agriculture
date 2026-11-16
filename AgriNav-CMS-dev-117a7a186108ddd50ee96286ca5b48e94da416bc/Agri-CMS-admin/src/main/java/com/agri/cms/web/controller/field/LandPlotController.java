package com.agri.cms.web.controller.field;

import cn.hutool.core.bean.BeanUtil;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.exception.LandPlotException;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.system.dao.LandPlotRepository;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.dto.LandPlotQueryDTO;
import com.agri.cms.system.domain.record.LandPlotRecord;
import com.agri.cms.system.domain.vo.HeatmapDataVO;
import com.agri.cms.system.domain.vo.LandPlotVO;
import com.agri.cms.system.service.LandPlotService;
import com.agri.cms.system.service.SearchEngineService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author simon
 */
@Tag(name = "地块管理")
@RestController
@RequestMapping("/landPlot")
public class LandPlotController extends BaseController {

    @Autowired
    private LandPlotService landPlotService;


    @Resource
    private LandPlotRepository landPlotRepository;



    /**
     * 查询当前用户或部门下所有地块信息
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/9/8
     */
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    @GetMapping
    @Operation(summary = "查询当前用户或部门下所有地块信息")
    public AjaxResult checkField(@RequestParam @NotNull long current,
                                 @RequestParam @NotNull long size
    ) {
        Long userId = getUserId();
        return success(landPlotService.getLandPlotByCreatorName(userId, current, size));
    }


    @PreAuthorize("@ss.hasPermi('field:square:check')")
    @GetMapping("/remeasure")
    @Operation(summary = "查询当前指定地块的复测历史信息")
    public AjaxResult findBeforeRemeasure(String id
    ) {
        return success(landPlotRepository.findByLandId(id));
    }

    /**
     * 逻辑删除，将idAvailable置0
     *
     * @param id 地块的唯一id
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/9/9
     */
    @Operation(summary = "删除地块")
    @PreAuthorize("@ss.hasPermi('field:square:delete')")
    @DeleteMapping("/{id}")
    public AjaxResult deleteById(@PathVariable @NotBlank String id) {
        if (StringUtils.isEmpty(id)) {
            return error("参数错误");
        }
        landPlotService.deleteById(id);
        return success();
    }

    /**
     * 修改地块信息
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/9/9
     */
    @PreAuthorize("@ss.hasPermi('field:square:update')")
    @Operation(summary = "修改地块信息")
    @PutMapping
    public AjaxResult updateField(
            @RequestParam("landPlot") @NotNull String json,
            @RequestParam("file") MultipartFile file
    ) {
        LandPlotVO landPlotVO = JSON.to(LandPlotVO.class, json);
        validate(landPlotVO);
        landPlotRepository.save(new LandPlotRecord(landPlotService.getById(landPlotVO.getId())));
        AjaxResult ajax = AjaxResult.success("地块修改成功");
        ajax.putAll(landPlotService.updateLandPlot(landPlotVO, file));
        return ajax;
    }


    /**
     * 查看历史删除的地块
     * @return AjaxResult
     * @author simon
     * @since 2024/12/7
     */
    @GetMapping("/history")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult findHistoryLandPlots(Page<LandPlot> page) {
        return success(landPlotService
                .getHistoryLandPlots(page,getUserId(),getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('field:square:update')")
    public AjaxResult restoreLandPlot(@PathVariable Long id) {
        landPlotService.restoreLandPlot(id);
        return success();
    }



    /**
     * 新增地块
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/11
     */
    @Operation(summary = "新增地块")
    @PreAuthorize("@ss.hasPermi('field:square:save')")
    @PostMapping
    public AjaxResult addField(
            @RequestParam("landPlot") @NotNull String json,
            @RequestParam("file") MultipartFile file
    ) {
        LandPlotVO landPlotVO = JSON.to(LandPlotVO.class, json);

        validate(landPlotVO);

        Map<String, Object> result = landPlotService.addLandPlot(landPlotVO, file);
        AjaxResult ajax = AjaxResult.success("地块插入成功");
        ajax.putAll(result);
        return ajax;
    }



    /**
     * 根据经纬度查询地块信息
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/25
     */
    @GetMapping("/byCoordinate")
    @Operation(summary = "根据经纬度查询地块信息")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult checkByCoordinate(@NotNull Double longitude,
                                        @NotNull Double latitude,
                                        @NotNull Double distance) {
        return success(landPlotService.checkByCoordinate(longitude, latitude, distance));
    }


    /**
     * 条件查询,接收地块类型，亩数大小，开始结束时间，测量方式
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/9/25
     */
    @GetMapping("/query")
    @Operation(summary = "根据条件查询地块信息")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult query(@RequestParam Map<String, Object> params) {
        // 查询
        LandPlotQueryDTO queryDTO = BeanUtil.toBean(params, LandPlotQueryDTO.class);
        return landPlotService.queryByConditions(queryDTO);
    }


    /**
     * 根据位置模糊查询加上手机号
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/25
     */
    @GetMapping("/queryByAddress")
    @Operation(summary = "根据位置模糊查询地块信息")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult queryByAddress(LandPlotQueryDTO queryDTO) {
        AjaxResult ajaxResult = landPlotService.queryByConditions(queryDTO);

        return ajaxResult;

    }


    /**
     * 根据地址查询所有地块
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/25
     */
    @GetMapping("/queryByAddressAll")
    @Operation(summary = "根据地址查询所有地块")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult queryByAddressAll(
            @RequestParam(required = false) String address
    ) {
        if (StringUtils.isBlank(address)) {
            return AjaxResult.warn("请输入地址");
        }
        QueryChainWrapper<LandPlot> like = landPlotService.query().like("address", address);
        List<LandPlot> landPlots = like.list();

        return AjaxResult.success(landPlots);
    }


    /**
     * 根据地块id查询地块信息
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/25
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据地块id查询地块信息")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult queryById(@PathVariable Long id) {
        return AjaxResult.success(landPlotService.getById(id));
    }


    /**
     * 获取地块热力图
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/25
     */
    @GetMapping("/heatmap")
    @Operation(summary = "获取地块热力图")
    @PreAuthorize("@ss.hasPermi('field:square:heatcheck')")
    public AjaxResult getHeatmapData() {
        List<HeatmapDataVO> heatmapData = landPlotService.getHeatmapData();
        return AjaxResult.success(heatmapData);
    }


    /**
     * 返回总体面积
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/25
     */
    @GetMapping("/area")
    @Operation(summary = "返回总体面积")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult getArea(@RequestParam(required = false, defaultValue = "0") Long addressCode,
                              @RequestParam(required = false, defaultValue = "1") int level) {


        AjaxResult ajax = landPlotService.getArea(addressCode, level);
        return ajax;
    }

    /**
     * 根据地址返回该地址内地块的面积之和
     *
     * @return AjaxResult
     * @author mfr
     * @since 2024/10/30
     */
    @GetMapping("/areaByAddress")
    @Operation(summary = "根据地址返回该地址内地块的面积之和")
    @PreAuthorize("@ss.hasPermi('field:square:check')")
    public AjaxResult getAreaByAddress(String address) {
        // 判断地址是否为空
        if (StringUtils.isBlank(address)) {
            throw new LandPlotException("请输入地址");
        }
        double areaByAddress = landPlotService.getAreaByAddress(address);
        return AjaxResult.success(areaByAddress);


    }

    @PreAuthorize("@ss.hasPermi('field:square:signout')")
    @DeleteMapping("signOut/{username}")
    public AjaxResult deleteByUsername(@PathVariable String username) {
        landPlotService.deleteByUsername(username);
        return AjaxResult.success();
    }

    private static void validate(LandPlotVO landPlotVO) {
        // 手动验证
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<LandPlotVO>> violations = validator.validate(landPlotVO);

        if (!violations.isEmpty()) {
            // 收集所有验证错误
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new LandPlotException(errorMessage);
        }
    }
}

