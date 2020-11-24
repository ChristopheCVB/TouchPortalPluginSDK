package com.github.ChristopheCVB.TouchPortal.model;

import com.github.ChristopheCVB.TouchPortal.Helpers.ReceivedMessageHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;

public class TPInfo {
    public String status;
    public Long sdkVersion;
    public String tpVersionString;
    public Long tpVersionCode;
    public Long pluginVersion;

    public transient HashMap<String, String> settings;

    public static TPInfo from(JsonObject jsonInfoMessage) {
        TPInfo tpInfo = new Gson().fromJson(jsonInfoMessage, TPInfo.class);
        tpInfo.settings = ReceivedMessageHelper.getSettings(jsonInfoMessage.get(ReceivedMessageHelper.SETTINGS));

        return tpInfo;
    }
}
