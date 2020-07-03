package com.dji.importsdkdemo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hushiyu1995.yolo_v3.Yolo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class ConnectionActivity extends Activity implements View.OnClickListener{
    private TextView mTextConnectionStatus;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };
    private TextView mTextProduct;
    private TextView mVersionTv;
    private Button mBtnOpen;
    private Button mBtnlogin;
    private Button mBtnlogout;


    private static final String TAG = ConnectionActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE ="dji_sdk_connection_change";
    private Handler mHander;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
          checkAndRequestPermission();
           setContentView(R.layout.activity_connection);

           initUI();

           //本地广播，注册广播
           IntentFilter filter=new IntentFilter();
           filter.addAction(FPVDemoApplication.FLAG_CONNECT_CHANGE);
           registerReceiver(mReceiver,filter);


       }
    }



    private void initUI() {
        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);
        mBtnOpen = (Button) findViewById(R.id.btn_open);


        mBtnOpen.setOnClickListener(this);

        mBtnOpen.setEnabled(false);

        mVersionTv = (TextView) findViewById(R.id.textView2);
        mVersionTv.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));
        mBtnlogin=(Button)findViewById(R.id.btn_login);
        mBtnlogout = findViewById(R.id.btn_logout);
        mBtnlogin.setOnClickListener(this);
        mBtnlogout.setOnClickListener(this);
        mBtnlogin.setEnabled(false);
        mBtnlogout.setEnabled(false);
        Toast.makeText(this,"开始广播",Toast.LENGTH_LONG).show();

    }


    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = FPVDemoApplication.getmProductInstance();
        showToast("asdasdasdasdasdasdasdasd");
        if (mProduct !=null && mProduct.isConnected()){
            mBtnOpen.setEnabled(true);
            mBtnlogin.setEnabled(true);
            mBtnlogout.setEnabled(true);
            String str = mProduct instanceof Aircraft ? "DJIAircaft" :"DJIHandHeld";
            mTextConnectionStatus.setText("Status"+str+"已连接");
            if (null != mProduct.getModel()){
                mTextProduct.setText(' '+mProduct.getModel().getDisplayName());
            }else{
                mTextProduct.setText("Product information");
            }
        }else {

            mBtnOpen.setEnabled(false);
            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
        }
    }

    private void checkAndRequestPermission() {
        for(String eachPermission:REQUIRED_PERMISSION_LIST){
            if(ContextCompat.checkSelfPermission(this,eachPermission)!= PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);

            }
        }
        if (missingPermission.isEmpty()){
            startSDKRegistration();
        }else if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M){
            showToast("Need to grant the permission!");
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),REQUEST_PERMISSION_CODE);
        }
    }

    private void showToast(final String s) {
        Handler handler =new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startSDKRegistration() {
        if(isRegistrationInProgress.compareAndSet(false,true)){
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast(" connection registering pls wait.");
                    DJISDKManager.getInstance().registerApp(ConnectionActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            //showToast("onRegister...zjh");
                            if(djiError == DJISDKError.REGISTRATION_SUCCESS){
                                showToast("connenct register success");
                                DJISDKManager.getInstance().startConnectionToProduct();
                            }
                                else{
                                    showToast("register fail....zjh");

                                }

                        }

                        @Override
                        public void onProductDisconnect() {
                            showToast("product Disconnect");

                            Intent intent = new Intent();
                            intent.setAction(FPVDemoApplication.FLAG_CONNECT_CHANGE);
                            sendBroadcast(intent);


                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            showToast("product connected");

                            Intent intent = new Intent();
                            intent.setAction(FPVDemoApplication.FLAG_CONNECT_CHANGE);
                            sendBroadcast(intent);

                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                                if(newComponent != null){
                                    newComponent.setComponentListener(new BaseComponent.ComponentListener() {
                                        @Override
                                        public void onConnectivityChange(boolean b) {
                                            Log.d(TAG, "onComponentConnectivityChanged: " + b);

                                        }
                                    });
                                }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long l, long l1) {

                        }
                    });
                }
            });
        }


    }


    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }
    }
    private void logoutAccount() {
        UserAccountManager.getInstance().logoutOfDJIUserAccount(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (null ==djiError){
                    showToast("logout,success");

                }else{
                    showToast("logout,error"+ djiError.getDescription());
                }
            }
        });

    }

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this, new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(UserAccountState userAccountState) {
                showToast("login,Success");
                Intent intent = new Intent(ConnectionActivity.this,MainActivity.class);
                startActivity(intent);
            }

            @Override
            public void onFailure(DJIError djiError) {
                showToast("login,Fauile"+djiError.getDescription());
            }
        });
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_open:{
                Intent intent = new Intent(ConnectionActivity.this,MainActivity.class);
                startActivity(intent);

                break;
            }
            case R.id.btn_login:{
                loginAccount();
                break;

            }
            case R.id.btn_logout:{
                logoutAccount();
                break;
            }


            default:
                break;
        }

    }
    @Override
    public void onPause() {
        Log.e(TAG, "onPause");

        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        Intent intent = new Intent();
        intent.setAction(FPVDemoApplication.FLAG_CONNECT_CHANGE);
        sendBroadcast(intent);
        super.onResume();
    }

}
