package cn.agrinav.utils;



import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;

import cn.agrinav.AgriNavApplication;
import cn.agrinav.R;
import cn.agrinav.user.login.otherways.FingerprintLoginActivity;
import cn.agrinav.user.login.otherways.GestureActivity;
import cn.agrinav.user.login.otherways.WxLoginActivity;
import cn.agrinav.utils.encryption.AESUtils;
import cn.agrinav.utils.encryption.EncryptConfig;
import cn.agrinav.utils.encryption.RSAUtils;
import okhttp3.Call;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author 11230
 */
public class OkHttpUtils {
    /**创建客户端实例*/
    public static OkHttpClient okHttpClient;
    private final SharedPreferences sharedPreferences;
    private final Context context;
    private final SharedPreferences.Editor edit;
    private final String REQUEST_POST = "POST";
    private final String REQUEST_GET = "GET";
    private final String REQUEST_PUT = "PUT";
    private final String MEDIA_TYPE_JSON = "application/json";
    private final String MEDIA_TYPE = "application";
    private final String MEDIA_SUBTYPE_OCTET_STREAM = "octet-stream";
    public OkHttpUtils(){
        context = AgriNavApplication.getInstance().getApplicationContext();
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.user_pref_name), Context.MODE_PRIVATE);
        edit = sharedPreferences.edit();
        if(okHttpClient==null){
            okHttpClient = new OkHttpClient.Builder().addInterceptor(new OkHttpInterceptor()).build();
        }
    }
    private class OkHttpInterceptor implements Interceptor{

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            //获取请求
            Request request = chain.request();
            request = request.newBuilder().addHeader("source","App").build();
            //解析请求中的路由
            String s = request.url().encodedPath();
            //查看当前路由是否需要加密
            boolean b = EncryptConfig.checkPath(s);
            //需要加密就开始加密，不需要的时候就直接运行
            if (b){
                //对于加密的请求首先拿到aes密钥
                String aesKey = sharedPreferences.getString(context.getString(R.string.user_pref_key_aes_public),"");
                //使用aes密钥加密数据就一定要捕捉异常
                try {
                    String method = request.method();
                    Request.Builder builder = request.newBuilder();
                    if (method.equals(REQUEST_POST)||method.equals(REQUEST_PUT)){
                        RequestBody body = request.body();
                        //对于post请求分为两种一种是纯粹的application/json的请求体，一种是纯文本的请求体
                        if (body instanceof MultipartBody){
                            //非form-data的请求体只加密参数是text的内容
                            MultipartBody multipartBody = (MultipartBody) body;
                            //这是新的内容
                            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                            List<MultipartBody.Part> parts =multipartBody.parts();
                            for (MultipartBody.Part part : parts) {
                                //首先获取到这一行的请求体
                                RequestBody requestBody = part.body();
                                //在获取请求体的类型
                                MediaType mediaType = requestBody.contentType();
                                //然后提取出name
                                //查看这一部分是否需要加密
                                if (mediaType==null||!(mediaType.type().equals(MEDIA_TYPE)&&mediaType.subtype().equals(MEDIA_SUBTYPE_OCTET_STREAM))){
                                    if(mediaType==null || !mediaType.equals(MediaType.parse("image/*"))) {
                                        //如果需要加密首先进行加密
                                        String nameWithYH = part.headers().get("Content-Disposition").split(";")[1].split("=")[1];
                                        String name = nameWithYH.substring(1, nameWithYH.length() - 1);
                                        String encryptText = EncryptConfig.encryptTextFromRequest(requestBody, aesKey);
                                        //然后填入值
                                        multipartBuilder.addFormDataPart(name, encryptText);
                                    }else {
                                        multipartBuilder.addPart(part);
                                    }
                                }else {
                                    multipartBuilder.addPart(part);
                                }
                            }
                            body = multipartBuilder.build();
                            builder.setBody$okhttp(body);
                        }else {
                            //加密请求体的json部分得到加密数据
                            String encryptRequestBodyJson = EncryptConfig.encryptRequestBodyJson(body,aesKey);
                            //重新创建请求体
                            RequestBody requestBody = RequestBody.create(encryptRequestBodyJson,MediaType.parse(MEDIA_TYPE_JSON));
                            //重新设置请求体
                            builder.setBody$okhttp(requestBody);
                        }
                    }
                    if (method.equals(REQUEST_GET)) {
                        //加密param
                        HttpUrl httpUrl = EncryptConfig.encryptParams(request.url(), aesKey);
                        //设置请求参数
                        builder.url(httpUrl);
                    }
                    //然后发送得到加密的数据
                    Response response = chain.proceed(builder.build());
                    //获取到响应体
                    ResponseBody body = response.body();
                    //把响应转化成字符串
                    String string =  body.string();
                    //解析响应的字符串
                    String decrypt = AESUtils.decrypt(string, aesKey);
                    Result result = new Gson().fromJson(decrypt,new TypeToken<Result>(){}.getType());
                    if (result.getCode()==401){
                        Log.e("aa","token 过期了");
                        logoutUser(context);
                        return response.newBuilder().code(500).build();
                    }
                    //解密后返回本地该拿到的数据
                    return response.newBuilder().body(ResponseBody.create(body.contentType(),decrypt)).build();
                } catch (Exception e) {
                    //当捕捉到异常的时候就直接获取response然后直接返回
                    Log.e("aa","token 过期了");
                    if (!(e instanceof ConnectException)) {
                        logoutUser(context);
                    }
                    Response response = chain.proceed(request);
                    return response.newBuilder().code(500).build();
                }
            }else {
                //对于不需要加密的接口如果在登录，就需要给后端发送一个aes密钥，用于后续加密，如果成功了就直接继续之前的获取的内容
                if (s.equals(context.getString(R.string.verification_code_router_getInfo))){
                    //检测是否是登陆的接口就看是否给我token和rsa密钥了，如果给了就说明是登录的接口
                    try {
                        //创建出来一个密钥
                        String aesKey = AESUtils.generateKey();
                        //然后使用rsa加密aes
                        String rsaPublicKey = sharedPreferences.getString(context.getString(R.string.user_pref_key_rsa_public),"");
                        String encryptAesKey = RSAUtils.encrypt(aesKey,rsaPublicKey);
                        //然后发送
                        OkHttpUtils okHttpUtils = new OkHttpUtils();
                        Request.Builder builder = okHttpUtils.createBuilder(context.getString(R.string.server_url), context.getString(R.string.verification_code_router_send_aes));
                        RequestBody requestBody = RequestBody.create(encryptAesKey,MediaType.parse("application/json"));
                        //需要携带token
                        Request requestAes = builder
                                .addHeader(context.getString(R.string.request_header_key_token), sharedPreferences.getString(context.getString(R.string.user_pref_key_token), ""))
                                .post(requestBody).build();
                        Response response = okHttpUtils.sendRequest(requestAes);
                        //如果请求是成功的
                        if (response.isSuccessful()){
                            String string = response.body().string();
                            Log.e("OkHttpUtilsExchange",string);
                            Result result = new Gson().fromJson(string, new TypeToken<Result>(){}.getType());
                            if (result.getCode()!=context.getResources().getInteger(R.integer.request_success)){
                                throw new RuntimeException(result.getMsg());
                            }else {
                                //发送成功之后本地记得保存一份aes密钥
                                edit.putString(context.getString(R.string.user_pref_key_aes_public),aesKey);
                                edit.commit();
                            }
                        }else {
                            throw new RuntimeException("请求失败");
                        }
                    } catch (Exception e) {
                        //当捕捉到异常的时候就直接获取response然后直接返回
                        Response response = chain.proceed(request);
                        return response.newBuilder().code(800).build();
                    }

                }
                Response response = chain.proceed(request);
                //获取响应体
                ResponseBody body = response.body();
                if (body != null&&response.isSuccessful()) {
                    String responseBody = body.string();
                    Result result = new Gson().fromJson(responseBody,new TypeToken<Result>(){}.getType());
                    if (result.getCode()==401){
                        Log.e("aa","token 过期了");
                        logoutUser(context);
                        return response.newBuilder().code(500).build();
                    }
                    return response.newBuilder().body(ResponseBody.create(body.contentType(),responseBody)).build();
                }else {
                    return response;
                }
            }
        }
    }
    protected void logoutUser(Context context) {
        // 清除登录状态
        String phone = sharedPreferences.getString(context.getString(R.string.user_pref_key_phone), "");
        String token = sharedPreferences.getString(context.getString(R.string.user_pref_key_token), "");
        boolean isAllowFingerLogin = sharedPreferences.getBoolean(context.getString(R.string.user_pref_key_finger), false);
        boolean isAllowGestureLogin = sharedPreferences.getBoolean(context.getString(R.string.user_pref_key_gesture), false);
        String gesturePassword = sharedPreferences.getString(context.getString(R.string.user_pref_key_gesture_password), "");
        edit.clear();
        edit.putString(context.getString(R.string.user_pref_key_token), token);
        edit.putString(context.getString(R.string.user_pref_key_phone), phone);
        edit.putString(context.getString(R.string.user_pref_key_user_state), context.getString(R.string.user_state_exit));
        edit.putBoolean(context.getString(R.string.user_pref_key_finger), isAllowFingerLogin);
        edit.putBoolean(context.getString(R.string.user_pref_key_gesture), isAllowGestureLogin);
        edit.putString(context.getString(R.string.user_pref_key_gesture_password), gesturePassword);
        edit.putBoolean(context.getString(R.string.user_pref_delete_sdcard), false);
        edit.putBoolean(context.getString(R.string.user_first),false);
        edit.apply();
        // 判断跳转页面
        if(sharedPreferences.getBoolean(context.getString(R.string.user_pref_key_finger), false)) {
            Intent intent = new Intent(context, FingerprintLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }else if(sharedPreferences.getBoolean(context.getString(R.string.user_pref_key_gesture), false)) {
            Intent intent = new Intent(context, GestureActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }else {
            Intent intent = new Intent(context, WxLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    public Response sendRequest(Request request) throws IOException {
        Call call = okHttpClient.newCall(request);
        return call.execute();
    }
    public Call sendRequest(Request request, Callback callback){
        Call call = okHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }
    public Request.Builder createBuilder(String ip,String path){
        Request.Builder builder = new Request.Builder();
        return builder.url(ip+path);
    }
    public Request.Builder createBuilder(String ip,String path,HashMap<String,?> params){
        Request.Builder builder = new Request.Builder();
        String url = ip+path;
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        sb.append("?");
        for (String key : params.keySet()) {
            sb.append(key);
            sb.append("=");
            sb.append(params.get(key));
            sb.append("&");
        }
        int index = sb.lastIndexOf("&");
        sb.replace(index,index+1,"");
        return builder.url(sb.toString());
    }


}
