package cn.agrinav.user.entity.wx;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class WxLoginResultToken {
    private int code;
    private String msg;
    private String token;
    private String rsa_public_key;
    private WxUserDetails userDetails;
}
