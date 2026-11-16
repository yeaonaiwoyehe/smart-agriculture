package cn.agrinav.measure.utils;

import android.widget.ImageView;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.BaseRequestOptions;

import cn.agrinav.R;

/**
 * @author 18506
 */
@GlideExtension
public class LandPlotGlideExtension{
    private LandPlotGlideExtension() {}
    //默认加载设置
    @GlideOption
    public static BaseRequestOptions<?>defaultImage(BaseRequestOptions<?> options, ImageView imageView){
        return options.placeholder(R.mipmap.loading)
                .error(R.mipmap.error)
                .fallback(R.mipmap.empty)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .centerCrop()
                .override(imageView.getWidth(), imageView.getHeight());
    }
}
