package com.PrivacyGuard.Plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Plugin.LeakReport.LeakCategory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin {
    private final static String TAG = LocationDetection.class.getSimpleName();
    private static long MIN_TIME_INTERVAL_PASSIVE = 60000; //one minute
    private static float MIN_DISTANCE_INTERVAL = 10; // 10 meters
    private static LocationManager mLocationManager;
    private static Map<String, Location> mLocations = Collections.synchronizedMap(new HashMap<String, Location>());

    @Override
    @Nullable
    public LeakReport handleRequest(String requestStr) {
        for (Location loc : mLocations.values()) {
            //double latD = Math.round(loc.getLatitude() * 10) / 10.0;
            //double lonD = Math.round(loc.getLongitude() * 10) / 10.0;
            //String latS = String.valueOf(latD);
            //String lonS = String.valueOf(lonD);
            //if ((requestStr.contains(latS) && requestStr.contains(lonS)) || (requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", "")))) {
            //    LeakReport rpt = new LeakReport(LeakCategory.LOCATION);
            //    rpt.addLeak(new LeakInstance("location", latS + ":" + lonS));
            //    return rpt;
            //}

            double latD = ((int) (loc.getLatitude() * 10)) / 10.0;
            double lonD = ((int) (loc.getLongitude() * 10)) / 10.0;
            String latS = String.valueOf(latD);
            String lonS = String.valueOf(lonD);
            if ((requestStr.contains(latS) && requestStr.contains(lonS))) {// || (requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", "")))) {
                LeakReport rpt = new LeakReport(LeakCategory.LOCATION);
                rpt.addLeak(new LeakInstance("location", latS + ":" + lonS));
                return rpt;
            }
        }
        return null;
    }


    @Override
    public LeakReport handleResponse(String responseStr) {
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
        return null;
    }

    @Override
    public void setContext(Context context) {
        synchronized (mLocations) {
            if (mLocationManager == null) {
                mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MIN_TIME_INTERVAL_PASSIVE, MIN_DISTANCE_INTERVAL, new LocationUpdateListener(), Looper.getMainLooper());
                updateLastLocations();
                Logger.logLastLocations(mLocations, true);
            }
        }
    }

    public void updateLastLocations() {
        List<String> providers = mLocationManager.getAllProviders();
        for (String provider : providers) {
            Location loc = mLocationManager.getLastKnownLocation(provider);
            if (loc == null) continue;
            synchronized(mLocations) {
                mLocations.put(loc.getProvider(), loc);
            }
        }
    }


    class LocationUpdateListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            synchronized(mLocations) {
                mLocations.put(loc.getProvider(), loc);
            }
            Logger.logLastLocation(loc);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

}
