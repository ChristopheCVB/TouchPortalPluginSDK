package com.github.ChristopheCVB.TouchPortal.model;

import org.json.JSONObject;

public class TPInfo {
    public static final String TP_VERSION_STRING = "tpVersionString";
    public static final String TP_VERSION_CODE = "tpVersionCode";
    public static final String SDK_VERSION = "sdkVersion";

    public String tpVersionString;
    public long tpVersionCode;
    public long sdkVersion;

    public static TPInfo from(JSONObject jsonInfoMessage) {
        TPInfo tpInfo = new TPInfo();

        tpInfo.tpVersionString = jsonInfoMessage.optString(TPInfo.TP_VERSION_STRING);
        tpInfo.tpVersionCode = jsonInfoMessage.optLong(TPInfo.TP_VERSION_CODE);
        tpInfo.sdkVersion = jsonInfoMessage.optLong(TPInfo.SDK_VERSION);

        return tpInfo;
    }
}
