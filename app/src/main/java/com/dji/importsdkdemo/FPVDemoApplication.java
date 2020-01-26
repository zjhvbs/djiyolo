package com.dji.importsdkdemo;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

class FPVDemoApplication extends Application {
    public static final String FLAG_CONNECT_CHANGE= "fpv_tutorial_connection_change";
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    public Handler mHandler;
    private static BaseProduct mProduct;
    private  Application instance;
    @Override

    public void onCreate() {

        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
            @Override
            public void onRegister(DJIError djiError) {
                if(djiError == DJISDKError.REGISTRATION_SUCCESS){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register success........zjhzheshiFPV", Toast.LENGTH_LONG).show();
                        }
                    });
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    Handler handler =new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"register sdk error,check!!zjh",Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onProductDisconnect() {
                Log.d("TAG", "onProductDisconnect");
                notifyStatusChange();
            }

            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                Log.d("TAG", String.format("onProductConnect newProduct:%s", baseProduct));
                notifyStatusChange();
            }

            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                        @Override
                        public void onConnectivityChange(boolean isConnected) {
                            Log.d("TAG", "onComponentConnectivityChanged: " + isConnected);
                            notifyStatusChange();
                        }
                    });
                }

            }

            @Override
            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

            }

            @Override
            public void onDatabaseDownloadProgress(long l, long l1) {

            }
        };
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {
            //This is used to start SDK services and initiate SDK.
            DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);
            Toast.makeText(getApplicationContext(), "registering, pls wait...", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(getApplicationContext(), "Please check if the permission is granted.", Toast.LENGTH_LONG).show();
        }

    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }
    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECT_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };

    public void setContext(Application application) {
        instance = application;
    }
    public Context getApplicationContext(){
        return instance;
    }
    public FPVDemoApplication(){

    }
    public static synchronized BaseProduct getmProductInstance(){
        if (null == mProduct){
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }
    public static synchronized Camera getCameraInstance(){
        if (getmProductInstance() == null)
            return null;
        Camera camera = null;
        if(getmProductInstance() instanceof Aircraft){
            camera =((Aircraft) getmProductInstance()).getCamera();
        }else if(getmProductInstance() instanceof HandHeld){
            camera = ((HandHeld) getmProductInstance()).getCamera();
        }
        return camera;
    }
}
