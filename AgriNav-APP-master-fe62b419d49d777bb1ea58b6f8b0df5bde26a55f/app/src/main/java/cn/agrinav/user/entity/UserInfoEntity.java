package cn.agrinav.user.entity;

/**
 * @author 11230
 */
public class UserInfoEntity {
    private String msg;
    private int code;
    private SysUser user;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public SysUser getUser() {
        return user;
    }

    public void setUser(SysUser user) {
        this.user = user;
    }
}
