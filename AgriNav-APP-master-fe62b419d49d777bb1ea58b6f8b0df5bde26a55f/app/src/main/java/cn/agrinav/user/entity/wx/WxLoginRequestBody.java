package cn.agrinav.user.entity.wx;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class WxLoginRequestBody {
    private String state;
    private String code;
}
