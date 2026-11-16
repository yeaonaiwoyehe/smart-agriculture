package cn.agrinav.front.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.ArrayList;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.activity.scan.ScanAfterActivity;
import cn.agrinav.utils.ToastUtil;

/**
 * @author 11230
 */
public class ScanCodeActivity extends AppCompatActivity {

    private DecoratedBarcodeView decoratedBarcodeView;
    private CaptureManager cameraManager;
    private Bundle instanceState;
    private Snackbar snackbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_code);
        instanceState = savedInstanceState;
        initViews();
        checkPermission(savedInstanceState);
    }
    //绑定相机
    private void initCamera(Bundle savedInstanceState) {
        cameraManager = new CaptureManager(this, decoratedBarcodeView);
        cameraManager.initializeFromIntent(getIntent(), savedInstanceState);
        cameraManager.decode();
        decoratedBarcodeView.decodeSingle(new BarcodeCallBackImpl());
        cameraManager.onPause();
        cameraManager.onResume();
    }
    private void checkPermission(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //如果有权限就让他使用
            initCamera(savedInstanceState);
        }else {
            //如果没有权限就执行后续操作初始化
            snackbar = Snackbar.make(findViewById(R.id.snack_bar_container_scan), "需要相机权限来扫描二维码", Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.snack_bar_container_scan);
            snackbar.setBackgroundTint(Color.WHITE);
            snackbar.setTextColor(Color.BLACK);
            snackbar.show();
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        }
    }
    class BarcodeCallBackImpl implements BarcodeCallback {
        @Override
        public void barcodeResult(BarcodeResult result) {
            //当收获内容之后就直接操作
            playBeepAndVibrate();
            Intent intent = new Intent(ScanCodeActivity.this, ScanAfterActivity.class);
            String text = result.getResult().getText();
            intent.putExtra("result", text);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        private void playBeepAndVibrate() {
            // 播放提示音
            MediaPlayer mediaPlayer = MediaPlayer.create(ScanCodeActivity.this, com.google.zxing.client.android.R.raw.zxing_beep);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
            // 震动
            ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        snackbar.dismiss();
        if (requestCode == 1){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //允许了就让他继续使用
                initCamera(instanceState);
            }else {
                //拒绝了就让回到上一个页面
                Intent intent = new Intent();
                intent.putExtra("result", "未授权相机权限，请授权");
                ToastUtil toastUtil = new ToastUtil();
                toastUtil.getToast(this, "未授权相机权限，请授权");
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        }
    }

    private void initViews() {
        decoratedBarcodeView = findViewById(R.id.front_barcode_scanner);
        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setAutoFocusEnabled(true);
        cameraSettings.setFocusMode(CameraSettings.FocusMode.CONTINUOUS);
        decoratedBarcodeView.setCameraSettings(cameraSettings);
        decoratedBarcodeView.getViewFinder().setVisibility(View.INVISIBLE);
        ArrayList<BarcodeFormat> list = new ArrayList<>();
        list.add(BarcodeFormat.QR_CODE);
        list.add(BarcodeFormat.CODE_128);
        list.add(BarcodeFormat.CODE_39);
        list.add(BarcodeFormat.CODE_93);
        list.add(BarcodeFormat.ITF);
        list.add(BarcodeFormat.EAN_13);
        list.add(BarcodeFormat.CODE_128);
        list.add(BarcodeFormat.EAN_8);
        list.add(BarcodeFormat.DATA_MATRIX);
        list.add(BarcodeFormat.AZTEC);
        list.add(BarcodeFormat.UPC_EAN_EXTENSION);
        list.add(BarcodeFormat.PDF_417 );
        list.add(BarcodeFormat.CODABAR);
        list.add(BarcodeFormat.MAXICODE);
        list.add(BarcodeFormat.CODE_93);
        decoratedBarcodeView.setStatusText("请把二维码放入框内");
        decoratedBarcodeView.setDecoderFactory(new DefaultDecoderFactory(list));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraManager != null){
            cameraManager.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraManager != null){
            cameraManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraManager != null){
            cameraManager.onDestroy();
        }
    }
}