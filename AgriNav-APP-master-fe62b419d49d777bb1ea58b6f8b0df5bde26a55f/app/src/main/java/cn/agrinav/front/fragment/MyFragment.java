package cn.agrinav.front.fragment;


import androidx.fragment.app.Fragment;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;


/**
 * @author lya
 *
 */
public abstract class MyFragment extends Fragment {
    abstract public void onLoadMore(SmartRefreshLayout layout);
}
