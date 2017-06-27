package tech.michaelx.hookactivityexample;

import android.app.Application;

/**
 * Created by xiongxunxiang on 2017/6/27. * Created by michaelx on 2017/6/27.
 * <p>
 * Blog:http://blog.csdn.net/xiong_it | https://xiong-it.github.io
 * github:https://github.com/xiong-it
 * <p> */

public class AppContext extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        HookUtil hookUtil = new HookUtil(this, ProxyActivity.class);
        hookUtil.hookAms();
        hookUtil.hookSystemHandler();
    }
}
