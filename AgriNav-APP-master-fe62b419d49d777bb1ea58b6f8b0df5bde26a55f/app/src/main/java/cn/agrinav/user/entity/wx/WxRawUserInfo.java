package cn.agrinav.user.entity.wx;

import java.util.List;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class WxRawUserInfo {
    private String country;
    private String unionid;
    private String province;
    private String city;
    private String openid;
    private int sex;
    private String nickname;
    private String headimgurl;
    private String language;
    private List<String> privilege;
}
