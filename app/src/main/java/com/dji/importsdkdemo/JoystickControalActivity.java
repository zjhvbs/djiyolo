package com.dji.importsdkdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/*
虚拟摇杆的测试代码

在以下四个模式下：参数的解释为：
mPitch :正数为右，负数为左。
mRoll：正数为前，负数为后。

 */
public class JoystickControalActivity extends Activity implements View.OnClickListener {
    private static final String TAG = JoystickControalActivity.class.getName();
    protected TextView mConnectStatusTextView;
    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;
    private ToggleButton mBtnSimulator;
    private Button mBtnTakeOff, mBtnLand;
    private TextView mTextView;
    private OnScreenJoystick mScreenJoystickRight,mScreenJoystickLeft;
    //实现虚拟摇杆控制
    private FlightController mFlightController;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_joystick_controal);
        initUI();
    }
    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        updateTitleBar();
        initFlightController();
    }
    class SendVirtualStickDataTask extends TimerTask{

        @Override
        public void run() {
            if (mFlightController !=null){
                mFlightController.sendVirtualStickFlightControlData(new FlightControlData(mPitch,
                        mRoll, mYaw, mThrottle), new CommonCallbacks.CompletionCallback() {
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

    private void updateTitleBar() {

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
        mBtnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        mBtnTakeOff = (Button) findViewById(R.id.btn_take_off);
        mBtnLand = (Button) findViewById(R.id.btn_land);
        mBtnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);
        mTextView = (TextView) findViewById(R.id.textview_simulator);
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);

        mBtnEnableVirtualStick.setOnClickListener(this);
        mBtnDisableVirtualStick.setOnClickListener(this);
        mBtnTakeOff.setOnClickListener(this);
        mBtnLand.setOnClickListener(this);
        mBtnSimulator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mTextView.setVisibility(View.VISIBLE);
                    if (mFlightController!=null){
                        mFlightController.getSimulator().start(InitializationData.createInstance(new LocationCoordinate2D(

                                23,113
                        ),10,10), new CommonCallbacks.CompletionCallback() {

                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError==null){
                                            showToast("start Simulator success");
                                        }else{
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                }
                        );
                    }else{
                        mTextView.setVisibility(View.VISIBLE);
                        if (mFlightController!=null){
                            mFlightController.getSimulator().stop(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError==null){
                                        showToast("stop Simulator success");
                                    }else{
                                        showToast(djiError.getDescription());
                                    }
                                }
                            });
                        }
                    }
                }

            }
        });
        mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX)<0.02){
                    pX=0;
                }
                if (Math.abs(pY)<0.02){
                    pY=0;
                }
                float pitchJoyControlMaxSpeed = 10;
                float rollJoyControlMaxspeed = 10;
                mPitch =(float)(pitchJoyControlMaxSpeed*pX);
                mRoll = (float)(rollJoyControlMaxspeed*pY);
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
                /**
                *schedule
                 *  第一参数TimerTask 类，在包：import java.util.TimerTask .使用者要继承该类，并实现 public void run() 方法，因为 TimerTask 类实现了 Runnable 接口。
                 *第二参数 调用后延迟多长时间调用方法
                 * 第三参数 每隔多长时间调用方法
                 * */
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
        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:
                if (mFlightController != null){
                    mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError!=null){
                                showToast(djiError.getDescription());
                            }else{
                                showToast("Enable Virtual stcick success");
                            }
                        }
                    });
                }
                break;

            case R.id.btn_disable_virtual_stick:
                if (mFlightController!=null){
                    mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError!=null){
                                showToast(djiError.getDescription());
                            }else{
                                showToast("Disable Virtual stcick success");
                            }
                        }
                    });
                }
                break;
            case R.id.btn_take_off:
                if (mFlightController !=null){
                    mFlightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError!=null){
                                showToast(djiError.getDescription());
                            }else{
                                showToast("take off success");
                            }
                        }
                    });
                }
                break;
            case R.id.btn_land:
                final boolean confirland = true;
                if (mFlightController != null){

                    mFlightController.startLanding(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());

                                    } else {

                                        showToast("Start Landing");
                                        mFlightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (djiError != null) {
                                                    showToast(djiError.getDescription());
                                                } else {
                                                    showToast("Landing success ");
                                                }

                                            }
                                        });
                                    }
                                }
                            }
                    );
                    /*
                    if (confirland)
                    mFlightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("Landing success ");
                            }

                        }
                    });*/

                }
                break;
            default:
                break;
        }
    }
    private void initFlightController(){
        Aircraft aircraft = FPVDemoApplication.getAircraftInstance();
        if(aircraft == null || !aircraft.isConnected()){
            showToast("Disconnect");
            mFlightController = null;
            return;
        }else{
            mFlightController = aircraft.getFlightController();
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(final SimulatorState stateData) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            String yaw = String.format("%.2f", stateData.getYaw());
                            String pitch = String.format("%.2f", stateData.getPitch());
                            String roll = String.format("%.2f", stateData.getRoll());
                            String positionX = String.format("%.2f", stateData.getPositionX());
                            String positionY = String.format("%.2f", stateData.getPositionY());
                            String positionZ = String.format("%.2f", stateData.getPositionZ());

                            mTextView.setText("Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
                                    ", PosY : " + positionY +
                                    ", PosZ : " + positionZ);
                        }
                    });
                }
            });
        }

    }

    private void showToast(final String s) {
        Handler mhandle = new Handler(Looper.getMainLooper());
        mhandle.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(JoystickControalActivity.this,s,Toast.LENGTH_LONG).show();
            }
        });

    }


}
