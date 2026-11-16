package cn.agrinav.user.entity;

/**
 * @author 18506
 */
public class SysUser {
    private Long userId;
    /** 用户昵称 */
    private String nickName;
    /** 用户邮箱 */
    private String email;
    /** 手机号码 */
    private String phonenumber;
    /** 用户性别 */
    private String sex;
    /** 用户头像 */
    private String avatar;
    /**用户部门*/
    private Long deptId;
    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getUserId() {
        return userId;
    }
    public String getNickName() {
        return nickName;
    }
    public String getEmail() {
        return email;
    }
    public String getPhonenumber() {
        return phonenumber;
    }
    public String getSex() {
        return sex;
    }
    public String getAvatar() {
        return avatar;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }
    public void setSex(String sex) {
        this.sex = sex;
    }
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
