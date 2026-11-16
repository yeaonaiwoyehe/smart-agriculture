package cn.agrinav;

import android.app.Application;
import android.content.Context;

/**
 * @author 11230
 */
public class AgriNavApplication extends Application {
    private static AgriNavApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static AgriNavApplication getInstance() {
        return instance;
    }

    public Context getAppContext() {
        return getApplicationContext();
    }
}
