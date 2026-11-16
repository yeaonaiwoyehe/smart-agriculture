package cn.agrinav.user.entity.wx;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class WxToken {
    private String accessToken;
    private int expireIn;
    private String refreshToken;
    private int refreshTokenExpireIn;
    private String uid;
    private String openId;
    private String accessCode;
    private String unionId;
    private String scope;
    private String tokenType;
    private String idToken;
    private String macAlgorithm;
    private String macKey;
    private String code;
    private boolean snapshotUser;
    private String oauthToken;
    private String oauthTokenSecret;
    private String userId;
    private String screenName;
    private Boolean oauthCallbackConfirmed;
}
