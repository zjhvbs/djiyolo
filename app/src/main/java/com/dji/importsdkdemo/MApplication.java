package com.dji.importsdkdemo;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.secneo.sdk.Helper;

public class MApplication extends Application {
    public FPVDemoApplication fpvDemoApplication ;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //ultiDex.install(this);
        Helper.install(MApplication.this);
        if (fpvDemoApplication != null) {
            fpvDemoApplication = new FPVDemoApplication();
            fpvDemoApplication.setContext(this);
        }

    }

    @Override
    public void onCreate() {

        super.onCreate();
        if (fpvDemoApplication != null ) {
            fpvDemoApplication.onCreate();
        }
        else{
            Toast.makeText(this,"asdasdasd",Toast.LENGTH_LONG).show();
        }
    }
}