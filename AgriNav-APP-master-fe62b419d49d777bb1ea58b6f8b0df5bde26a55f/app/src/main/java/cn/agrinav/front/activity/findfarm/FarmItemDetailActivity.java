package cn.agrinav.front.activity.findfarm;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.youth.banner.Banner;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;
import com.youth.banner.indicator.CircleIndicator;

import cn.agrinav.R;
import cn.agrinav.front.adapter.FarmPhotoAdapter;
import cn.agrinav.front.entity.FindFarmDetail;
import cn.agrinav.measure.utils.ImageLoaderUtils;

public class FarmItemDetailActivity extends AppCompatActivity {

    private Banner farmItemBanner;
    private TextView tvTaskName, tvLandPlot, tvLandArea, tvAddress, tvCrops, tvOperationTypes, tvOperationDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_item_detail);
        initViews();
        loadTaskDetail();
    }

    private void initViews() {
        farmItemBanner = findViewById(R.id.farm_item_banner);
        tvTaskName = findViewById(R.id.tv_task_name);
        tvLandPlot = findViewById(R.id.tv_land_plot);
        tvLandArea = findViewById(R.id.tv_land_area);
        tvAddress = findViewById(R.id.tv_address);
        tvCrops = findViewById(R.id.tv_crops);
        tvOperationTypes = findViewById(R.id.tv_operation_types);
        tvOperationDate = findViewById(R.id.tv_operation_date);

    }

    private void loadTaskDetail() {
        FindFarmDetail detail = (FindFarmDetail) getIntent().getSerializableExtra("FarmDetail");
        if (detail != null) {
            // 加载地块图片
            if (detail.getImageList() != null && !detail.getImageList().isEmpty()) {
                farmItemBanner.setAdapter(new BannerImageAdapter<String>(detail.getImageList()) {
                    @Override
                    public void onBindView(BannerImageHolder holder, String data, int position, int size) {
                        ImageLoaderUtils.loadNowImage(holder.itemView.getContext(), data, holder.imageView);
                    }
                })
                        .addBannerLifecycleObserver(this)
                        .setIndicator(new CircleIndicator(this));
            }else{
                farmItemBanner.setVisibility(ViewPager2.GONE);
                ImageView farmItemImage = findViewById(R.id.farm_item_image);
                farmItemImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(R.drawable.plot_use).into(farmItemImage);
            }

            // 设置任务名称
            tvTaskName.setText(detail.getTaskName());

            // 设置地块信息
            if (detail.getLandPlots() != null && !detail.getLandPlots().isEmpty()) {
                StringBuilder landNames = new StringBuilder();
                detail.getLandPlots().forEach(landPlot -> {
                    if (landNames.length() > 0) {
                        landNames.append(" ");
                    }
                    landNames.append(landPlot.getLandName());
                });
                tvLandPlot.setText(landNames.toString());
            }

            // 设置地块亩数
            tvLandArea.setText(detail.getLandPlotArea() + "亩");

            // 设置地址
            if (detail.getLandPlots() != null && !detail.getLandPlots().isEmpty()) {
                StringBuilder addresses = new StringBuilder();
                detail.getLandPlots().forEach(landPlot -> {
                    if (addresses.length() > 0) {
                        addresses.append("\n");
                    }
                    addresses.append(landPlot.getAddress());
                });
                tvAddress.setText(addresses.toString());
            }

            // 设置作物
            tvCrops.setText(String.join(" ", detail.getCrops()));
            // 设置作业类型
            tvOperationTypes.setText(String.join(" ", detail.getOperationTypes()));

            // 设置作业有效期
            tvOperationDate.setText(detail.getOperationDate());
        }
    }
}