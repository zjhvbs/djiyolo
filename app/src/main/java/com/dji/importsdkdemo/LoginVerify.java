package com.dji.importsdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import dji.common.error.DJIError;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class LoginVerify extends AppCompatActivity implements View.OnClickListener {
    private  static final String TAG = LoginVerify.class.getName();
    protected Button loginBtn;
    protected Button logoutBtn;
    protected TextView bindingStateTV;
    protected TextView appActivitionStateTV;
    private AppActivationManager appActivationManager;
    private AppActivationState.AppActivationStateListener activationStateListener;
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activtiy_login);

        initUI();
        initData();

    }

    private void initData() {
        setUpListener();

        appActivationManager = DJISDKManager.getInstance().getAppActivationManager();

        if(appActivationManager != null){
            appActivationManager.addAppActivationStateListener(activationStateListener);
            appActivationManager.addAircraftBindingStateListener(bindingStateListener);
            LoginVerify.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appActivitionStateTV.setText(""+appActivationManager.getAppActivationState());
                    bindingStateTV.setText(""+appActivationManager.getAircraftBindingState());
                }
            });
        }
    }

    private void initUI() {
        bindingStateTV = findViewById(R.id.tv_binding_state_info);
        appActivitionStateTV = findViewById(R.id.tv_activation_state_info);
        loginBtn = findViewById(R.id.btn_login);
        logoutBtn = findViewById(R.id.btn_logout);
        loginBtn.setOnClickListener(this);
        logoutBtn.setOnClickListener(this);
    }



    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        setUpListener();
        super.onResume();
    }

    private void setUpListener() {
        activationStateListener = new AppActivationState.AppActivationStateListener() {
            @Override
            public void onUpdate(final AppActivationState appActivationState) {
                LoginVerify.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appActivitionStateTV.setText(""+appActivationState);                 }
                });
            }
        };
        bindingStateListener = new AircraftBindingState.AircraftBindingStateListener() {
            @Override
            public void onUpdate(final AircraftBindingState aircraftBindingState) {
                LoginVerify.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bindingStateTV.setText(""+aircraftBindingState);
                    }
                });
            }
        };
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
        tearDownListener();
        super.onDestroy();
    }

    private void tearDownListener() {
        if (activationStateListener != null) {
            appActivationManager.removeAppActivationStateListener(activationStateListener);
            LoginVerify.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appActivitionStateTV.setText("Unknown");
                }

            });

        }
        if (bindingStateListener !=null) {
            appActivationManager.removeAircraftBindingStateListener(bindingStateListener);
            LoginVerify.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bindingStateTV.setText("Unknown");
                }
            });
        }

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_login:{
                //showToast("loginclick");
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
                Intent intent = new Intent(LoginVerify.this,ControlActivity.class);
                startActivity(intent);
            }

            @Override
            public void onFailure(DJIError djiError) {
                showToast("login,Fauile"+djiError.getDescription());
            }
        });
    }

    private void showToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginVerify.this,s,Toast.LENGTH_LONG).show();
            }
        });
    }
}
