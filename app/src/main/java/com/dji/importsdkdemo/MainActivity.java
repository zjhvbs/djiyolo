package com.dji.importsdkdemo;

import android.app.Activity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {
    protected TextureView mVideoSurface = null;
    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected DJICodecManager mCodeManager = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;

    private  Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        final TextView recordingTime1 =(TextView) findViewById(R.id.timer);
        recordingTime1.setVisibility(View.INVISIBLE);
        initUI();
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
            if (mCodeManager!=null){
                mCodeManager.sendDataToDecoder(videoBuffer,size);
            }
            }
        };
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera!=null){
            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(@NonNull SystemState cameraSystemState) {
                    if (null != cameraSystemState){
                        final int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime%3600)/60;
                        int seconds = recordTime%60;
                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (timeString !=null) {
                                    recordingTime1.setText(timeString);
                                }else{
                                    showToast("timeString为空");
                                }
                                if (isVideoRecording){
                                    recordingTime1.setVisibility(View.VISIBLE);

                                }else{
                                    recordingTime1.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });
        }
    }
    protected  void onProductChange(){
        initPreviewer();
        loginAccount();
    }

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this, new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(UserAccountState userAccountState) {
                Log.e(TAG,"login success");
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG,"login fail"+ djiError.getDescription()); }
        });
    }

    private void initPreviewer() {
        BaseProduct product = FPVDemoApplication.getmProductInstance();
        if(product==null||!product.isConnected()){
            showToast("Disconnect");
        }else{
            if(mVideoSurface != null){
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if(!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)){
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener
                );
            }
        }
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
     public void onResume() {
         Toast.makeText(this,"欢迎回来。。",Toast.LENGTH_LONG).show();
         super.onResume();
         initPreviewer();
         onProductChange();
        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
     }
     @Override
     public void onPause() {
         Log.e(TAG, "onPause");
         uninitPreviewer();
         super.onPause();
     }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
     public void onStop() {

         super.onStop();
     }
     public  void onReturn(View view){
        this.finish();
     }
     @Override
     protected void onDestroy() {
         uninitPreviewer();

         super.onDestroy();
     }

    private void initUI() {
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        Button mCaptureBtn = findViewById(R.id.btn_capture);
        ToggleButton mRecordBtn = findViewById(R.id.btn_record);
        Button mShootPhotoModenBtn = findViewById(R.id.btn_shoot_photo_mode);
        Button mRecordVideoModeBtn = findViewById(R.id.btn_record_video_mode);
        if (mVideoSurface !=null){
            mVideoSurface.setSurfaceTextureListener(this);
        }
        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModenBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    startRecord();
                }else{
                    stopRecord();
                }
            }
        });

    }

    private void stopRecord() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera!=null){
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null){
                        showToast("stop recording success....zjh");
                    }else{
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    private void startRecord() {
        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera !=null){
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError == null){
                        showToast("record success");

                    }else{
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodeManager == null) {
            mCodeManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodeManager != null) {
            mCodeManager.cleanSurface();
            mCodeManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onClick(View v) {
    switch(v.getId()){
        case R.id.btn_capture:{
            captureAction();
            break;
        }
        case R.id.btn_shoot_photo_mode:{
            switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
            break;
        }
        case R.id.btn_record_video_mode:{
            switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);

            break;
        }
        default:
            break;
    }

    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode) {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if(camera != null){
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null){
                        showToast("switch camera mode succeed...zjh");

                    }else{
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    private void captureAction() {
        final Camera camera =FPVDemoApplication.getCameraInstance();
        showToast("take photo start");

        if(camera != null){
            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE;
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError){
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null){
                                                showToast("take photo success");
                                        }else{
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        },2000);
                    }
                }
            });
        }
        else{
            showToast("capture camera null");
        }
    }

}
