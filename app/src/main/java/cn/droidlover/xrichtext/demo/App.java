package cn.droidlover.xrichtext.demo;

import android.app.Application;

/**
 * Created by wanglei on 2016/11/02.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        UILKit.init(getApplicationContext());        //初始化UIL
    }

}
