package cn.agrinav.measure.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.ImageSwitcher;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;


/**
 * @author 18506
 * 用于加载图片，并将图片大小样式与View适配
 */
public class ImageLoaderUtils {
    public static void loadNowImage(Context context, String imageUrl, ImageView imageView){
        if (imageUrl == null){
            return;
        }
        GlideApp.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 使用磁盘缓存
                .into(imageView);
    }
    public static void loadNowImage(Context context, String imageUrl, ImageSwitcher imageView){
        GlideApp.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 使用磁盘缓存
                .into((ImageView) imageView.getNextView());
        imageView.showNext();
    }
    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        // 在加载新图片之前，先清除 ImageView 的内容
        imageView.setImageDrawable(null);

        GlideApp.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 使用磁盘缓存
                .into(new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
                // 获取ImageView的宽高
                int targetWidth = imageView.getWidth();
                int targetHeight = imageView.getHeight();
                
                // 如果ImageView的宽高为0，使用wrap_content的默认大小
                if (targetWidth == 0) {
                    targetWidth = ImageView.resolveSize(resource.getIntrinsicWidth(), imageView.getMeasuredWidthAndState());
                }
                if (targetHeight == 0) {
                    targetHeight = ImageView.resolveSize(resource.getIntrinsicHeight(), imageView.getMeasuredHeightAndState());
                }

                // 对Drawable进行中心裁剪以适应ImageView
                Drawable croppedDrawable = centerCropDrawable(resource, targetWidth, targetHeight);

                // 先获取 ImageView 的背景
                Drawable background = imageView.getBackground();
                if (background instanceof LayerDrawable) {
                    LayerDrawable layerDrawable = (LayerDrawable) background;
                    Drawable topBackground = layerDrawable.getDrawable(0);
                    if (topBackground instanceof GradientDrawable) {
                        GradientDrawable gradientDrawable = (GradientDrawable) topBackground;
                        // 获取背景的形状（如矩形、圆形等）和圆角半径等信息
                        int shape = gradientDrawable.getShape();
                        float[] radii = gradientDrawable.getCornerRadii();
                        // 根据背景的形状和圆角信息对加载的图片进行处理
                        if (shape == android.graphics.drawable.GradientDrawable.RECTANGLE) {
                            if (radii != null && radii.length > 0) {
                                croppedDrawable = getRoundedDrawable(context, croppedDrawable, radii);
                            }
                        } else if (shape == android.graphics.drawable.GradientDrawable.OVAL) {
                            croppedDrawable = getCircularDrawable(context, croppedDrawable);
                        }
                    }
                }
                // 设置处理后的图片
                imageView.setImageDrawable(croppedDrawable);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                // 当加载被清除时，确保 ImageView 为空
                imageView.setImageDrawable(null);
            }
        });
    }

    // 将图片转换为圆角图片
    private static Drawable getRoundedDrawable(Context context,Drawable drawable, float[] radii) {
        // 实现圆角图片的转换逻辑
        // 您可以使用 BitmapShader 或其他方式来实现
        // 检查输入的 Drawable 是否为 BitmapDrawable 类型
        if (drawable instanceof BitmapDrawable) {
            // 获取 BitmapDrawable 中的 Bitmap 对象
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            // 创建一个与原始 Bitmap 大小相同、配置为 ARGB_8888 的新 Bitmap
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            // 创建用于绘制的 Canvas 对象，并关联新创建的 Bitmap
            Canvas canvas = new Canvas(output);
            // 创建一个抗锯齿的 Paint 对象
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            // 创建 BitmapShader 对象，用于在绘制时平铺 Bitmap
            paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            // 创建 RectF 对象，用于定义绘制圆角矩形的区域
            RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            // 在 Canvas 上绘制圆角矩形
            canvas.drawRoundRect(rectF, radii[0], radii[1], paint);
            // 将绘制后的 Bitmap 包装为 BitmapDrawable 并返回
            return new BitmapDrawable(context.getResources(),output);
        }
        // 如果输入的 Drawable 类型不支持转换，直接返回原 Drawable
        return drawable;
    }
    // 将图片转换为圆形图片
    private static Drawable getCircularDrawable(Context context,Drawable drawable) {
        // 实现圆形图片的转换逻辑
        // 您可以使用 BitmapShader 或其他方式来实现
        // 检查输入的 Drawable 是否为 BitmapDrawable 类型
        if (drawable instanceof BitmapDrawable) {
            // 获取 BitmapDrawable 中的 Bitmap 对象
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            // 创建一个与原始 Bitmap 大小相同、配置为 ARGB_8888 的新 Bitmap
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            // 创建用于绘制的 Canvas 对象，并关联新创建的 Bitmap
            Canvas canvas = new Canvas(output);
            // 创建一个抗锯齿的 Paint 对象
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            // 创建 BitmapShader 对象，用于在绘制时平铺 Bitmap
            paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            // 获取 Bitmap 的较小边长作为直径
            int diameter = Math.min(bitmap.getWidth(), bitmap.getHeight());
            // 创建 RectF 对象，用于定义绘制圆形的区域
            RectF rectF = new RectF(0, 0, diameter, diameter);
            // 在 Canvas 上绘制圆形
            canvas.drawOval(rectF, paint);
            // 将绘制后的 Bitmap 包装为 BitmapDrawable 并返回
            return new BitmapDrawable(context.getResources(),output);
        }
        // 如果输入的 Drawable 类型不支持转换，直接返回原 Drawable
        return drawable;
    }

    private static Drawable centerCropDrawable(Drawable drawable, int targetWidth, int targetHeight) {
        Bitmap sourceBitmap = drawableToBitmap(drawable);
        
        float sourceRatio = (float) sourceBitmap.getWidth() / sourceBitmap.getHeight();
        float targetRatio = (float) targetWidth / targetHeight;

        int resultWidth;
        int resultHeight;
        int x, y;

        if (sourceRatio > targetRatio) {
            resultWidth = (int) (sourceBitmap.getHeight() * targetRatio);
            resultHeight = sourceBitmap.getHeight();
            x = (sourceBitmap.getWidth() - resultWidth) / 2;
            y = 0;
        } else {
            resultWidth = sourceBitmap.getWidth();
            resultHeight = (int) (sourceBitmap.getWidth() / targetRatio);
            x = 0;
            y = (sourceBitmap.getHeight() - resultHeight) / 2;
        }

        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, x, y, resultWidth, resultHeight);
        return new BitmapDrawable(null, resultBitmap);
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
