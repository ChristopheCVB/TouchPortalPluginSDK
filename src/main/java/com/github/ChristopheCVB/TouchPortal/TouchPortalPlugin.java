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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private final Class pluginClass;
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
                if (jsonMessage.getString(Message.PLUGIN_ID).equals(TouchPortalPlugin.this.pluginClass.getName())) {
                    if (TouchPortalPlugin.this.touchPortalPluginListener != null) {
                        TouchPortalPlugin.this.touchPortalPluginListener.onReceive(jsonMessage);
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
                TouchPortalPlugin.this.close();
                if (TouchPortalPlugin.this.touchPortalPluginListener != null) {
                    TouchPortalPlugin.this.touchPortalPluginListener.onDisconnect(ioException);
                }
                break;
            } catch (Exception e) {
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
     * Helper Method to retrieve the ID of an Action
     *
     * @param jsonAction {@link JSONObject}
     * @return String Action ID
     */
    public static String getActionId(JSONObject jsonAction) {
        String actionId = null;

        try {
            actionId = jsonAction.getString(Message.ACTION_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return actionId;
    }

    /**
     * Helper Method to retrieve the Type of a Message
     *
     * @param jsonMessage {@link JSONObject}
     * @return String Message Type
     */
    public static String getMessageType(JSONObject jsonMessage) {
        String messageType = null;

        try {
            messageType = jsonMessage.getString(Message.TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return messageType;
    }

    /**
     * Connect to the Touch Portal Plugin System Socket Server
     * Then, Send the Pairing message
     * And start listening for incoming messages
     *
     * @param touchPortalPluginListener TouchPortalPluginListener
     * @return Boolean Is the Plugin connected, paired and started listening
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
            ;
        }

        return connected && paired && this.listen();
    }

    /**
     * Send the Pair Message
     *
     * @return Boolean Pairing Message sent
     */
    private boolean sendPair() {
        boolean paired = false;
        try {

            // Send Pairing Message
            JSONObject pairingMessage = new JSONObject();
            pairingMessage.put(Message.TYPE, "pair");
            pairingMessage.put(Message.ID, this.pluginClass.getName());

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
     * Send a Message to the Touch Portal Plugin System
     *
     * @param message {@link JSONObject}
     * @return Boolean Message sent
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
     * @return Boolean Choice Update Message sent
     */
    public boolean sendChoiceUpdate(String id, String[] values) {
        boolean sent = false;
        try {
            JSONObject choiceUpdateMessage = new JSONObject()
                    .put(Message.TYPE, Message.TYPE_CHOICE_UPDATE)
                    .put(Message.ID, id)
                    .put(Message.VALUE, new JSONArray(values));
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
     * @return Boolean Specific Choice Update Message sent
     */
    public boolean sendSpecificChoiceUpdate(String id, String instanceId, String[] values) {
        boolean sent = false;
        try {
            JSONObject specificChoiceUpdateMessage = new JSONObject()
                    .put(Message.TYPE, Message.TYPE_CHOICE_UPDATE)
                    .put(Message.ID, id)
                    .put(Message.INSTANCE_ID, instanceId)
                    .put(Message.VALUE, new JSONArray(values));
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
     * @return Boolean State Update Message sent
     */
    public boolean sendStateUpdate(String id, String value) {
        boolean sent = false;
        try {
            JSONObject stateUpdateMessage = new JSONObject()
                    .put(Message.TYPE, Message.TYPE_STATE_UPDATE)
                    .put(Message.ID, id)
                    .put(Message.VALUE, value);
            sent = this.send(stateUpdateMessage);
            System.out.println("Update State [" + id + "] sent");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sent;
    }

    /**
     * Start listening for incoming messages from the Touch Portal Plugin System
     *
     * @return Boolean Started listening
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
    protected class Category {
        protected static final String ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String NAME = TouchPortalPlugin.GENERIC_NAME;
        protected static final String IMAGE_PATH = "imagepath";
        protected static final String ACTIONS = "actions";
        protected static final String EVENTS = "events";
        protected static final String STATES = "states";
    }

    /**
     * Touch Portal Plugin State Helper
     */
    protected class State {
        protected static final String ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        protected static final String TYPE_CHOICE = "choice";
        protected static final String TYPE_TEXT = "text";
        protected static final String DESCRIPTION = "desc";
        protected static final String DEFAULT = TouchPortalPlugin.GENERIC_DEFAULT;
        protected static final String VALUE_CHOICES = "valueChoices";
    }

    /**
     * Touch Portal Plugin Action Helper
     */
    protected class Action {
        protected static final String ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String NAME = TouchPortalPlugin.GENERIC_NAME;
        protected static final String PREFIX = "prefix";
        protected static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        protected static final String TYPE_EXECUTE = "execute";
        protected static final String TYPE_COMMUNICATE = "communicate";
        protected static final String EXECUTION_TYPE = "executionType";
        protected static final String EXECUTION_COMMAND = "execution_cmd";
        protected static final String DESCRIPTION = TouchPortalPlugin.GENERIC_DESCRIPTION;
        protected static final String DATA = "data";
        protected static final String DATA_ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String DATA_VALUE = TouchPortalPlugin.GENERIC_VALUE;
        protected static final String TRY_INLINE = "tryInline";
        protected static final String FORMAT = "format";
    }

    /**
     * Touch Portal Plugin Event Helper
     */
    protected class Event {
        protected static final String ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String NAME = TouchPortalPlugin.GENERIC_NAME;
        protected static final String FORMAT = "format";
        protected static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        protected static final String TYPE_COMMUNICATE = "communicate";
        protected static final String VALUE_TYPE = "valueType";
        protected static final String VALUE_CHOICES = "valueChoices";
        protected static final String VALUE_STATE_ID = "valueStateId";
    }

    /**
     * Touch Portal Plugin Message Helper
     */
    protected class Message {
        protected static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        protected static final String TYPE_ACTION = "action";
        protected static final String TYPE_STATE_UPDATE = "stateUpdate";
        protected static final String TYPE_CHOICE_UPDATE = "choiceUpdate";
        protected static final String TYPE_LIST_CHANGE = "listChange";
        protected static final String PLUGIN_ID = "pluginId";
        protected static final String ACTION_ID = "actionId";
        protected static final String LIST_ID = "listId";
        protected static final String INSTANCE_ID = "instanceId";
        protected static final String ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String VALUE = TouchPortalPlugin.GENERIC_VALUE;
    }
}
