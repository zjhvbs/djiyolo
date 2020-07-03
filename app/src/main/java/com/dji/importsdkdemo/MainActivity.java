package com.dji.importsdkdemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import com.dji.importsdkdemo.media.DJIVideoStreamDecoder;
import com.hushiyu1995.yolo_v3.yolotest;

import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
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
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;
import dji.thirdparty.afinal.core.AsyncTask;

import static com.hushiyu1995.yolo_v3.Yolo.copyFilesFassets;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener,DJICodecManager.YuvDataCallback {
    protected TextureView mVideoSurface = null;
    private FlightController mFlightController;
    private OnScreenJoystick mScreenJoystickRight,mScreenJoystickLeft;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected VideoFeeder.VideoDataListener getmReceivedVideoDataListener_test=null;
    protected DJICodecManager mCodeManager = null;
//    protected DJICodecManager mCodeManager_test = null;
    //四个按钮 private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private Button mYoloBtn;//抓拍
    private Button mExtractFileBtn;//释放模型
    private boolean isExtractFile=false;
    private Button mVideoManager;
    private  Handler handler;
    //////////////抓拍
    private int count;
    private StringBuilder stringBuilder;
    private ImageView show_img;
//    private SurfaceView videostreamPreviewSf;
//    private SurfaceHolder videostreamPreviewSh;
//    private SurfaceHolder.Callback surfaceCallback;//用来感知surfaceview的创建等过程
    private int videoViewWidth;
    private int videoViewHeight;
//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            if (msg.what == ) {
////                .setText("completed");
//            }
//        }
//    };
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
            public void onReceive(byte[] videoBuffer, int size)
            {
            if (mCodeManager!=null)
                {
                    mCodeManager.sendDataToDecoder(videoBuffer,size);
                }
            }
        };
//        两个摄像头的时候用mCodeManager_test
//        getmReceivedVideoDataListener_test = new VideoFeeder.VideoDataListener() {
//            @Override
//            public void onReceive(byte[] videoBuffer, int size) {
//                if(mCodeManager_test!=null){
//               mCodeManager_test.sendDataToDecoder(videoBuffer,size,VideoSource.CAMERA);
//
//
//                }else{
//                    showToast("secondary manager is null");
//                }
//            }
//        };
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
                                    recordingTime1.setVisibility(View.GONE);
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
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
//                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(getmReceivedVideoDataListener_test);
            }
        }
    }
    private void initFlightControaller() {
        Aircraft aircraft = FPVDemoApplication.getAircraftInstance();
        if(aircraft==null ||!aircraft.isConnected()){
            showToast("未连接");
            mFlightController=null;
            return;
        }else{
            mFlightController = aircraft.getFlightController();
            mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError==null){
                        showToast("虚拟摇杆启用成功");
                    }else{
                        showToast("虚拟摇杆不能正常启用");
                    }
                }
            });
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        }
    }

    @Override
    public void onYuvDataReceived(final MediaFormat format, ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
        if (count++ % 180 == 0 && yuvFrame != null) {
            final byte[] bytes = new byte[dataSize];
            yuvFrame.get(bytes);
            //DJILog.d(TAG, "onYuvDataReceived2 " + dataSize);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    // two samples here, it may has other color format.
                    int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    switch (colorFormat) {
                        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                            //NV12
                            if (Build.VERSION.SDK_INT <= 23) {
                                oldSaveYuvDataToJPEG(bytes, width, height);
                            } else {
                                newSaveYuvDataToJPEG(bytes, width, height);
                            }
                            break;
                        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                            //YUV420P
                            newSaveYuvDataToJPEG420P(bytes, width, height);
                            break;
                        default:
                            break;
                    }

                }
            });
        }
    }

    private void newSaveYuvDataToJPEG420P(byte[] yuvFrame, int width, int height) {
        if (yuvFrame.length < width * height) {
            return;
        }
        int length = width * height;

        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];

        for (int i = 0; i < u.length; i ++) {
            u[i] = yuvFrame[length + i];
            v[i] = yuvFrame[length + u.length + i];
        }
        for (int i = 0; i < u.length; i++) {
            yuvFrame[length + 2 * i] = v[i];
            yuvFrame[length + 2 * i + 1] = u[i];
        }
        screenShot(yuvFrame, Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);

    }

    private void screenShot(byte[] buf, String shotDir, int width, int height) {
        File dir = new File(shotDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        YuvImage yuvImage = new YuvImage(buf,
                ImageFormat.NV21,
                width,
                height,
                null);
        OutputStream outputFile;
        final String path = dir + "/ScreenShot_" + System.currentTimeMillis() + ".jpg";
        try {
            outputFile = new FileOutputStream(new File(path));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "test screenShot: new bitmap output file error: " + e);
            return;
        }
        if (outputFile != null) {
            yuvImage.compressToJpeg(new Rect(0,
                    0,
                    width,
                    height), 100, outputFile);
        }
        try {
            outputFile.close();
        } catch (IOException e) {
            Log.e(TAG, "test screenShot: compress yuv image error: " + e);
            e.printStackTrace();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                String[] outgir =path.split("\\.");
                String outdir = outgir[0] + "yoloshibie";
                yolotest df =new yolotest();
                boolean isperson = df.isperson(path,outdir);
                outdir=outdir+".png";
                displayPath(path,isperson,outdir);
            }
        });
