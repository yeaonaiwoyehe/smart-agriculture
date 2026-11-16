package cn.agrinav.base;



import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import com.amap.api.location.AMapLocationClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import cn.agrinav.R;
import cn.agrinav.front.activity.sysmessage.utils.SysMessageUtils;
import cn.agrinav.front.fragment.FragmentFront;
import cn.agrinav.measure.fragment.FragmentMeasure;
import cn.agrinav.personal.fragment.FragmentPersonal;
import cn.agrinav.trajectory.fragment.FragmentTrajectory;
import cn.agrinav.trajectory.permission.PermissionTrajectory;
import cn.agrinav.utils.DialogUtils;
import cn.agrinav.utils.DisplayUtil;
import cn.agrinav.utils.ToastUtil;

/**
 * @author 11230
 */
public class BaseActivity extends AppCompatActivity {

    private Fragment[] fragments;
    /**这里是这四个fragment*/
    private FragmentFront fragmentFront;
    private FragmentMeasure fragmentMeasure;
    private FragmentTrajectory fragmentTrajectory;
    private FragmentPersonal fragmentPersonal;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;
    private ToastUtil toastUtil;
    private Boolean havePermission = false;
    private ActivityResultLauncher lancher;
    private long d;
    private Snackbar snackbar;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            long date = System.currentTimeMillis();
            exit(date);
            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void exit(long date) {
        if (date - d < 2000) {
            finish();
        } else {
            d = date;
            new ToastUtil().getToast(this, "再按一次退出");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_base);
        initSdkPermission();
        initViews();
        initEvents();
        int index = getIntent().getIntExtra(getString(R.string.fragment_index), 0);
        switch (index) {
            case 0:
                bottomNavigationView.setSelectedItemId(R.id.menu_font);
                break;
            case 1:
                bottomNavigationView.setSelectedItemId(R.id.menu_measure);
                break;
            case 2:
                bottomNavigationView.setSelectedItemId(R.id.menu_trajectory);
                break;
            case 3:
                bottomNavigationView.setSelectedItemId(R.id.menu_personal);
                break;
            default:
                bottomNavigationView.setSelectedItemId(R.id.menu_font);
        }
        SysMessageUtils.checkUnreadMessages(this, sharedPreferences);
        toastUtil = new ToastUtil();
//        improvePositionAccuracy();
        lancher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> bottomNavigationView.setSelectedItemId(R.id.menu_trajectory));
    }
    private void improvePositionAccuracy() {
        boolean isIgnoring = false;
//        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        if (powerManager!=null){
//            isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());
//        }
//        if (!isIgnoring){
//            new AlertDialog.Builder(this)
//                    .setTitle("提示")
//                    .setMessage("需要通知，后台定位(始终允许定位)，以及后台耗电权限\n否则可能会导致定位失败")
//                    .setPositiveButton("去设置", (dialogInterface, i) -> {
//                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                        intent.setData(Uri.parse("package:" + getPackageName()));
//                        startActivity(intent);
//                        dialogInterface.dismiss();
//                    })
//                    .setNegativeButton("取消", (dialogInterface, i) -> {
//                        dialogInterface.dismiss();
//                    })
//                    .create()
//                    .show();
//        }
    }
    /**申请sdk的使用权限*/
    private void initSdkPermission() {
        AMapLocationClient.updatePrivacyAgree(this,true);
        AMapLocationClient.updatePrivacyShow(this,true,true);
    }
    /**初始化事件*/
    private void initEvents() {
        bottomNavigationView.setOnItemSelectedListener(v->{
            int id = v.getItemId();
            if (id==R.id.menu_font){
                switchFragment(fragments[0]);
            } else if (id==R.id.menu_measure) {
                switchFragment(fragments[1]);
            } else if (id==R.id.menu_trajectory) {
                changeCanUseMap();
            }else {
                switchFragment(fragments[3]);
            }
            return true;
        });
    }
    /**初始化组件*/
    private void initViews() {
        //首先获取到内容承载的地方
        sharedPreferences = getSharedPreferences(getString(R.string.user_pref_name), MODE_PRIVATE);
        FrameLayout containerView = findViewById(R.id.container_key);
        bottomNavigationView = findViewById(R.id.navigation_bottom_view);
        int itemSize = DisplayUtil.dip2px(this, 20);
        bottomNavigationView.setItemActiveIndicatorHeight(0);
        bottomNavigationView.setItemActiveIndicatorWidth(0);
        //TODO:在这里面创建各个fragment
        getFragment();
        //TODO:在这里面获取到所有的fragment
        //然后获取到fragment集合
        getFragments();
    }
    /** 切换fragment*/
    private void switchFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.container_key, fragment);
        transaction.commitAllowingStateLoss();
    }
    /**添加fragment*/
    private void getFragments() {
        if (fragments==null){
            //TODO:加入首页换回这一行
            fragments = new Fragment[]{fragmentFront, fragmentMeasure, fragmentTrajectory, fragmentPersonal};
        }
    }
    /**获取fragment集合*/
    private void getFragment() {
        if (fragmentFront==null){
            fragmentFront = new FragmentFront(this,sharedPreferences);
        }
        if (fragmentMeasure==null){
            fragmentMeasure = new FragmentMeasure(this);
        }
        if (fragmentTrajectory==null){
            fragmentTrajectory=new FragmentTrajectory(this,sharedPreferences);
        }
        if (fragmentPersonal==null){
            fragmentPersonal = new FragmentPersonal(this, sharedPreferences);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(Color.rgb(9,204,125));
        }
    }
    /**这个是用户使用地图服务之前的检查操作
     * 包含权限检查和能力检查
     * 如果检查成功了会让haveTargetingPermission为true,否则会让他为false*/
    private void changeCanUseMap() {
        //是否开启定位
        boolean allowTargeting= PermissionTrajectory.isAllowTargeting(this);
        //是否开启wifi
        boolean allowWifi = PermissionTrajectory.isAllowWifi(this);
        //先检查wifi和定位
        if (allowTargeting && allowWifi){
            //执行定位权限的检查，如果检查成功了会让haveTargetingPermission为true,否则会让他为false
            checkTargetingPermissions();
        }else {
            switchFragment(fragments[2]);
            if (!allowTargeting) {
                //在这里弹出一个弹窗让用户打开定位
                DialogUtils dialogUtils = new DialogUtils(this);
                AlertDialog dialog = dialogUtils.getDialog();
                dialogUtils.setTitle("温馨提示")
                       .setContent("该页面需要使用定位功能来查询周围轨迹信息,请打开定位功能")
                       .setPositiveText("去开启")
                       .setNegativeText("取消")
                       .setPositiveListener(v -> {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            lancher.launch(intent);
                            dialog.cancel();
                       })
                       .setNegativeListener(v -> {
                            bottomNavigationView.setSelectedItemId(R.id.menu_font);
                            dialog.cancel();
                       });
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }
            if(!allowWifi){
                //在这里弹出一个弹窗让用户打开定位
                DialogUtils dialogUtils = new DialogUtils(this);
                AlertDialog dialog = dialogUtils.getDialog();
                dialogUtils.setTitle("温馨提示")
                        .setContent("需要wifi功能来精确定位信息")
                        .setPositiveText("去开启")
                        .setNegativeText("取消")
                        .setPositiveListener(v -> {
                            //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            lancher.launch(intent);
                            dialog.cancel();
                        })
                        .setNegativeListener(v -> {
                            dialog.cancel();
                        });
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }
        }
    }
    /**这个是用来检查权限的*/
    private void checkTargetingPermissions() {
        ArrayList<String> permissions = null;
        //在这里把每一个权限都进行一次检验
        for(int i = 0; i< PermissionTrajectory.PERMISSIONS_LOCATION.length; i++) {
            //判断是否拥有某项权限
            if (ContextCompat.checkSelfPermission(this, PermissionTrajectory.PERMISSIONS_LOCATION[i]) != PackageManager.PERMISSION_GRANTED) {
                //判断是否有未获得的权限已经创建了集合，如果不是，现在就开始新建集合
                if(permissions==null){
                    permissions = new ArrayList<>();
                }
                //把欠缺的权限加入到欠缺集合中
                permissions.add(PermissionTrajectory.PERMISSIONS_LOCATION[i]);
            }
        }//判断是否有欠缺权限,如果有就去申请，没有就开始正常操作
        //判断是否有缺少的权限
        if(permissions!=null) {
            String[] strings = new String[permissions.size()];
            for (short j=0;j<strings.length;j++){
                strings[j]=permissions.get(j);
            }
            //在这里设置去请求用户权限
            snackbar = Snackbar.make(findViewById(R.id.snack_bar_container), "需要定位权限来获取地块信息", Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.snack_bar_container);
            snackbar.setBackgroundTint(Color.WHITE);
            snackbar.setTextColor(Color.BLACK);
            snackbar.show();
            ActivityCompat.requestPermissions(this,strings, getResources().getInteger(R.integer.permission_trajectory_request_code));
            Log.i(getResources().getString(R.string.trajectory_tag),"已经去申请权限了");
        }else {
            //如果不缺少定位的所有权限就直接初始化这个页面
            ifHavePermission();
        }
    }
    //如果权限从有到无是会直接闪退的，如果从无到有是可以的,所以在这个页面只需要在onCreate的时候才需要调用这个方法而且这次验证权限就只需要去发送一个定位信息给后台就可以了

    /**这个方法用于让用户自己去授权
     * 如果授权了就让haveTargetingPermission为true
     * 反之会让他为false*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String tag = getResources().getString(R.string.trajectory_tag);
        //如果回调方法中的是trajectory的内容状态码就执行以下逻辑
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        snackbar.dismiss();
        if (requestCode == getResources().getInteger(R.integer.permission_trajectory_request_code)) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                Toast.makeText(this, "用户已授权定位权限", Toast.LENGTH_SHORT).show();
                //用户如果给予了定位权限就直接让他操作了
                ifHavePermission();
            } else {
                //如果没有给权限就提示他，并且让他去给予权限，重新授权，不给权限不让用轨迹功能
                DialogUtils dialogUtils = new DialogUtils(this);
                AlertDialog dialog = dialogUtils.getDialog();
                dialogUtils.setTitle("温馨提示")
                        .setContent("该页面需要定位权限获取地址以及周围轨迹信息")
                        .setPositiveText("重新授权")
                        .setNegativeText("取消授权")
                        .setPositiveListener(v -> {
                            //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            lancher.launch(intent);
                            dialog.cancel();
                        })
                        .setNegativeListener(v -> {
                            //不给权限不让用这个功能给他个提示顺便窗口不可以关闭
                            DialogUtils dialogUtilsExit = new DialogUtils(this);
                            AlertDialog dialogExit = dialogUtilsExit.getDialog();
                            dialogUtilsExit.setTitle("温馨提示")
                                    .setContent("未授予定位权限,无法获取周围地块即将退出该界面,是否退出")
                                    .setPositiveText("退出")
                                    .setNegativeText("重新授权")
                                    .setPositiveListener(v1 -> {
                                        bottomNavigationView.setSelectedItemId(R.id.menu_font);
                                        dialogExit.cancel();
                                    })
                                    .setNegativeListener(v1 -> {
                                        //不给权限不让用这个功能给他个提示顺便窗口不可以关闭
                                        //如果要重新授权就直接让他重新给权限重新调用一遍直到他给我权限为止
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        lancher.launch(intent);
                                        dialogExit.cancel();
                                    });
                            View viewExit = dialogUtilsExit.getView();
                            dialogExit.setContentView(viewExit);
                            dialogExit.show();
                            dialog.cancel();
                        });
                View view = dialogUtils.getView();
                dialog.setContentView(view);
                dialog.show();
            }
        }
    }
    public void ifHavePermission(){
        //如果拥有权限就开始一个定位蓝点然后获取到自己现在的位置就可以了，然后还需要给后台发送请求，告知后台现在用户已经进入了这个页面
        havePermission= true;
        switchFragment(fragments[2]);
    }
}