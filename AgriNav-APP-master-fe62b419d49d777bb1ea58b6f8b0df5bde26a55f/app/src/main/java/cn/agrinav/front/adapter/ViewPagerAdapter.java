package cn.agrinav.front.adapter;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

import cn.agrinav.front.fragment.MyFragment;

/**
 * viewpager2的适配器
 * @author 18506
 */
public class ViewPagerAdapter extends FragmentStateAdapter {
    private List<MyFragment> fragmentList;
    public ViewPagerAdapter(List<MyFragment> fragmentList,@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentList = fragmentList;
    }
    @NonNull
    @Override
    public MyFragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

}
