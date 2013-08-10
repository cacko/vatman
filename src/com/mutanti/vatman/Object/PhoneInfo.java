package com.mutanti.vatman.Object;

import android.os.Build;
import android.telephony.TelephonyManager;

public class PhoneInfo {

    private String mIMEI;
    private String mOperator;
    private String mPhoneModel;
    private String mOSVersion;

    public PhoneInfo(TelephonyManager telephonyManager) {
        mIMEI = telephonyManager.getDeviceId();
        mOperator = telephonyManager.getNetworkOperatorName();
        mPhoneModel = Build.MODEL;
        mOSVersion = Build.VERSION.RELEASE;
    }

    public final String getIMEI() {
        return mIMEI;
    }

    public final String getOperator() {
        return mOperator;
    }

    public final String getPhoneModel() {
        return mPhoneModel;
    }

    public final String getOSVersion() {
        return mOSVersion;
    }

}