//        yolotest testperson = new yolotest();
//        final boolean df = testperson.isperson(path);

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                displayPath(path,df);
//            }
//        });

    }

    private void displayPath(String path,final boolean isperson,final String outdir) {
        if (stringBuilder == null) {
            stringBuilder = new StringBuilder();
        }

        path = path + "\n";
        stringBuilder.append(path);
        //savePath.setText(stringBuilder.toString());
                if (isperson) {

                    showToast("有人，已保存"+outdir);
                    Bitmap bitmap = BitmapFactory.decodeFile(outdir);
                    show_img.setImageBitmap(bitmap);

                }else{
//                    Bitmap bitmap = BitmapFactory.decodeFile(outdir);
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.alert_icon);
                    show_img.setImageBitmap(bitmap);
//                    show_img.setImageDrawable();

                    showToast("没有人");
                }
    }



    private void newSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height) {
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }
        int length = width * height;

        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[length + 2 * i];
            u[i] = yuvFrame[length + 2 * i + 1];
        }
        for (int i = 0; i < u.length; i++) {
            yuvFrame[length + 2 * i] = u[i];
            yuvFrame[length + 2 * i + 1] = v[i];
        }
        screenShot(yuvFrame,Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);

    }

    private void oldSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height) {
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }

        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];

        System.arraycopy(yuvFrame, 0, y, 0, y.length);
        for
        (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[y.length + 2 * i];
            u[i] = yuvFrame[y.length + 2 * i + 1];
        }
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j= 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                byte uSample1 = u[i * uvWidth + j];
                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                nu[2 * (i * uvWidth + j)] = uSample1;
                nu[2 * (i * uvWidth + j) + 1] = uSample1;
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                nv[2 * (i * uvWidth + j)] = vSample1;
                nv[2 * (i * uvWidth + j) + 1] = vSample1;
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
            }
        }
        //nv21test
        byte[] bytes = new byte[yuvFrame.length];
        System.arraycopy(y, 0, bytes, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            bytes[y.length + (i * 2)] = nv[i];
            bytes[y.length + (i * 2) + 1] = nu[i];
        }
        Log.d(TAG,
                "onYuvDataReceived: frame index: "
                        + DJIVideoStreamDecoder.getInstance().frameIndex
                        + ",array length: "
                        + bytes.length);
        screenShot(bytes, Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);
    }

    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {
            if (mFlightController != null){
                mFlightController.sendVirtualStickFlightControlData(new FlightControlData(mPitch
                        , mRoll, mYaw, mThrottle), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError!=null){
                            showToast(""+djiError.getDescription());
                        }
                    }
                });
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
        initFlightControaller();
