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
    public static final String PLUGIN_ID = "pluginId";
    public static final String ACTION_ID = "actionId";
    public static final String LIST_ID = "listId";
    public static final String INSTANCE_ID = "instanceId";
    public static final String VALUE = GenericHelper.VALUE;
    public static final String ACTION_DATA_VALUE = GenericHelper.VALUE;
    public static final String ACTION_DATA_ID = GenericHelper.ID;

    /**
     * Helper Method to retrieve the Type of a ReceivedMessage
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
     * Helper Method to retrieve the Action ID of a received Message
     *
     * @param jsonMessage {@link JSONObject}
     * @return String Action ID
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
     * @param receivedActionId String
     * @param jsonMessage      JSONObject
     * @param actionFieldName  String
     * @return String dataValue
     */
    public static String getActionDataValue(String receivedActionId, JSONObject jsonMessage, String actionFieldName) {
        String dataValue = "";

        try {
            JSONArray actionData = jsonMessage.getJSONArray(ActionHelper.DATA);
            for (int actionDataIndex = 0; actionDataIndex < actionData.length(); actionDataIndex++) {
                JSONObject jsonData = actionData.getJSONObject(actionDataIndex);
                String receivedJsonDataId = jsonData.getString(ReceivedMessageHelper.ACTION_DATA_ID);
                if (receivedJsonDataId.equals(DataHelper.getActionDataId(receivedActionId, actionFieldName))) {
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
    public static boolean isMessageForPlugin(JSONObject jsonMessage, Class pluginClass) {
        boolean isMessageForPlugin = false;

        try {
            isMessageForPlugin = jsonMessage.getString(ReceivedMessageHelper.PLUGIN_ID).equals(pluginClass.getName());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return isMessageForPlugin;
    }
}
