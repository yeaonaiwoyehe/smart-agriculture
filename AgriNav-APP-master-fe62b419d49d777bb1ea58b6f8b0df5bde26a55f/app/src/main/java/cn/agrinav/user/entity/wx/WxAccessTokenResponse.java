package cn.agrinav.user.entity.wx;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class WxAccessTokenResponse {
    @SerializedName("errcode")
    private Integer errCode;
    @SerializedName("errmsg")
    private String errMsg;
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("expires_in")
    private Integer expiresIn;
    @SerializedName("refresh_token")
    private String refreshToken;
    private String openid;
    private String scope;
    private String unionid;
}
