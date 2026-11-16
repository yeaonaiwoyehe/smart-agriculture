package cn.agrinav.trajectory.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import cn.agrinav.MainActivity;
import cn.agrinav.R;
import cn.agrinav.trajectory.adapter.ItemWorkAdapter;
import cn.agrinav.trajectory.entity.ItemWork;
import cn.agrinav.trajectory.entity.SortMessage;
import cn.agrinav.trajectory.util.DataUtil;
import okhttp3.Call;

/**
 * @author 11230
 * 页面包含一个gridview
 * 当onstart的时候直接检测是否listview中含有六条以上的数据
 *     如果有，不动list,如果没有，获取新的list
 */
public class FragmentTrajectoryWork extends Fragment {
    private  final AppCompatActivity activity;
    private  ItemWorkAdapter itemWorkAdapter;
    private  GridView gridView;
    private  int pageNum = 1;
    private  int pageSize = 6;
    private  ArrayList<ItemWork> itemWorks;
    private  Call workTrajectoryCall;
    private  Call workTrajectoryByCondition;
    private  DataUtil dataUtil;
    private  final SortMessage sortMessage;
    private  SmartRefreshLayout refreshLayout;
    private long a;
    private ClassicsFooter classicsFooter;
    public FragmentTrajectoryWork() {
        activity=null;
        sortMessage=null;
    }
    public FragmentTrajectoryWork(WeakReference<AppCompatActivity> activityWeakReference, WeakReference<SortMessage> sortMessage) {
        this.activity = activityWeakReference.get();
        this.sortMessage = sortMessage.get();
    }
    public void refresh(){
        pageNum = 1;
        if(sortMessage!=null){
            if (sortMessage.isEmpty()) {
                getWorkInfoNullCondition();
            } else {
                getWorkInfoByCondition();
            }
        }
    }
    public void load(RefreshLayout refreshLayout){
        if(sortMessage!=null){
            if (sortMessage.isEmpty()) {
                addWorkInfoNullCondition(refreshLayout);
            } else {
                addWorkInfoByCondition(refreshLayout);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (activity==null&&sortMessage==null){
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trajectory_work,null);
        initViews(view);
        initData();
        initEvents();
        refresh();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }



    private void initEvents() {
        //初始化事件
        //下拉刷新时间
        refreshLayout.setOnRefreshListener(refreshlayout -> {
            //刷新数据
            refresh();
            //刷新完成
            refreshlayout.finishRefresh();
        });
        refreshLayout.setOnLoadMoreListener(refreshlayout -> {
            pageNum++;
            load(refreshlayout);
        });
    }
    private void initData() {
        //初始化数据
        itemWorks = new ArrayList<>();
        //初始化工具包
        dataUtil = new DataUtil(activity);
        //初始化适配器
        itemWorkAdapter = new ItemWorkAdapter(itemWorks, R.layout.item_trajectory_work, this.getContext());
    }
    private void initViews(View view){
        //初始化gridView
        refreshLayout = view.findViewById(R.id.refreshLayout);
        gridView = view.findViewById(R.id.gv_show_trajectory_work);
        refreshLayout.refreshDrawableState();
        classicsFooter = view.findViewById(R.id.grid_footer);
    }
    private void getWorkInfoNullCondition() {
        //初始化出来工作的数据
        workTrajectoryCall = new DataUtil(activity).getWorkTrajectoryData(itemWorks, pageNum, pageSize,gridView,itemWorkAdapter);
    }
    private void getWorkInfoByCondition() {
        //获取条件查询的工作数据
        workTrajectoryByCondition = new DataUtil(activity).getWorkTrajectoryConditionData(itemWorks,sortMessage,pageNum,pageSize,gridView,itemWorkAdapter);
    }
    private void addWorkInfoNullCondition(RefreshLayout refreshLayout){
        workTrajectoryCall = new DataUtil(activity).addWorkTrajectoryData(itemWorks,pageNum,pageSize,refreshLayout,itemWorkAdapter,classicsFooter);
    }
    private void addWorkInfoByCondition(RefreshLayout refreshLayout){
        workTrajectoryByCondition = new DataUtil(activity).addWorkTrajectoryConditionData(itemWorks,sortMessage,pageNum,pageSize,refreshLayout,itemWorkAdapter,classicsFooter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (workTrajectoryCall!=null) {
            workTrajectoryCall.cancel();
        }else if(workTrajectoryByCondition!=null) {
            workTrajectoryByCondition.cancel();
        }
    }
}
