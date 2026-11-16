package cn.agrinav.trajectory.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.location.AMapLocation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import cn.agrinav.R;
import cn.agrinav.trajectory.entity.TrajectoryPosition;
import cn.agrinav.utils.jtt808.send.JTT808T0003Sender;
import cn.agrinav.utils.jtt808.send.JTT808T0200Sender;
import cn.agrinav.utils.jtt808.send.JTT808T0801Sender;
import cn.agrinav.utils.netty.NettyClientManager;
import cn.agrinav.utils.netty.exception.NettyConnectionException;

public class JTService extends Service {
    private NettyClientManager manager;
    private JTT808T0200Sender jtt808T0200Sender;
    private JTT808T0003Sender jtt808T0003Sender;
    private JTT808T0801Sender jtt808T0801Sender;
    private int width;
    private int workTypeId;
    private String phone;
    private static final int POSITION_SEND = 0;
    private static final int MEDIA_SEND = 1;
    private Handler handlerSave;
    private MediaPlayer player;
    public JTService() {
        try {
            construct808();
        } catch (NettyConnectionException e) {
            Log.i("aaa","建立连接失败");
        }
        Log.i("lifeService","服务构造");
    }
    @Override
    public void onCreate() {
        super.onCreate();
        initMessagePool();
//        Log.i("lifeService","服务以创建");
//        play();
    }
    private void play() {
        if (player == null) {
            player = MediaPlayer.create(getApplicationContext(), R.raw.other_love);
            Log.i("a", "aaaa");
        }
        player.start();
    }
    private void stop() {
        if (player != null && player.isPlaying()) {
            player.stop();
            //重置播放器
            player.reset();
            //释放资源
            player.release();
        }
    }
    private void initMessagePool() {
        new Thread(()->{
            Looper.prepare();
            handlerSave = new Handler(Looper.myLooper()){
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what==POSITION_SEND){
                        Intent intent = (Intent) msg.obj;
                        phone = intent.getStringExtra("phone");
                        width = intent.getIntExtra("width",0);
                        workTypeId = intent.getIntExtra("workTypeId",0);
                        sendLocation(phone,intent.getDoubleExtra("latitude",0),intent.getDoubleExtra("longitude",0),width,workTypeId);
                    } else if (msg.what==MEDIA_SEND) {
                        Intent intent = (Intent) msg.obj;
                        sendMedia(
                            intent.getStringExtra("phone"),
                            intent.getByteArrayExtra(getString(R.string.trajectory_work_service_extra_data_media)),
                            intent.getIntExtra("mediaId", 0),
                            intent.getDoubleExtra(getString(R.string.trajectory_work_service_extra_data_latitude), 0),
                            intent.getDoubleExtra(getString(R.string.trajectory_work_service_extra_data_longitude), 0),
                            intent.getIntExtra("speed", 0),
                            intent.getIntExtra("direction", 0)
                        );
                    }
                }
            };
            Looper.loop();
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String type = intent.getStringExtra(getString(R.string.trajectory_work_service_media_type_key));
        if (getString(R.string.trajectory_work_service_media_type_value).equals(type)) {
            // 处理多媒体数据上传
            if(handlerSave != null) {
                handlerSave.sendMessage(handlerSave.obtainMessage(MEDIA_SEND, intent));
            }
        } else {
            // 处理位置信息上传
            Log.i(getResources().getString(R.string.trajectory_tag),
                "当前的位置："+intent.getDoubleExtra("latitude",0)+
                "   "+intent.getDoubleExtra("longitude",0));
            if(handlerSave != null) {
                handlerSave.sendMessage(handlerSave.obtainMessage(POSITION_SEND, intent));
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
//        stop();
        Log.i("lifeService","服务已销毁");
        end808();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    /**
     * 构造808的信息
     */
    private void construct808() throws NettyConnectionException {
        // 实例化NettyClientManager
        manager = NettyClientManager.getInstance();
        //终端注销
        jtt808T0003Sender = new JTT808T0003Sender(manager);
        //终端位置信息汇报
        jtt808T0200Sender = new JTT808T0200Sender(manager);
        //多媒体数据上传
        jtt808T0801Sender = new JTT808T0801Sender(manager);
    }
    /**
     * 发送坐标信息
     */
    private void sendLocation(String phone,double latitude,double longitude,int width,int workTypeId) {
        Map<Integer, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(0xee, (byte) workTypeId);
        additionalInfo.put(0xff, (byte) width);
        jtt808T0200Sender.sendLocationMessage("0".concat(phone), latitude, longitude, 0, 0, System.currentTimeMillis(), additionalInfo);
    }
    /**
     * 发送多媒体数据
     */
    private void sendMedia(String phone, byte[] mediaData, int mediaId,
                         double latitude, double longitude, 
                         int speed, int direction) {
        if (jtt808T0801Sender != null && phone != null && mediaData != null) {
            Map<Integer, Object> additionalInfo = new HashMap<>();
            additionalInfo.put(0xee, (byte) workTypeId);
            additionalInfo.put(0xff, (byte) width);
            
            try {
                jtt808T0801Sender.send(
                    "0".concat(phone),
                    mediaData,
                    mediaId,
                    latitude,
                    longitude,
                    speed,
                    direction,
                    System.currentTimeMillis(),
                    additionalInfo
                );
                Log.i("Media", "多媒体数据上传成功");
            } catch (Exception e) {
                Log.e("Media", "多媒体数据上传失败", e);
            }
        }
    }
    /**
     * 结束808
     */
    private void end808(){
        if (jtt808T0003Sender!=null&&phone!=null) {
            //注销
            jtt808T0003Sender.sendLogoutMessage("0".concat(phone));
        }
        //断开连接
        manager.close();
    }

}