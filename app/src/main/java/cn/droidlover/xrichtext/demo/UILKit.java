package cn.droidlover.xrichtext.demo;

import android.content.Context;
import android.os.Environment;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.io.File;

/**
 * Created by wanglei on 2016/11/02.
 */

public class UILKit {

    private static UILKit instance = null;
    private static DisplayImageOptions picOptions = null;

    /**
     * 初始化（外部调用）
     *
     * @param context
     */
    public static void init(Context context) {
        if (instance == null) {
            synchronized (UILKit.class) {
                if (instance == null) {
                    instance = new UILKit(context);
                }
            }
        }
    }

    /**
     * 初始化
     *
     * @param context
     */
    private UILKit(Context context) {
        File cacheDir = getDiskCacheDir(context, "img");

        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCacheSize(10 * 1024 * 1024)
                .diskCache(new UnlimitedDiskCache(cacheDir))
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCacheSize(50 * 1024 * 1024) // 50 MiB
                .tasksProcessingOrder(QueueProcessingType.LIFO);

        ImageLoader.getInstance().init(config.build());



        picOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.EXACTLY)
                .cacheOnDisk(false).cacheInMemory(false)
                .resetViewBeforeLoading(true)
                .displayer(new FadeInBitmapDisplayer(500)).build();
    }


    public static ImageLoader getLoader() {
        return ImageLoader.getInstance();
    }

    public static DisplayImageOptions getPicOptions() {
        return picOptions;
    }

    /**
     * 获取缓存地址
     *
     * @param context
     * @param uniqueName
     * @return
     */
    private static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }
}
