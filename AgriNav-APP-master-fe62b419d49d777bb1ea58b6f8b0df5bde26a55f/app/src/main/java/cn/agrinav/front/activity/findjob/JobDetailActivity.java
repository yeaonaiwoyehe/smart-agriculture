package cn.agrinav.front.activity.findjob;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import cn.agrinav.R;
import cn.agrinav.front.adapter.JobPhotoAdapter;
import cn.agrinav.front.entity.FindJobDetail;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.youth.banner.Banner;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;
import com.youth.banner.indicator.CircleIndicator;
import cn.agrinav.measure.utils.ImageLoaderUtils;
import java.util.Arrays;

import java.text.SimpleDateFormat;
import java.util.Date;

public class JobDetailActivity extends AppCompatActivity {
    private TextView tvTaskName;
    private TextView tvMachineType;
    private TextView tvWorkableCrops;
    private TextView tvWorkArea;
    private TextView tvHasGrainTransport;
    private TextView tvValidityPeriod;
    private Banner banner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);
        initViews();
        loadJobDetail();
    }

    private void initViews() {
        tvTaskName = findViewById(R.id.tv_task_name);
        tvMachineType = findViewById(R.id.tv_machine_type);
        tvWorkableCrops = findViewById(R.id.tv_workable_crops);
        tvWorkArea = findViewById(R.id.tv_work_area);
        tvHasGrainTransport = findViewById(R.id.tv_has_grain_transport);
        tvValidityPeriod = findViewById(R.id.tv_validity_period);
        banner = findViewById(R.id.banner);
    }

    private void loadJobDetail() {
        Intent intent = getIntent();
        FindJobDetail detail = (FindJobDetail) intent.getSerializableExtra("jobDetail");
        updateUI(detail);
    }

    private void updateUI(FindJobDetail detail) {
        if (detail.getImage() != null && detail.getImage().length > 0) {
            banner.setAdapter(new BannerImageAdapter<String>(Arrays.asList(detail.getImage())) {
                @Override
                public void onBindView(BannerImageHolder holder, String data, int position, int size) {
                    ImageLoaderUtils.loadNowImage(holder.itemView.getContext(), data, holder.imageView);
                }
            })
            .addBannerLifecycleObserver(this)
            .setIndicator(new CircleIndicator(this));
        }

        tvTaskName.setText(detail.getName());
        tvMachineType.setText(detail.getMachineType());
        
        // 设置可作业作物
        String[] crops = detail.getWorkableCrops();
        if (crops != null) {
            tvWorkableCrops.setText(String.join(" ", crops));
        }

        // 设置作业区域
        String[] workArea = detail.getWorkArea();
        StringBuilder workAreaString = new StringBuilder();
        for(int i = 0; i < workArea.length; i++) {
            if(i == workArea.length - 1) {
                workAreaString.append(workArea[i]);
            }else {
                workAreaString.append(workArea[i]).append("\n");
            }
        }
        tvWorkArea.setText(workAreaString.toString());

        // 设置是否配备运粮车
        tvHasGrainTransport.setText(detail.isHasGrainTransportVehicle() ? getString(R.string.find_job_true) : getString(R.string.find_job_false));

        // 设置有效期
        if (detail.getTaskValidityPeriod() != null) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.find_job_time_parse));
            tvValidityPeriod.setText(sdf.format(detail.getTaskValidityPeriod()));
        }
    }
}
