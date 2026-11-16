package cn.agrinav.measure.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import cn.agrinav.user.entity.SysUser;
import cn.agrinav.user.entity.UserInfoEntity;

/**
 * @author 18506
 */
public class DefaultInfoUtils {
    private static SharedPreferences sharedPreferences;
    private static Context context;


    public static void init(Context appContext) {
        context = appContext.getApplicationContext();
    }

    //获取token
    public static String getToken(Context context){
        sharedPreferences = context.getSharedPreferences("verification", Context.MODE_PRIVATE);
        return sharedPreferences.getString("token", "");
    }
    public static String getToken(){
        sharedPreferences = context.getSharedPreferences("verification", Context.MODE_PRIVATE);
        return sharedPreferences.getString("token", "");
    }
    //获取用户
    public static SysUser getUserData(Context context) {
        //从本地存储中拿到用户信息
        sharedPreferences = context.getSharedPreferences("verification", Context.MODE_PRIVATE);
        String userInfoStr = sharedPreferences.getString("userInfo", "");
        //解析成对象
        Gson gson = new Gson();
        UserInfoEntity userInfoEntity = gson.fromJson(userInfoStr, UserInfoEntity.class);
        //拿到具体的用户信息对象
        return userInfoEntity.getUser();
    }

    public static String getUserDataName(Context context) {
        //从本地存储中拿到用户信息
        sharedPreferences = context.getSharedPreferences("verification", Context.MODE_PRIVATE);
        String nickName = sharedPreferences.getString("nickName", "");
        //拿到具体的用户信息对象
        return nickName;
    }

    public static String getUserDataPhone(Context context) {
        //从本地存储中拿到用户信息
        sharedPreferences = context.getSharedPreferences("verification", Context.MODE_PRIVATE);
        String phoneNumber = sharedPreferences.getString("phone", "");
        //拿到具体的用户信息对象
        return phoneNumber;
    }

}
