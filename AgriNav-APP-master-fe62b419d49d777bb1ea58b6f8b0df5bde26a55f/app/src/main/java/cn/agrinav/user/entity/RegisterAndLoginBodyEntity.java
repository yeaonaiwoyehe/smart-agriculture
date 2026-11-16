package cn.agrinav.user.entity;

/**
 * @author 11230
 */
public class RegisterAndLoginBodyEntity {
    private int code;
    private String msg;
    private String token;
    private String rsa_public_key;
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRsa_public_key() {
        return rsa_public_key;
    }

    public void setRsa_public_key(String rsa_public_key) {
        this.rsa_public_key = rsa_public_key;
    }

    @Override
    public String toString() {
        return "RegisterAndLoginBodyEntity{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", token='" + token + '\'' +
                ", rsa_public_key='" + rsa_public_key + '\'' +
                '}';
    }
}
