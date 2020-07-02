package com.github.ChristopheCVB.TouchPortal.model;

import com.google.gson.JsonObject;

public class TPInfo {
    public static final String STATUS = "status";
    public static final String SDK_VERSION = "sdkVersion";
    public static final String TP_VERSION_STRING = "tpVersionString";
    public static final String TP_VERSION_CODE = "tpVersionCode";
    public static final String PLUGIN_VERSION = "pluginVersion";

    public String status;
    public Long sdkVersion;
    public String tpVersionString;
    public Long tpVersionCode;
    public Long pluginVersion;

    public static TPInfo from(JsonObject jsonInfoMessage) {
        TPInfo tpInfo = new TPInfo();

        tpInfo.status = jsonInfoMessage.has(TPInfo.STATUS) ? jsonInfoMessage.get(TPInfo.STATUS).getAsString() : null;
        tpInfo.tpVersionString = jsonInfoMessage.has(TPInfo.TP_VERSION_STRING) ? jsonInfoMessage.get(TPInfo.TP_VERSION_STRING).getAsString() : null;
        tpInfo.tpVersionCode = jsonInfoMessage.has(TPInfo.TP_VERSION_CODE) ? jsonInfoMessage.get(TPInfo.TP_VERSION_CODE).getAsLong() : null;
        tpInfo.sdkVersion = jsonInfoMessage.has(TPInfo.SDK_VERSION) ? jsonInfoMessage.get(TPInfo.SDK_VERSION).getAsLong() : null;
        tpInfo.pluginVersion = jsonInfoMessage.has(TPInfo.PLUGIN_VERSION) ? jsonInfoMessage.get(TPInfo.PLUGIN_VERSION).getAsLong() : null;

        return tpInfo;
    }
}
