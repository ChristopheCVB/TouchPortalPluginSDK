package com.github.ChristopheCVB.TouchPortal.model;

import com.google.gson.JsonObject;

public class TPInfo {
    public static final String TP_VERSION_STRING = "tpVersionString";
    public static final String TP_VERSION_CODE = "tpVersionCode";
    public static final String SDK_VERSION = "sdkVersion";

    public String tpVersionString;
    public long tpVersionCode;
    public long sdkVersion;

    public static TPInfo from(JsonObject jsonInfoMessage) {
        TPInfo tpInfo = new TPInfo();

        tpInfo.tpVersionString = jsonInfoMessage.has(TPInfo.TP_VERSION_STRING) ? jsonInfoMessage.get(TPInfo.TP_VERSION_STRING).getAsString() : null;
        tpInfo.tpVersionCode = jsonInfoMessage.has(TPInfo.TP_VERSION_CODE) ? jsonInfoMessage.get(TPInfo.TP_VERSION_CODE).getAsLong() : -1;
        tpInfo.sdkVersion = jsonInfoMessage.has(TPInfo.SDK_VERSION) ? jsonInfoMessage.get(TPInfo.SDK_VERSION).getAsLong() : -1;

        return tpInfo;
    }
}
