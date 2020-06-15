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

package com.github.ChristopheCVB.TouchPortal.Helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Touch Portal Plugin Received Message Helper
 */
public class ReceivedMessageHelper {
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_ACTION = "action";
    public static final String TYPE_INFO = "info";
    public static final String TYPE_LIST_CHANGE = "listChange";
    public static final String TYPE_CLOSE_PLUGIN = "closePlugin";
    public static final String PLUGIN_ID = "pluginId";
    public static final String ACTION_ID = "actionId";
    public static final String LIST_ID = "listId";
    public static final String INSTANCE_ID = "instanceId";
    public static final String VALUE = GenericHelper.VALUE;
    public static final String ACTION_DATA_VALUE = GenericHelper.VALUE;
    public static final String ACTION_DATA_ID = GenericHelper.ID;

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
     * Return true if the  received jsonMessage is an Action
     *
     * @param jsonMessage JSONObject
     * @return boolean isMessageAnAction
     */
    public static boolean isAnAction(JsonObject jsonMessage) {
        return ReceivedMessageHelper.TYPE_ACTION.equals(ReceivedMessageHelper.getType(jsonMessage));
    }

    /**
     * Retrieve the Action ID of a received Message
     *
     * @param jsonMessage {@link JsonObject}
     * @return String actionId
     */
    public static String getActionId(JsonObject jsonMessage) {
        return jsonMessage.has(ReceivedMessageHelper.ACTION_ID) ? jsonMessage.get(ReceivedMessageHelper.ACTION_ID).getAsString() : "";
    }

    /**
     * Retrieve an Action Data Value from a received Message
     *
     * @param pluginClass         Class
     * @param jsonMessage         JsonObject
     * @param actionMethodName    String
     * @param actionParameterName String
     * @return String dataValue
     */
    public static String getActionDataValue(Class<?> pluginClass, JsonObject jsonMessage, String actionMethodName, String actionParameterName) {
        return ReceivedMessageHelper.getActionDataValue(jsonMessage, DataHelper.getActionDataId(pluginClass, actionMethodName, actionParameterName));
    }

    /**
     * Retrieve an Action Data Value from a received Message
     *
     * @param jsonMessage  JsonObject
     * @param actionDataId String
     * @return String dataValue
     */
    public static String getActionDataValue(JsonObject jsonMessage, String actionDataId) {
        String dataValue = "";

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
     * Retrieve an Action Data Value from a received Message as a Double
     *
     * @param jsonMessage  JsonObject
     * @param actionDataId String
     * @return Double dataValueDouble
     */
    public static Double getActionDataValueDouble(JsonObject jsonMessage, String actionDataId) {
        return Double.valueOf(ReceivedMessageHelper.getActionDataValue(jsonMessage, actionDataId));
    }

    /**
     * Retrieve an Action Data Value from a received Message as a Long
     *
     * @param jsonMessage  JsonObject
     * @param actionDataId String
     * @return Double dataValueLong
     */
    public static Long getActionDataValueLong(JsonObject jsonMessage, String actionDataId) {
        return ReceivedMessageHelper.getActionDataValueDouble(jsonMessage, actionDataId).longValue();
    }

    /**
     * Retrieve an Action Data Value from a received Message as a Boolean
     *
     * @param jsonMessage  JsonObject
     * @param actionDataId String
     * @return Double dataValueLong
     */
    public static Boolean getActionDataValueBoolean(JsonObject jsonMessage, String actionDataId) {
        return ReceivedMessageHelper.getActionDataValue(jsonMessage, actionDataId).equals("On");
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
