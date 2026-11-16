package com.agri.cms.web.controller.key;


import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.service.IEncryptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 密钥交换接口
 *
 * @author mfr
 * @since 2024/11/7
 */
@RestController
@RequestMapping("/system/key")
public class KeyExchangeController {

    @Autowired
    private IEncryptService encryptService;

    /**
     * 密钥交换接口
     *
     * @param encryptedAesKey RSA加密后的AES密钥
     */
    @PostMapping("/exchange")
    public AjaxResult exchangeKey(@RequestBody String encryptedAesKey, @RequestHeader("source") String source) {
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getUserId();

            // 保存AES密钥
            boolean success = encryptService.saveAESKey(userId, encryptedAesKey, source);

            if (success) {
                return AjaxResult.success("密钥交换成功");
            } else {
                return AjaxResult.error("密钥交换失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("密钥交换失败：" + e.getMessage());
        }
    }

    //获取公钥
    @GetMapping("/public")
    public AjaxResult getPublicKey() {
        String data = encryptService.getRSAPublicKey();
        AjaxResult success = AjaxResult.success();
        success.put("data",data);
        return success;
    }
} 