//        initPreviewerSurfaceView();
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
        mScreenJoystickLeft = findViewById(R.id.directionJoystickLeft);
        mScreenJoystickRight = findViewById(R.id.directionJoystickRight);

        mYoloBtn = findViewById(R.id.btn_yolo);
        mYoloBtn.setOnClickListener(this);
        mVideoManager = findViewById(R.id.btn_videomanager);
        mVideoManager.setOnClickListener(this);
        mExtractFileBtn = findViewById(R.id.btn_extractfile);
        mExtractFileBtn.setOnClickListener(this);
        mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX)<0.02){
                    pX=0;
                }
                if (Math.abs(pY)<0.02){
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed =5;
                float rollJoyControlMaxSpeed = 5;
                mPitch = (float)(pitchJoyControlMaxSpeed*pX);
                mRoll = (float)(rollJoyControlMaxSpeed*pY);
                if (null ==mSendVirtualStickDataTimer){
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask,100,200);

                }
            }
        });
        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = 2;
                float yawJoyControlMaxSpeed = 30;

                mYaw = (float)(yawJoyControlMaxSpeed * pX);
                mThrottle = (float)(verticalJoyControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }
        });
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        File s =new File("/sdcard/djiyolotest/yolo/cfg");
        if (s.exists()){
            mExtractFileBtn.setEnabled(false);
            mYoloBtn.setEnabled(true);
        }
        show_img = findViewById(R.id.isperson_imageView);
        show_img.setVisibility(View.GONE);
//        videostreamPreviewSf = findViewById(R.id.video_surfaceView);
//        videostreamPreviewSf.setClickable(false);
//        videostreamPreviewSf.setOnClickListener(new View.OnClickListener(){
//
//            @Override
//            public void onClick(View v) {
//                float rate = VideoFeeder.getInstance().getTranscodingDataRate();
//                showToast("current rate"+rate+"Mbs");
//                if (rate < 10) {
//                    VideoFeeder.getInstance().setTranscodingDataRate(10.0f);
//                    showToast("set rate to 10Mbps");
//                } else {
//                    VideoFeeder.getInstance().setTranscodingDataRate(3.0f);
//                    showToast("set rate to 3Mbps");
//                }
//
//            }
//        });

//        Button mCaptureBtn = findViewById(R.id.btn_capture);
//        ToggleButton mRecordBtn = findViewById(R.id.btn_record);
//        Button mShootPhotoModenBtn = findViewById(R.id.btn_shoot_photo_mode);
//        Button mRecordVideoModeBtn = findViewById(R.id.btn_record_video_mode);
//        if (mVideoSurface !=null){
//            mVideoSurface.setSurfaceTextureListener(this);
//        }
//        mCaptureBtn.setOnClickListener(this);
//        mRecordBtn.setOnClickListener(this);
//        mShootPhotoModenBtn.setOnClickListener(this);
//        mRecordVideoModeBtn.setOnClickListener(this);
//
//        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked){
//                    startRecord();
//                }else{
//                    stopRecord();
//                }
//            }
//        });

    }

