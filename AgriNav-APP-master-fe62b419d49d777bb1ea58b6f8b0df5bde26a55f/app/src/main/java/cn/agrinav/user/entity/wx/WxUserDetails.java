package cn.agrinav.user.entity.wx;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class WxUserDetails {
    private String uuid;
    private String username;
    private String nickname;
    private String avatar;
    private String blog;
    private String company;
    private String location;
    private String email;
    private String remark;
    private String gender;
    private String source;
    private WxToken token;
    private WxRawUserInfo rawUserInfo;
    private boolean snapshotUser;

}
