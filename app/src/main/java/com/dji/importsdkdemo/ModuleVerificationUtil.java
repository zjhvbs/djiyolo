package com.dji.importsdkdemo;

public class ModuleVerificationUtil {
    public static boolean isProductModuleAvailable() {
        return (null != FPVDemoApplication.getmProductInstance());
    }
    public static boolean isGimbalModuleAvailable() {
        return isProductModuleAvailable() && (null != FPVDemoApplication.getmProductInstance().getGimbal());
    }

}
