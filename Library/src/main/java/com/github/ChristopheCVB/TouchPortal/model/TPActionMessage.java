package com.github.ChristopheCVB.TouchPortal.model;

import com.github.ChristopheCVB.TouchPortal.Helpers.DataHelper;
import com.github.ChristopheCVB.TouchPortal.Helpers.ReceivedMessageHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

public class TPActionMessage extends TPMessage {
    public String pluginId;
    public String actionId;
    public ArrayList<Data> data;

    public static class Data {
        public String id;
        public String value;
    }

    public Object getTypedDataValue(Class<?> pluginClass, Method actionMethod, Parameter actionMethodParameter) {
        return this.getTypedDataValue(actionMethodParameter.getParameterizedType().getTypeName(), DataHelper.getActionDataId(pluginClass, actionMethod, actionMethodParameter));
    }

    public Object getTypedDataValue(String actionDataType, String actionDataId) {
        Object value = null;

        Data data = this.data.stream().filter(datum -> datum.id.equals(actionDataId)).findFirst().orElse(null);
        if (data != null) {
            value = ReceivedMessageHelper.getTypedValue(actionDataType, data.value);
        }

        return value;
    }
}
