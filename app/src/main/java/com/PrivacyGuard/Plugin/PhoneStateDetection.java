package com.PrivacyGuard.Plugin;

import android.content.Context;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import com.PrivacyGuard.Utilities.HashHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank on 23/07/14.
 */
public class PhoneStateDetection implements IPlugin {
    private static HashMap<String, String> nameofValue = new HashMap<String, String>();
    private static boolean init = false;
    private final boolean DEBUG = false;
    private final String TAG = PhoneStateDetection.class.getSimpleName();

    @Override
    @Nullable
    public LeakReport handleRequest(String request) {
        ArrayList<LeakInstance> leaks = new ArrayList<>();
        for(String key : nameofValue.keySet()) {
            if (request.contains(key)){
                leaks.add(new LeakInstance(nameofValue.get(key),key));
            }
        }
        if(leaks.isEmpty()){
            return null;
        }
        LeakReport rpt = new LeakReport(LeakReport.LeakCategory.DEVICE);
        rpt.addLeaks(leaks);
        return rpt;
    }

    @Override
    public LeakReport handleResponse(String response) {
        return null;
    }

    @Override
    public String modifyRequest(String request) {
        return request;
    }

    @Override
    public String modifyResponse(String response) {
        return response;
    }

    @Override
    public Collection<LeakDetectable> getDetectables() {
        ArrayList<LeakDetectable> detectables = new ArrayList<>();
        for (Map.Entry<String, String> e : nameofValue.entrySet()) {
            detectables.add(new LeakDetectable(
                                e.getValue(),
                                e.getKey(),
                                LeakReport.LeakCategory.DEVICE));
        }
        return detectables;
    }

    @Override
    public void setContext(Context context) {
        if(init) return;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ArrayList<String> info = new ArrayList<String>();
        String deviceID = telephonyManager.getDeviceId();
        if(deviceID != null && deviceID.length() > 0) {
            nameofValue.put(deviceID, "IMEI");
            info.add(deviceID);
        }
        String phoneNumber = telephonyManager.getLine1Number();
        if(phoneNumber != null && phoneNumber.length() > 0){
            nameofValue.put(phoneNumber, "Phone Number");
            info.add(phoneNumber);
        }
        String subscriberID = telephonyManager.getSubscriberId();
        if(subscriberID != null && subscriberID.length()>0) {
            nameofValue.put(subscriberID, "IMSI");
            info.add(subscriberID);
        }
        String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if(androidId != null && androidId.length()>0) {
            nameofValue.put(androidId, "Android ID");
            info.add(androidId);
        }
        for(String key : info) {
            nameofValue.put(HashHelpers.SHA1(key), nameofValue.get(key));
        }
        init = true;
    }
}
