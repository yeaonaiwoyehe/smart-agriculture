package cn.agrinav.utils.camera;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
/**
 * @author 11230
 */
public class CameraManager {
    /**
     * 首先是需要相机功能的Context
     */
    private Context context;
    /**
     * 相机预览框
     */
    private PreviewView previewView;
    /**
     * 拍照器
     */
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> instance;
    private ProcessCameraProvider processCameraProvider;
    private Preview preview;
    public static final byte CAMERA_TYPE_BACK = 0;
    public static final byte CAMERA_TYPE_FRONT = 1;
    public byte currentType=0;
    private ImageCapture.OnImageSavedCallback photoCallback = new ImageCapture.OnImageSavedCallback(){

        @Override
        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
            Toast.makeText(context, "拍照成功", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(@NonNull ImageCaptureException exception) {
            Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show();
        }
    };
    private Bitmap bitmap;
    private byte[] imageData;

    public CameraManager(Context context, PreviewView previewView) {
        this.context = context;
        this.previewView = previewView;
    }
    public ContentValues setSaveConfig(){
        //然后创建保存规则
        String name = SimpleDateFormat.getInstance().format(System.currentTimeMillis());
        //创建好保存方式
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        return contentValues;
    }
    /**
     * 后置摄像头选项
     */
    public void initCamera(){
        //首先获取相机进程集合
        instance = ProcessCameraProvider.getInstance(context);
        instance.addListener(() -> {
            try {
                //然后获取到相机进程
                processCameraProvider = instance.get();
                //预览窗口设置取景器
                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                //设置拍照器
                imageCapture = new ImageCapture.Builder().build();
                bindBackCamera();
            } catch (Exception e) {
                Log.e("TAG", "相机初始化失败", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }
    public boolean bindBackCamera(){
        if (preview!=null&&imageCapture!=null) {
            processCameraProvider.unbind(preview, imageCapture);
            //绑定相机生命周期和对应的控制器和预览内容
            processCameraProvider.bindToLifecycle((LifecycleOwner) context, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            currentType = CAMERA_TYPE_BACK;
            return true;
        }else {
            return false;
        }
    }
    public boolean bindFrontCamera(){
        if (preview!=null&&imageCapture!=null) {
            processCameraProvider.unbind(preview, imageCapture);
            //绑定相机生命周期和对应的控制器和预览内容
            processCameraProvider.bindToLifecycle((LifecycleOwner) context, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture);
            currentType = CAMERA_TYPE_FRONT;
            return true;
        }else {
            return false;
        }
    }
    public void takePhoto(int type){
        ContentValues contentValues = setSaveConfig();
        boolean haveBind = false;
        //首先切换绑定的摄像头
        if (type==CAMERA_TYPE_BACK){
           haveBind =  bindBackCamera();
        }else{
           haveBind =  bindFrontCamera();
        }
        //然后获取拍照得到的流
        ImageCapture.OutputFileOptions.Builder outputFileOptionsBuilder = new ImageCapture.OutputFileOptions.Builder(
                context.getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);
        if (haveBind){
            imageCapture.takePicture(outputFileOptionsBuilder.build(), ContextCompat.getMainExecutor(context), photoCallback);
        }
    }
    public void takePhotoChange(){
        ContentValues contentValues = setSaveConfig();
        boolean haveBind = false;
        //首先切换绑定的摄像头
        if (currentType==CAMERA_TYPE_BACK){
           haveBind =  bindFrontCamera();
        }else{
           haveBind =  bindBackCamera();
        }
        //然后获取拍照得到的流
        ImageCapture.OutputFileOptions.Builder outputFileOptionsBuilder = new ImageCapture.OutputFileOptions.Builder(
                context.getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);
        if (haveBind){
            imageCapture.takePicture(outputFileOptionsBuilder.build(), ContextCompat.getMainExecutor(context), photoCallback);
        }
    }
    public byte[] takePhotoChangeForBitmap(BitmapGetCallback callback) {
        boolean haveBind = false;
        // 首先切换绑定的摄像头
        if (currentType == CAMERA_TYPE_BACK) {
            haveBind = bindFrontCamera();
        } else {
            haveBind = bindBackCamera();
        }

        // 创建一个 ByteArrayOutputStream 来保存图像数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // 然后获取拍照得到的流
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(outputStream).build();
        if (haveBind) {
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(context),new BitmapCallback(outputStream, callback));
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            Log.e("aaaa","aaaaaa");
        }
        return imageData;
    }
 class BitmapCallback implements ImageCapture.OnImageSavedCallback {
     private ByteArrayOutputStream outputStream;
     private BitmapGetCallback callback;

     public BitmapCallback(ByteArrayOutputStream outputStream,BitmapGetCallback callback) {
         this.outputStream = outputStream;
         this.callback = callback;
     }

     @Override
     public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
         // 将 ByteArrayOutputStream 转换为 byte 数组
         imageData = outputStream.toByteArray();
         callback.onBitmapGet(imageData);
     }

     @Override
     public void onError(@NonNull ImageCaptureException exception) {
         // 处理错误
         Log.e("CameraManager", "Error taking picture: " + exception.getMessage());
     }
 }
 public interface BitmapGetCallback {
        void onBitmapGet(byte[] data);
 }
}
