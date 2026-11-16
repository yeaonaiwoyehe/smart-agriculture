package cn.agrinav.front.activity.front;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.youth.banner.Banner;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;
import com.youth.banner.indicator.CircleIndicator;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.entity.AgriculturalMachinery;
import cn.agrinav.front.util.TransformUtils;
import cn.agrinav.measure.utils.ImageLoaderUtils;

/**
 * @author 18506
 */
public class FreeAgriculturalMachineryActivity extends AppCompatActivity {

    private ImageButton backImageButton;
    private ImageButton shareImgButton;
    private RelativeLayout inquire;
    private RelativeLayout call;
    private AgriculturalMachinery agriculturalMachinery;
    private Banner machineryImage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_free_agricultural_machinery1);
        //获取数据
        Intent intent = getIntent();
        agriculturalMachinery = (AgriculturalMachinery) intent.getSerializableExtra("AgriculturalMachinery");
        //初始化页面组件
        initViews();
        //设置事件
        initEvents();
    }

    private void initEvents() {
        backImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BaseActivity.class);
            startActivity(intent);
        });
        shareImgButton.setOnClickListener(v -> {
            //TODO 分享逻辑
        });
        //咨询事件
        inquire.setOnClickListener(v -> {
            //TODO 咨询逻辑
        });
        //打电话联系
        call.setOnClickListener(v -> {
            //将电话号码传过去
            String phone = agriculturalMachinery.getPhoneNumber();
            Intent intent = new Intent(Intent.ACTION_DIAL);
            //设置Uri
            intent.setData(android.net.Uri.parse("tel:" + phone));
            startActivity(intent);
        });

    }

    private void initViews() {
        //返回按钮
        backImageButton = findViewById(R.id.iv_agricultural_machinery_back);
        //分享按钮
        shareImgButton = findViewById(R.id.iv_agricultural_machinery_share);
        //农机图
        machineryImage = findViewById(R.id.banner);
        //调用banner的addBannerLifecycleObserver()方法，让banner自己根据宿主的生命周期来控制
        machineryImage.setAdapter(new BannerImageAdapter<String>(agriculturalMachinery.getImage()) {
            @Override
            public void onBindView(BannerImageHolder holder, String data, int position, int size) {
                ImageLoaderUtils.loadNowImage(holder.itemView.getContext(),data, holder.imageView);
            }
        }).addBannerLifecycleObserver(this)
                .setIndicator(new CircleIndicator(this));
        //农机型号
        TextView machineryModel = findViewById(R.id.tv_detail_page_machinery_model);
        machineryModel.setText(agriculturalMachinery.getTypeName());
        //可作业作物
        TextView crop = findViewById(R.id.tv_detail_page_operable_crops);
        crop.setText(TransformUtils.transformRegion(agriculturalMachinery.getCrop()));
        //作业区域
        TextView workArea = findViewById(R.id.tv_detail_page_work_area);
        workArea.setText(TransformUtils.transformRegion(agriculturalMachinery.getLocation()));
        //配备运梁车
        TextView isTruck = findViewById(R.id.tv_detail_page_equipped_grain_truck);
        isTruck.setText(agriculturalMachinery.isHavingGrainCarrier() ? "是" : "否");
        //咨询一下
        inquire = findViewById(R.id.rl_detail_page_inquire);
        //电话联系
        call = findViewById(R.id.rl_detail_page_call);
    }
}