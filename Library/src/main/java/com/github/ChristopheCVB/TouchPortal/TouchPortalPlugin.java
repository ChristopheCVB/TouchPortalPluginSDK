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

package com.github.ChristopheCVB.TouchPortal;

import com.github.ChristopheCVB.TouchPortal.Annotations.Action;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This is the class you need to extend in order to create a Touch Portal Plugin
 */
public abstract class TouchPortalPlugin {
    /**
     * Touch Portal Plugin SDK Version
     */
    protected static final String TOUCH_PORTAL_VERSION = "2.0";
    /**
     * Argument passed to the jar to start the plugin
     */
    protected static final String COMMAND_START = "start";

    protected static final String GENERIC_ID = "id";
    protected static final String GENERIC_NAME = "name";
    protected static final String GENERIC_TYPE = "type";
    protected static final String GENERIC_DESCRIPTION = "description";
    protected static final String GENERIC_VALUE = "value";
    protected static final String GENERIC_DEFAULT = "default";

    /**
     * Socket Server IP used by the Touch Portal Plugin System
     */
    private static final String SOCKET_IP = "127.0.0.1";
    /**
     * Socket Server Port used by the Touch Portal Plugin System
     */
    private static final int SOCKET_PORT = 12136;
    /**
     * This is used internally to represent the Plugin ID
     */

