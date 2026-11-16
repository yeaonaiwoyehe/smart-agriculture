package cn.agrinav.utils.permission;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

/**
 * @author 11230
 */
public class PermissionUtils {
    public static Snackbar checkPermission(View snackBarContainer, Activity context , ArrayList<String> permissions, CheckPermissionListener listener,int requestCode,String snackBarTips){
        ArrayList<String> noPermissionList = new ArrayList<>();
        for (String permission:permissions){
            if (ContextCompat.checkSelfPermission(context,permission)!= PackageManager.PERMISSION_GRANTED){
                noPermissionList.add(permission);
            }
        }
        if (noPermissionList.isEmpty()){
            listener.onHavePermission();
            return null;
        }else{
            Snackbar snackbar = Snackbar.make(snackBarContainer, snackBarTips, Snackbar.LENGTH_INDEFINITE).setAnchorView(snackBarContainer);
            snackbar.setBackgroundTint(Color.WHITE);
            snackbar.setTextColor(Color.BLACK);
            snackbar.show();
            listener.noHavePermission(snackbar);
            ActivityCompat.requestPermissions(context,noPermissionList.toArray(new String[noPermissionList.size()]),requestCode);
            //在这里设置去请求用户权限
            return snackbar;
        }
    }


    public interface CheckPermissionListener{
        /**有权限会调用这个*/
        void onHavePermission();
        /**
        * 默认会有个snackBar提示用户
         * @param snackbar 表示弹出的提示框
        */
        void noHavePermission(Snackbar snackbar);
    }
}
