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

import com.github.ChristopheCVB.TouchPortal.Helpers.ReceivedMessageHelper;
import com.github.ChristopheCVB.TouchPortal.Helpers.SentMessageHelper;
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
            pairingMessage.put(SentMessageHelper.TYPE, "pair");
            pairingMessage.put(SentMessageHelper.ID, this.pluginClass.getName());

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
     * Connect to the Touch Portal Plugin System Socket Server.
     * Then, Send the Pairing message.
     * And start listening for incoming messages.
     *
     * @param touchPortalPluginListener {@link TouchPortalPluginListener}
     * @return boolean Is the Plugin connected, paired and listening
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
                    .put(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CHOICE_UPDATE)
                    .put(SentMessageHelper.ID, id)
                    .put(SentMessageHelper.VALUE, new JSONArray(values));
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
                    .put(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CHOICE_UPDATE)
                    .put(SentMessageHelper.ID, id)
                    .put(SentMessageHelper.INSTANCE_ID, instanceId)
                    .put(SentMessageHelper.VALUE, new JSONArray(values));
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
                    .put(SentMessageHelper.TYPE, SentMessageHelper.TYPE_STATE_UPDATE)
                    .put(SentMessageHelper.ID, id)
                    .put(SentMessageHelper.VALUE, value);
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
}