//    private void stopRecord() {
//        Camera camera = FPVDemoApplication.getCameraInstance();
//        if (camera!=null){
//            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError djiError) {
//                    if (djiError == null){
//                        showToast("stop recording success....zjh");
//                    }else{
//                        showToast(djiError.getDescription());
//                    }
//                }
//            });
//        }
//    }
//
//    private void startRecord() {
//        final Camera camera = FPVDemoApplication.getCameraInstance();
//        if (camera !=null){
//            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError djiError) {
//                    if(djiError == null){
//                        showToast("record success");
//
//                    }else{
//                        showToast(djiError.getDescription());
//                    }
//                }
//            });
//        }
//    }

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
//        case R.id.btn_capture:{
//            captureAction();
//            break;
//        }
//        case R.id.btn_shoot_photo_mode:{
//            switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
//            break;
//        }
//        case R.id.btn_record_video_mode:{
//            switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
//
//            break;
//        }
        case R.id.btn_videomanager:{
            Intent intent = new Intent(MainActivity.this,YoloMediaActivity.class);
            startActivity(intent);
            this.onDestroy();
            break;

        }
        case R.id.btn_yolo:{
            if(mCodeManager!=null) {
                handleYUVClick();
            }else{
                showToast("second manager need time");
            }

            break;
        }
        case R.id.btn_extractfile:{
            isExtractFile = extractFile();
            if(isExtractFile){
                mYoloBtn.setEnabled(true);
            }
            break;
        }
        default:
            break;
    }

    }

    private boolean extractFile() {


        boolean iscfg,isdata,isweights;

        iscfg = copyFilesFassets(this, "cfg", "/sdcard/djiyolotest/yolo/cfg");
        isdata=copyFilesFassets(this, "data", "/sdcard/djiyolotest/yolo/data");
        isweights=copyFilesFassets(this, "weights", "/sdcard/djiyolotest/yolo/weights");
        if (iscfg&&isdata&&isweights){
            showToast("释放模型成功");
            return true;
        }else{
            showToast("释放模型失败");
            return false;
        }
    }

    private void handleYUVClick() {
        if (mYoloBtn.isSelected()){

            showToast("识别模式关闭");
            show_img.setVisibility(View.GONE);
            mVideoSurface.setVisibility(View.VISIBLE);
//            mCodeManager_test.enabledYuvData(false);
//            mCodeManager_test.setYuvDataCallback(null);
            mCodeManager.enabledYuvData(false);
            mCodeManager.setYuvDataCallback(null);
            mYoloBtn.setSelected(false);
            mYoloBtn.setText("开启识别模式");

        }else{
            showToast("识别模式已开启");
            mVideoSurface.setVisibility(View.GONE);
            show_img.setVisibility(View.VISIBLE);

            mYoloBtn.setSelected(true);
//            mCodeManager_test.enabledYuvData(true);
//            mCodeManager_test.setYuvDataCallback(this);
            mCodeManager.enabledYuvData(true);
            mCodeManager.setYuvDataCallback(this);
            mYoloBtn.setText("关闭识别模式");
        }

    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode) {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if(camera != null){
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null){
                        showToast("切换模式成功");

                    }else{
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    private void captureAction() {
        final Camera camera =FPVDemoApplication.getCameraInstance();
        showToast("准备拍照");

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
                                                showToast("拍照成功");
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
//    private void initPreviewerSurfaceView() {
//        videostreamPreviewSh = videostreamPreviewSf.getHolder();
//        surfaceCallback = new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
////                Log.d(TAG,"real onSurfaceTextureAvailable");
//                videoViewWidth = videostreamPreviewSf.getWidth();
//                videoViewHeight = videostreamPreviewSf.getHeight();
////                Log.d(TAG,"real onSurfaceTextureAvailable:width"+videoViewWidth+"height"+videoViewHeight);
//
//                        if (mCodeManager_test == null) {
//                            mCodeManager_test = new DJICodecManager(getApplicationContext(), holder, videoViewWidth,
//                                    videoViewHeight);
//                        }
//            }
//
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable4: width " + videoViewWidth + " height " + videoViewHeight);
//
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//
//                        if (mCodeManager_test != null) {
//                            mCodeManager_test.cleanSurface();
//                            mCodeManager_test.destroyCodec();
//                            mCodeManager_test = null;
//                        }
//            }
//        };
//        videostreamPreviewSh.addCallback(surfaceCallback);
//
//    }

}
