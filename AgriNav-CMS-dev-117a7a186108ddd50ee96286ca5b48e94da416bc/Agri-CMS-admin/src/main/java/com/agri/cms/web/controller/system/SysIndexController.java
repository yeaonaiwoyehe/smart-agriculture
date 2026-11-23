package com.agri.cms.web.controller.system;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.agri.cms.common.config.GlobalConfig;
import com.agri.cms.common.utils.StringUtils;

/**
 * 首页
 *
 * @author ruoyi
 */
@RestController
@Tag(name = "系统首页")
public class SysIndexController {
    /**
     * 系统基础配置
     */
    @Autowired
    private GlobalConfig globalConfig;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index() {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", globalConfig.getName(), globalConfig.getVersion());
    }
}
