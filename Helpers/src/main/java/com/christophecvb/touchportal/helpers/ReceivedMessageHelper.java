/*
 * Touch Portal Plugin SDK
 *
 * Copyright 2020 Christophe Carvalho Vilas-Boas
 * christophe.carvalhovilasboas@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.christophecvb.touchportal.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;

/**
 * Touch Portal Plugin Received Message Helper
 */
public class ReceivedMessageHelper {
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_ACTION = "action";
    public static final String TYPE_HOLD_DOWN = "down";
    public static final String TYPE_HOLD_UP = "up";
    public static final String TYPE_INFO = "info";
    public static final String TYPE_LIST_CHANGE = "listChange";
    public static final String TYPE_CLOSE_PLUGIN = "closePlugin";
    public static final String TYPE_BROADCAST = "broadcast";
    public static final String TYPE_SETTINGS = "settings";
    public static final String PLUGIN_ID = "pluginId";
    public static final String ACTION_ID = "actionId";
    public static final String LIST_ID = "listId";
    public static final String INSTANCE_ID = "instanceId";
    public static final String VALUE = GenericHelper.VALUE;
    public static final String ACTION_DATA_VALUE = GenericHelper.VALUE;
    public static final String ACTION_DATA_ID = GenericHelper.ID;
    public static final String EVENT = "event";
    public static final String EVENT_PAGE_CHANGE = "pageChange";
    public static final String PAGE_NAME = "pageName";
    public static final String SETTINGS = "settings";
    public static final String VALUES = "values";

