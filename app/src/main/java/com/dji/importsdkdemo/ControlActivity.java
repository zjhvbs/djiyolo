package com.dji.importsdkdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class ControlActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = ControlActivity.class.getName();
    //虚拟摇杆
    private FlightController mFlightController;
    private OnScreenJoystick mScreenJoystickRight,mScreenJoystickLeft;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avtivity_control);
        initUI();
    }
    @Override
    protected void onResume() {
        Log.e(TAG,"onResume");
        super.onResume();
        initFlightControaller();
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

    private void showToast(final String s) {
    Handler handler=new Handler(Looper.getMainLooper());
    handler.post(new Runnable() {
        @Override
        public void run() {
            Toast.makeText(ControlActivity.this, s, Toast.LENGTH_LONG).show();
        }
    });
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

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    private void initUI() {
        mScreenJoystickLeft = findViewById(R.id.directionJoystickLeft);
        mScreenJoystickRight = findViewById(R.id.directionJoystickRight);
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
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

        }

    }
}
