package com.PrivacyGuard.Plugin;

import android.content.Context;

import java.util.Collection;

/**
 * Created by frank on 2014-06-23.
 */
public interface IPlugin {
    // May modify the content of the request and response
    public LeakReport handleRequest(String request);
    public LeakReport handleResponse(String response);
    public String modifyRequest(String request);
    public String modifyResponse(String response);
    public Collection<LeakDetectable> getDetectables();
    public void setContext(Context context);
}