    /**
     * Retrieve the Type of a ReceivedMessage
     *
     * @param jsonMessage {@link JsonObject}
     * @return String Message Type
     */
    public static String getType(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.TYPE) ? jsonMessage.get(ReceivedMessageHelper.TYPE).getAsString() : null;
    }

    /**
     * Return true if the received jsonMessage is an Action
     *
     * @param jsonMessage JSONObject
     * @return boolean isMessageAnAction
     */
    public static boolean isTypeAction(JsonObject jsonMessage) {
        return ReceivedMessageHelper.TYPE_ACTION.equals(ReceivedMessageHelper.getType(jsonMessage));
    }

    /**
     * Return true if the received jsonMessage is a List Change
     *
     * @param jsonMessage JSONObject
     * @return boolean isMessageAListChange
     */
    public static boolean isTypeListChange(JsonObject jsonMessage) {
        return ReceivedMessageHelper.TYPE_LIST_CHANGE.equals(ReceivedMessageHelper.getType(jsonMessage));
    }

    /**
     * Retrieve the Action ID of a received Message
     *
     * @param jsonMessage {@link JsonObject}
     * @return String actionId
     */
    public static String getActionId(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.ACTION_ID) ? jsonMessage.get(ReceivedMessageHelper.ACTION_ID).getAsString() : null;
    }

    /**
     * Retrieve the List ID of a received Message
     *
     * @param jsonMessage {@link JsonObject}
     * @return String listId
     */
    public static String getListId(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.LIST_ID) ? jsonMessage.get(ReceivedMessageHelper.LIST_ID).getAsString() : null;
    }

    /**
     * Retrieve the Instance ID of a received Message
     *
     * @param jsonMessage {@link JsonObject}
     * @return String instanceId
     */
    public static String getListInstanceId(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.INSTANCE_ID) ? jsonMessage.get(ReceivedMessageHelper.INSTANCE_ID).getAsString() : null;
    }

    /**
     * Retrieve the List Value of a received Message
     *
     * @param jsonMessage {@link JsonObject}
     * @return String instanceId
     */
    public static String getListValue(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.VALUE) ? jsonMessage.get(ReceivedMessageHelper.VALUE).getAsString() : null;
    }

    /**
     * Retrieve an Action Data Value from a received Message
     *
     * @param jsonMessage  JsonObject
     * @param actionDataId String
     * @return String dataValue
     */
    public static String getActionDataValue(JsonObject jsonMessage, String actionDataId) {
        String dataValue = null;

        if (jsonMessage.has(ActionHelper.DATA)) {
            JsonArray actionData = jsonMessage.getAsJsonArray(ActionHelper.DATA);
            for (int actionDataIndex = 0; actionDataIndex < actionData.size(); actionDataIndex++) {
                JsonObject jsonData = actionData.get(actionDataIndex).getAsJsonObject();
                String receivedJsonDataId = jsonData.has(ReceivedMessageHelper.ACTION_DATA_ID) ? jsonData.get(ReceivedMessageHelper.ACTION_DATA_ID).getAsString() : "";
                if (receivedJsonDataId.equals(actionDataId)) {
                    dataValue = jsonData.get(ReceivedMessageHelper.ACTION_DATA_VALUE).getAsString();
                    break;
                }
            }
        }

        return dataValue;
    }

    /**
     * Retrieve an Action Data Value from a received Message
     *
     * @param jsonMessage         JsonObject
     * @param pluginClass         Class
     * @param actionMethodName    String
     * @param actionParameterName String
     * @return String dataValue
     */
    public static String getActionDataValue(JsonObject jsonMessage, Class<?> pluginClass, String actionMethodName, String actionParameterName) {
        return ReceivedMessageHelper.getActionDataValue(jsonMessage, DataHelper.getActionDataId(pluginClass, actionMethodName, actionParameterName));
    }

    /**
     * Retrieve an Action Data Value from a received Message for a specific Parameter
     *
     * @param jsonMessage      JsonObject
     * @param parameterDataId  String
     * @param parameterRawType String
     * @return Object actionDataValue
     */
    protected static Object getTypedActionDataValue(JsonObject jsonMessage, String parameterDataId, String parameterRawType) {
        String argumentValue = ReceivedMessageHelper.getActionDataValue(jsonMessage, parameterDataId);
        return ReceivedMessageHelper.getTypedValue(parameterRawType, argumentValue);
    }

    /**
     * Retrieve an Action Data Value from a received Message for a specific Parameter
     *
     * @param rawType  String
     * @param rawValue String
     * @return Object typedValue
     */
    public static Object getTypedValue(String rawType, String rawValue) {
        Object argumentValue = rawValue;
        if (rawValue != null) {
            switch (rawType) {
                case "short":
                case "java.lang.Short":
                    argumentValue = ReceivedMessageHelper.getValueAsDouble(rawValue);
                    if (argumentValue != null) {
                        argumentValue = ((Double) argumentValue).shortValue();
                    }
                    break;

                case "int":
                case "java.lang.Integer":
                    argumentValue = ReceivedMessageHelper.getValueAsDouble(rawValue);
                    if (argumentValue != null) {
                        argumentValue = ((Double) argumentValue).intValue();
                    }
                    break;

                case "float":
                case "java.lang.Float":
                    argumentValue = ReceivedMessageHelper.getValueAsDouble(rawValue);
                    if (argumentValue != null) {
                        argumentValue = ((Double) argumentValue).floatValue();
                    }
                    break;

                case "double":
                case "java.lang.Double":
                    argumentValue = ReceivedMessageHelper.getValueAsDouble(rawValue);
                    break;

                case "long":
                case "java.lang.Long":
                    argumentValue = ReceivedMessageHelper.getValueAsDouble(rawValue);
                    if (argumentValue != null) {
                        argumentValue = ((Double) argumentValue).longValue();
                    }
                    break;

                case "boolean":
                case "java.lang.Boolean":
                    argumentValue = ReceivedMessageHelper.getValueAsBoolean(rawValue);
                    break;

                case "java.lang.String[]":
                    argumentValue = new String[]{(String) argumentValue};
                    break;

                case "java.io.File":
                    argumentValue = new File((String) argumentValue);
                    break;
            }
        }
        return argumentValue;
    }

    /**
     * Retrieve an Action Data Value from a received Message for a specific Parameter
     *
     * @param jsonMessage JsonObject
     * @param pluginClass Class
     * @param method      Method
     * @param parameter   Parameter
     * @return Object actionDataValue
     */
    public static Object getTypedActionDataValue(JsonObject jsonMessage, Class<?> pluginClass, Method method, Parameter parameter) {
        String parameterDataId = DataHelper.getActionDataId(pluginClass, method, parameter);
        return ReceivedMessageHelper.getTypedActionDataValue(jsonMessage, parameterDataId, parameter.getParameterizedType().getTypeName());
    }

    /**
     * Retrieve an Action Data Value from a received Message as a Double
     *
     * @param jsonMessage  JsonObject
     * @param actionDataId String
     * @return Double dataValueDouble
     */
    public static Double getActionDataValueDouble(JsonObject jsonMessage, String actionDataId) {
        return ReceivedMessageHelper.getValueAsDouble(ReceivedMessageHelper.getActionDataValue(jsonMessage, actionDataId));
    }

    /**
     * Retrieve an Action Data Value from a received Message as a Double
     *
     * @param rawValue String
     * @return Double valueDouble
     */
    protected static Double getValueAsDouble(String rawValue) {
        Double valueDouble = null;
        if (rawValue != null) {
            try {
                valueDouble = Double.valueOf(rawValue);
            }
            catch (NumberFormatException ignored) {}
        }
        return valueDouble;
    }

    /**
     * Retrieve an Action Data Value from a received Message as a Boolean
     *
     * @param jsonMessage  JsonObject
     * @param actionDataId String
     * @return Double dataValueLong
     */
    public static Boolean getActionDataValueBoolean(JsonObject jsonMessage, String actionDataId) {
        return ReceivedMessageHelper.getValueAsBoolean(ReceivedMessageHelper.getActionDataValue(jsonMessage, actionDataId));
    }

    /**
     * Retrieve an Action Data Value from a received Message as a Boolean
     *
     * @param rawValue String
     * @return Boolean valueBoolean
     */
    public static Boolean getValueAsBoolean(String rawValue) {
        return "On".equals(rawValue);
    }

    /**
     * Retrieve the Event Value from a received Message
     *
     * @param jsonMessage JsonObject
     * @return String event
     */
    public static String getBroadcastEvent(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.EVENT) ? jsonMessage.get(ReceivedMessageHelper.EVENT).getAsString() : null;
    }

    /**
     * Retrieve the Page Name Value from a received Message
     *
     * @param jsonMessage JsonObject
     * @return String pageName
     */
    public static String getBroadcastPageName(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.PAGE_NAME) ? jsonMessage.get(ReceivedMessageHelper.PAGE_NAME).getAsString() : null;
    }

    /**
     * Retrieve the Settings from a received Message
     *
     * @param jsonSettingsElement JsonElement
     * @return HashMap<String, String> settings
     */
    public static HashMap<String, String> getSettings(JsonElement jsonSettingsElement) {
        HashMap<String, String> settings = new HashMap<>();

        if (jsonSettingsElement != null && jsonSettingsElement.isJsonArray()) {
            JsonArray jsonSettings = jsonSettingsElement.getAsJsonArray();
            for (JsonElement jsonSettingElement : jsonSettings) {
                JsonObject jsonSetting = jsonSettingElement.getAsJsonObject();
                jsonSetting.keySet().forEach(key -> settings.put(key, jsonSetting.get(key).getAsString()));
            }
        }

        return settings;
    }

    /**
     * Whether the received Message concerns the Plugin Class
     *
     * @param jsonMessage JsonObject
     * @param pluginClass Class
     * @return boolean isMessageForPlugin
     */
    public static boolean isMessageForPlugin(JsonObject jsonMessage, Class<?> pluginClass) {
        return ReceivedMessageHelper.isMessageForPlugin(jsonMessage, pluginClass.getName());
    }

    /**
     * Whether the received Message concerns the Plugin Class
     *
     * @param jsonMessage JsonObject
     * @param pluginId    String
     * @return boolean isMessageForPlugin
     */
    public static boolean isMessageForPlugin(JsonObject jsonMessage, String pluginId) {
        return jsonMessage.has(ReceivedMessageHelper.PLUGIN_ID) && jsonMessage.get(ReceivedMessageHelper.PLUGIN_ID).getAsString().equals(pluginId);
    }
}
