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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import cn.agrinav.R;
import cn.agrinav.base.BaseActivity;
import cn.agrinav.front.entity.FarmWork;
import cn.agrinav.front.util.TransformUtils;
import cn.agrinav.measure.utils.ImageLoaderUtils;

/**
 * 农活需求详情页面
 * @author 18506
 */
public class DemandFarmWorkActivity extends AppCompatActivity {

    private Banner banner;
    private ImageButton imageButtonFarmWorkBack;
    private ImageButton imageButtonFarmWorkShare;
    private TextView address;
    private TextView area;
    private TextView areaFarm;
    private TextView crops;
    private TextView workType;
    private TextView deadline;
    private RelativeLayout inquire;
    private TextView call;
    private FarmWork farmWork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_demand_farm_work1);
        Intent intent = getIntent();
        farmWork = (FarmWork) intent.getSerializableExtra("FarmWork");
        initViews();
        initEvents();

    }

    private void initEvents() {
        //返回按钮事件
        imageButtonFarmWorkBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, BaseActivity.class);
            startActivity(intent);
        });
        //分享按钮事件
        imageButtonFarmWorkShare.setOnClickListener(v -> {
            //TODO 分享逻辑
        });
        //咨询按钮事件
        inquire.setOnClickListener(v -> {
            //TODO 咨询逻辑
        });
        //打电话联系
        call.setOnClickListener(v -> {
            //将电话号码传过去
            Intent intent = new Intent(Intent.ACTION_DIAL);
            //设置Uri
            intent.setData(android.net.Uri.parse("tel:" + farmWork.getPhoneNumber()));
            startActivity(intent);
        });
    }

    private void initViews() {
        //轮播图
        banner = findViewById(R.id.banner);
        //调用banner的addBannerLifecycleObserver()方法，让banner自己根据宿主的生命周期来控制
        banner.setAdapter(new BannerImageAdapter<String>(farmWork.getLandPlotImage()) {
                    @Override
                    public void onBindView(BannerImageHolder holder, String data, int position, int size) {
                        ImageLoaderUtils.loadNowImage(holder.itemView.getContext(),data, holder.imageView);
                    }
                }).addBannerLifecycleObserver(this)
                .setIndicator(new CircleIndicator(this));
        //返回按钮
        imageButtonFarmWorkBack = findViewById(R.id.iv_farm_work_back);
        //分享
        imageButtonFarmWorkShare = findViewById(R.id.iv_farm_work_share);
        //地块
        address = findViewById(R.id.tv_detail_page_address);
        String name = TransformUtils.transformRegion(farmWork.getLandPlotName());
        address.setText(name);
        //亩数
        area = findViewById(R.id.tv_detail_page_area);
        area.setText(farmWork.getArea().toString());
        //作业区域
        areaFarm = findViewById(R.id.tv_detail_page_work_area_farm);
        String location = TransformUtils.transformRegion(farmWork.getLandAddress());
        areaFarm.setText(location);
        //作物
        crops = findViewById(R.id.tv_detail_page_crops);
        crops.setText(TransformUtils.transformRegion(farmWork.getCropName()));
        //作业类型
        workType = findViewById(R.id.tv_detail_page_work_type);
        workType.setText(TransformUtils.transformRegion(farmWork.getWorkTypeName()));
        //截止日趋
        deadline = findViewById(R.id.tv_detail_page_deadline);
        if (farmWork.getEndTime()!=null){
            LocalDate localDate = farmWork.getCreateTime().toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            String deadlineTime = localDate.format(formatter);
            deadline.setText(deadlineTime);
        }
        //咨询
        inquire = findViewById(R.id.rl_detail_page_call_farm);
        //电话call
        call = findViewById(R.id.tv_phone_inquire_farm);
    }
}