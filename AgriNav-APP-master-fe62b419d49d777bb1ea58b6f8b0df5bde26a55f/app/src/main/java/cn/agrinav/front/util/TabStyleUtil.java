package cn.agrinav.front.util;


import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

public class TabStyleUtil {

    public static void setTabBold(TabLayout.Tab tab, boolean b) {
        //拿到tab的textView
        View view = tab.getCustomView();
        if (view != null && view instanceof TextView){
            ((TextView) view).setTypeface(null, b ? Typeface.BOLD : Typeface.NORMAL);

        }
    }
}
