package cn.agrinav.utils.encryption;
import java.util.Arrays;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * @author 11230
 */
public class EncryptConfig {
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/login",
            "/logout",
            "/captchaImage",
            "/register",
            // 一些公共接口
            "/common",
            // 文件上传下载等接口
            "/profile"  ,
            // 密钥交换接口,请求数据本身就是已经加密过的,不需要再次加密
            "/system/key/exchange",
            "/system/user/bind",
            "/getInfo",
            "/getRouters",
            "/captcha",
            "/loginWithoutSecret",
            "/loginWechatMobile",
            "/render",
            "/loginQR",
            "/validate",
            "/system/user/profile"
    );
    /**
     * 这个字段的意义是对于login的接口，要去交换一下aes
     */
    private static List<String> LOGIN_PATH=Arrays.asList(
            "/login",
            "/loginWithoutSecret",
            "/loginWechatMobile",
            "/render"
    );

    /**
     * 检查是否需要加密
     * @param url 地址
     * @return 是否需要加密，false为不需要加密
     */
    public synchronized static boolean checkPath(String url){
        // 本地联调：后端当前全局关闭传输加密，APP 端同步关闭，避免“明文响应被当密文解密失败”而误判登录失效
        return false;
        /* ---- 原加密判定逻辑暂时保留，待后端恢复加密后重新启用 ----
        String[] split = url.split("/");
        //只有一层path直接匹配
        if (split.length==2){
            //获取路径
            String path = "/"+split[1];
            if (EXCLUDE_PATHS.contains(path)){
                     //如果包含在不需要加密的范围内
                return false;
            }
        }
        //多层path先检测是否是模糊的那俩路径：common和profile
        if (split.length>2){
            String path = "/"+split[1];
            if (path.equals(EXCLUDE_PATHS.get(4))||path.equals(EXCLUDE_PATHS.get(5))){
                return false;
            }
            //如果不是那两个，检测是否是system/key/exchange
            if (split.length==4&&split[1].equals("system")&&split[2].equals("key")&&split[3].startsWith("exchange")){
                //这个情况是不需要加密的
                return false;
            }
            if (split.length==4&&split[1].equals("system")&&split[2].equals("user")&&split[3].startsWith("bind")){
                //这个情况是不需要加密的
                return false;
            }
            if (split.length==5&&split[1].equals("system")&&split[2].equals("user")&&split[3].equals("profile")&&split[4].equals("phoneNumber")){
                //这个情况是不需要加密的
                return false;
            }
            if (split.length==5&&split[1].equals("system")&&split[2].equals("user")&&split[3].equals("profile")&&split[4].equals("avatar")){
                //这个情况是不需要加密的
                return false;
            }
        }
        return true;
        */
    }
    public synchronized static boolean checkLoginPath(String path){
        return  LOGIN_PATH.contains(path);
    }
    public synchronized static HttpUrl encryptParams(HttpUrl url,String aesKey) throws Exception{
        HttpUrl.Builder urlBuilder = url.newBuilder();
        //删除原有参数
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (String s : url.queryParameterNames()) {
            urlBuilder.removeAllQueryParameters(s);
            //不是删除原有参数而是把原有参数形成一个json字符串
            stringBuilder.append("\"").append(s).append("\":\"").append(url.queryParameter(s)).append("\",");
        }
        if (stringBuilder.length()!=1) {
            stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "");
        }
        stringBuilder.append("}");
        String encrypt = AESUtils.encrypt(stringBuilder.toString(), aesKey);
        //构造新参数
        urlBuilder.addQueryParameter("param",encrypt);
        return urlBuilder.build();
    }

    public synchronized static String encryptRequestBodyJson(RequestBody body,String aesKey) throws Exception {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        String bodyString =buffer.readUtf8();
        return AESUtils.encrypt(bodyString, aesKey);
    }

    public synchronized static String encryptTextFromRequest(RequestBody body, String aesKey) throws Exception {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        String bodyString =buffer.readUtf8();
        return AESUtils.encrypt(bodyString, aesKey);
    }
}
