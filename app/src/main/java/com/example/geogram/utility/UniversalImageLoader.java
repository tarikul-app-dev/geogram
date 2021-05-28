package com.example.geogram.utility;

import android.content.Context;
import android.util.Log;


import com.example.geogram.R;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class UniversalImageLoader {
    private static final String TAG = "UniversalImageLoader";
    private static final int defaultImage = R.drawable.ic_launcher_background;
    private Context mContext;

    public UniversalImageLoader(Context context) {
        this.mContext = context;
        Log.d(TAG, "UniversalImageLoader: started");
    }

    public ImageLoaderConfiguration getConfig(){
        Log.d(TAG, "getConfig: Returning image loader configuration");
        // UNIVERSAL IMAGE LOADER SETUP
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(defaultImage) // resource or drawable
                .showImageForEmptyUri(defaultImage) // resource or drawable
                .showImageOnFail(defaultImage) // resource or drawable
                .cacheOnDisk(true).cacheInMemory(true)//for fast load the image
                .cacheOnDisk(true).resetViewBeforeLoading(true)//for fast load the image
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new FadeInBitmapDisplayer(300)).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                mContext)
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCache(new WeakMemoryCache())//for fast load the image
                .diskCacheSize(100 * 1024 * 1024)//for fast load the image
                .build();


        return config;
    }


}