    private final Class<? extends TouchPortalPlugin> pluginClass;
    /**
     * Touch Portal Socket Client connection
     */
    private Socket touchPortalSocket;
    /**
     * Used Buffer to write messages to Touch Portal Plugin System
     */
    private BufferedOutputStream bufferedOutputStream;
    /**
     * Used Buffer to read messages from Touch Portal Plugin System
     */
    private BufferedReader bufferedReader;
    /**
     * Listener used for the callbacks
     */
    private TouchPortalPluginListener touchPortalPluginListener;
    /**
     * Thread that continuously reads messages coming from Touch Portal Plugin System
     */
    private final Thread listenerThread = new Thread(() -> {
        while (true) {
            try {
                String socketMessage = TouchPortalPlugin.this.bufferedReader.readLine();
                JSONObject jsonMessage = new JSONObject(socketMessage);
                if (ReceivedMessageHelper.isMessageForPlugin(jsonMessage, TouchPortalPlugin.this.pluginClass)) {
                    if (TouchPortalPlugin.this.touchPortalPluginListener != null) {
                        TouchPortalPlugin.this.touchPortalPluginListener.onReceive(jsonMessage);
                    }
                } else {
                    Thread.sleep(100);
                }
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
                TouchPortalPlugin.this.close();
                if (TouchPortalPlugin.this.touchPortalPluginListener != null) {
                    TouchPortalPlugin.this.touchPortalPluginListener.onDisconnect(ioException);
                }
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    /**
     * Constructor
     */
    protected TouchPortalPlugin() {
        this.pluginClass = this.getClass();
    }

    /**
     * Send the Pair Message
     *
     * @return boolean Pairing Message sent
     */
    private boolean sendPair() {
        boolean paired = false;
        try {
            // Send Pairing Message
            JSONObject pairingMessage = new JSONObject();
            pairingMessage.put(MessageHelper.TYPE, "pair");
            pairingMessage.put(MessageHelper.ID, this.pluginClass.getName());

            paired = this.send(pairingMessage);
            System.out.println("Pairing Message sent");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return paired;
    }

    /**
     * Close the Socket connection
     */
    private void close() {
        if (this.touchPortalSocket != null && this.touchPortalSocket.isConnected()) {
            try {
                this.bufferedOutputStream = null;
                this.bufferedReader = null;
                this.touchPortalSocket.close();
                this.touchPortalSocket = null;
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Start listening for incoming messages from the Touch Portal Plugin System
     *
     * @return boolean Started listening
     */
    private boolean listen() {
        if (this.bufferedReader == null) {
            try {
                this.bufferedReader = new BufferedReader(new InputStreamReader(this.touchPortalSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (this.bufferedReader != null && !this.listenerThread.isAlive()) {
            this.listenerThread.start();
        }

        return this.listenerThread.isAlive();
    }

    /**
     * Connect to the Touch Portal Plugin System Socket Server
     * Then, Send the Pairing message
     * And start listening for incoming messages
     *
     * @param touchPortalPluginListener TouchPortalPluginListener
     * @return boolean Is the Plugin connected, paired and started listening
     */
    public boolean connectThenPairAndListen(TouchPortalPluginListener touchPortalPluginListener) {
        boolean connected = false;

        this.touchPortalPluginListener = touchPortalPluginListener;

        try {
            this.touchPortalSocket = new Socket(InetAddress.getByName(TouchPortalPlugin.SOCKET_IP), TouchPortalPlugin.SOCKET_PORT);
            connected = true;
        } catch (IOException e) {
            e.printStackTrace();
            if (this.touchPortalPluginListener != null) {
                this.touchPortalPluginListener.onDisconnect(e);
            }
        }

        boolean paired = false;
        if (connected) {
            paired = this.sendPair();
        }

        return connected && paired && this.listen();
    }

    /**
     * Send a Message to the Touch Portal Plugin System
     *
     * @param message {@link JSONObject}
     * @return boolean Message sent
     */
    public boolean send(JSONObject message) {
        if (this.bufferedOutputStream == null) {
            try {
                this.bufferedOutputStream = new BufferedOutputStream(this.touchPortalSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        boolean sent = false;

        if (this.touchPortalSocket != null && this.touchPortalSocket.isConnected() && this.bufferedOutputStream != null) {
            try {
                this.bufferedOutputStream.write((message.toString() + "\n").getBytes());
                this.bufferedOutputStream.flush();
                sent = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sent;
    }

    /**
     * Send a Choice Update Message to the Touch Portal Plugin System
     *
     * @param id     String
     * @param values String[]
     * @return boolean Choice Update Message sent
     */
    public boolean sendChoiceUpdate(String id, String[] values) {
        boolean sent = false;
        try {
            JSONObject choiceUpdateMessage = new JSONObject()
                    .put(MessageHelper.TYPE, MessageHelper.TYPE_CHOICE_UPDATE)
                    .put(MessageHelper.ID, id)
                    .put(MessageHelper.VALUE, new JSONArray(values));
            sent = this.send(choiceUpdateMessage);
            System.out.println("Update Choices [" + id + "] sent");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sent;
    }

    /**
     * Send a Specific Choice Update Message to the Touch Portal Plugin System
     *
     * @param id         String
     * @param instanceId String
     * @param values     String[]
     * @return boolean Specific Choice Update Message sent
     */
    public boolean sendSpecificChoiceUpdate(String id, String instanceId, String[] values) {
        boolean sent = false;
        try {
            JSONObject specificChoiceUpdateMessage = new JSONObject()
                    .put(MessageHelper.TYPE, MessageHelper.TYPE_CHOICE_UPDATE)
                    .put(MessageHelper.ID, id)
                    .put(MessageHelper.INSTANCE_ID, instanceId)
                    .put(MessageHelper.VALUE, new JSONArray(values));
            sent = this.send(specificChoiceUpdateMessage);
            System.out.println("Update Choices [" + id + "] sent");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sent;
    }

    /**
     * Send a State Update Message to the Touch Portal Plugin System
     *
     * @param id    String
     * @param value String
     * @return boolean State Update Message sent
     */
    public boolean sendStateUpdate(String id, String value) {
        boolean sent = false;
        try {
            JSONObject stateUpdateMessage = new JSONObject()
                    .put(MessageHelper.TYPE, MessageHelper.TYPE_STATE_UPDATE)
                    .put(MessageHelper.ID, id)
                    .put(MessageHelper.VALUE, value);
            sent = this.send(stateUpdateMessage);
            System.out.println("Update State [" + id + "] sent");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sent;
    }

    /**
     * Interface Definition for Callbacks
     */
    public interface TouchPortalPluginListener {
        /**
         * Called when the Socket connection is lost
         *
         * @param exception {@link Exception} raised
         */
        void onDisconnect(Exception exception);

        /**
         * Called when receiving a message from the Touch Portal Plugin System
         *
         * @param message {@link JSONObject}
         */
        void onReceive(JSONObject message);
    }

    /**
     * Touch Portal Plugin Category Helper
     */
    protected static class CategoryHelper {
        public static final String ID = TouchPortalPlugin.GENERIC_ID;
        public static final String NAME = TouchPortalPlugin.GENERIC_NAME;
        public static final String IMAGE_PATH = "imagepath";
        public static final String ACTIONS = "actions";
        public static final String EVENTS = "events";
        public static final String STATES = "states";
    }

    /**
     * Touch Portal Plugin State Helper
     */
    protected static class StateHelper {
        public static final String ID = TouchPortalPlugin.GENERIC_ID;
        public static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        public static final String TYPE_CHOICE = "choice";
        public static final String TYPE_TEXT = "text";
        public static final String DESCRIPTION = "desc";
        public static final String DEFAULT = TouchPortalPlugin.GENERIC_DEFAULT;
        public static final String VALUE_CHOICES = "valueChoices";
    }

    /**
     * Touch Portal Plugin Action Helper
     */
    protected static class ActionHelper {
        public static final String ID = TouchPortalPlugin.GENERIC_ID;
        public static final String NAME = TouchPortalPlugin.GENERIC_NAME;
        public static final String PREFIX = "prefix";
        public static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        public static final String TYPE_EXECUTE = "execute";
        public static final String TYPE_COMMUNICATE = "communicate";
        public static final String EXECUTION_TYPE = "executionType";
        public static final String EXECUTION_COMMAND = "execution_cmd";
        public static final String DESCRIPTION = TouchPortalPlugin.GENERIC_DESCRIPTION;
        public static final String DATA = "data";
        public static final String DATA_ID = TouchPortalPlugin.GENERIC_ID;
        public static final String DATA_VALUE = TouchPortalPlugin.GENERIC_VALUE;
        public static final String TRY_INLINE = "tryInline";
        public static final String FORMAT = "format";

        /**
         * Retrieve the Action ID
         *
         * @param pluginClass Class
         * @param actionMethodName String
         * @return String actionId
         */
        public static String getActionId(Class<? extends TouchPortalPlugin> pluginClass, String actionMethodName) {
            String actionId = "";

            for (Method method : pluginClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Action.class) && method.getName().equals(actionMethodName)) {
                    Action action = method.getDeclaredAnnotation(Action.class);
                    actionId = pluginClass.getName() + ".basecategory.action." + (action != null && !action.id().isEmpty() ? action.id() : actionMethodName);
                }
            }

            return actionId;
        }

        /**
         * Retrieve the Action Data Id
         *
         * @param receivedActionId String
         * @param actionFieldName String
         * @return String actionDataId
         */
        public static String getActionDataId(String receivedActionId, String actionFieldName) {
            return receivedActionId + ".data." + actionFieldName;
        }
    }

    /**
     * Touch Portal Plugin Event Helper
     */
    protected static class EventHelper {
        public static final String ID = TouchPortalPlugin.GENERIC_ID;
        public static final String NAME = TouchPortalPlugin.GENERIC_NAME;
        public static final String FORMAT = "format";
        public static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        public static final String TYPE_COMMUNICATE = "communicate";
        public static final String VALUE_TYPE = "valueType";
        public static final String VALUE_CHOICES = "valueChoices";
        public static final String VALUE_STATE_ID = "valueStateId";
    }

    /**
     * Touch Portal Plugin Received Message Helper
     */
    protected static class ReceivedMessageHelper {
        public static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        public static final String TYPE_ACTION = "action";
        public static final String TYPE_LIST_CHANGE = "listChange";
        public static final String PLUGIN_ID = "pluginId";
        public static final String ACTION_ID = "actionId";
        public static final String LIST_ID = "listId";
        public static final String INSTANCE_ID = "instanceId";
        public static final String VALUE = TouchPortalPlugin.GENERIC_VALUE;

        /**
         * Helper Method to retrieve the Type of a ReceivedMessage
         *
         * @param jsonMessage {@link JSONObject}
         * @return String Message Type
         */
        public static String getType(JSONObject jsonMessage) {
            String messageType = null;

            try {
                messageType = jsonMessage.getString(MessageHelper.TYPE);
            } catch (JSONException e) {
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
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return actionId;
        }

        /**
         * Helper Method to retrieve an Action Data Value from a received Message
         *
         * @param receivedActionId String
         * @param jsonMessage JSONObject
         * @param actionFieldName String
         * @return String dataValue
         */
        public static String getActionDataValue(String receivedActionId, JSONObject jsonMessage, String actionFieldName) {
            String dataValue = "";

            try {
                JSONArray actionData = jsonMessage.getJSONArray(ActionHelper.DATA);
                for (int actionDataIndex = 0; actionDataIndex < actionData.length(); actionDataIndex++) {
                    JSONObject jsonData = actionData.getJSONObject(actionDataIndex);
                    String receivedJsonDataId = jsonData.getString(ActionHelper.DATA_ID);
                    if (receivedJsonDataId.equals(ActionHelper.getActionDataId(receivedActionId, actionFieldName))) {
                        dataValue = jsonData.getString(ActionHelper.DATA_VALUE);
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
        public static boolean isMessageForPlugin(JSONObject jsonMessage, Class<? extends TouchPortalPlugin> pluginClass) {
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

    /**
     * Touch Portal Plugin Message Helper
     */
    protected static class MessageHelper {
        public static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        public static final String TYPE_STATE_UPDATE = "stateUpdate";
        public static final String TYPE_CHOICE_UPDATE = "choiceUpdate";
        public static final String INSTANCE_ID = "instanceId";
        public static final String ID = TouchPortalPlugin.GENERIC_ID;
        public static final String VALUE = TouchPortalPlugin.GENERIC_VALUE;
    }
}
