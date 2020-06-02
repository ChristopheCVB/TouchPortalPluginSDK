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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Touch Portal Plugin Received Message Helper
 */
public class ReceivedMessageHelper {
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_ACTION = "action";
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
     * @param jsonMessage {@link JSONObject}
     * @return String Message Type
     */
    public static String getType(JSONObject jsonMessage) {
        String messageType = null;

        try {
            messageType = jsonMessage.getString(ReceivedMessageHelper.TYPE);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return messageType;
    }

    /**
     * Return true if the  received jsonMessage is an Action
     *
     * @param jsonMessage JSONObject
     * @return boolean isMessageAnAction
     */
    public static boolean isAnAction(JSONObject jsonMessage) {
        return ReceivedMessageHelper.TYPE_ACTION.equals(ReceivedMessageHelper.getType(jsonMessage));
    }

    /**
     * Retrieve the Action ID of a received Message
     *
     * @param jsonMessage {@link JSONObject}
     * @return String actionId
     */
    public static String getActionId(JSONObject jsonMessage) {
        String actionId = null;

        try {
            actionId = jsonMessage.getString(ReceivedMessageHelper.ACTION_ID);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return actionId;
    }

    /**
     * Retrieve an Action Data Value from a received Message
     *
     * @param pluginClass         Class
     * @param jsonMessage         JSONObject
     * @param actionMethodName    String
     * @param actionParameterName String
     * @return String dataValue
     */
    public static String getActionDataValue(Class<?> pluginClass, JSONObject jsonMessage, String actionMethodName, String actionParameterName) {
        String dataValue = "";

        try {
            JSONArray actionData = jsonMessage.getJSONArray(ActionHelper.DATA);
            for (int actionDataIndex = 0; actionDataIndex < actionData.length(); actionDataIndex++) {
                JSONObject jsonData = actionData.getJSONObject(actionDataIndex);
                String receivedJsonDataId = jsonData.getString(ReceivedMessageHelper.ACTION_DATA_ID);
                if (receivedJsonDataId.equals(DataHelper.getActionDataId(pluginClass, actionMethodName, actionParameterName))) {
                    dataValue = jsonData.getString(ReceivedMessageHelper.ACTION_DATA_VALUE);
                    break;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return dataValue;
    }

    /**
     * Retrieve an Action Data Value from a received Message
     *
     * @param jsonMessage  JSONObject
     * @param actionDataId String
     * @return String dataValue
     */
    public static String getActionDataValue(JSONObject jsonMessage, String actionDataId) {
        String dataValue = "";

        try {
            JSONArray actionData = jsonMessage.getJSONArray(ActionHelper.DATA);
            for (int actionDataIndex = 0; actionDataIndex < actionData.length(); actionDataIndex++) {
                JSONObject jsonData = actionData.getJSONObject(actionDataIndex);
                String receivedJsonDataId = jsonData.getString(ReceivedMessageHelper.ACTION_DATA_ID);
                if (receivedJsonDataId.equals(actionDataId)) {
                    dataValue = jsonData.getString(ReceivedMessageHelper.ACTION_DATA_VALUE);
                    break;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return dataValue;
    }

    /**
     * Whether the received Message concerns the Plugin Class
     *
     * @param jsonMessage JSONObject
     * @param pluginClass Class
     * @return boolean isMessageForPlugin
     */
    public static boolean isMessageForPlugin(JSONObject jsonMessage, Class<?> pluginClass) {
        boolean isMessageForPlugin = false;

        try {
            isMessageForPlugin = jsonMessage.getString(ReceivedMessageHelper.PLUGIN_ID).equals(pluginClass.getName());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return isMessageForPlugin;
    }

    /**
     * Whether the received Message concerns the Plugin Class
     *
     * @param jsonMessage JSONObject
     * @param pluginId    String
     * @return boolean isMessageForPlugin
     */
    public static boolean isMessageForPlugin(JSONObject jsonMessage, String pluginId) {
        boolean isMessageForPlugin = false;

        try {
            isMessageForPlugin = jsonMessage.getString(ReceivedMessageHelper.PLUGIN_ID).equals(pluginId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return isMessageForPlugin;
    }
}
