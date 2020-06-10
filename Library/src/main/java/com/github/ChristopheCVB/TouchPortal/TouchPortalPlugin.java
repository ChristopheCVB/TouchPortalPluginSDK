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

import com.github.ChristopheCVB.TouchPortal.Helpers.PluginHelper;
import com.github.ChristopheCVB.TouchPortal.Helpers.ReceivedMessageHelper;
import com.github.ChristopheCVB.TouchPortal.Helpers.SentMessageHelper;
import com.github.ChristopheCVB.TouchPortal.Helpers.StateHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is the class you need to extend in order to create a Touch Portal Plugin
 */
public abstract class TouchPortalPlugin {
    /**
     * Touch Portal Plugin SDK Version
     */
    protected static final int TOUCH_PORTAL_SDK_VERSION = PluginHelper.TOUCH_PORTAL_PLUGIN_VERSION;
    /**
     * Socket Server IP used by the Touch Portal Plugin System
     */
    private static final String SOCKET_IP = "127.0.0.1";
    /**
     * Socket Server Port used by the Touch Portal Plugin System
     */
    private static final int SOCKET_PORT = 12136;

    /**
     * Actual Plugin Class
     * <p>
     * This is used internally to represent the Plugin ID
     * </p>
     */
    private final Class<? extends TouchPortalPlugin> pluginClass;
    /**
     * Touch Portal Plugin Folder passed at start command
     */
    private final String touchPortalPluginFolder;
    /**
     * Touch Portal Socket Client connection
     */
    private Socket touchPortalSocket;
    /**
     * Writer used to send messages to the Touch Portal Plugin System
     */
    private PrintWriter printWriter;
    /**
     * Buffer used to read messages from Touch Portal Plugin System
     */
    private BufferedReader bufferedReader;
    /**
     * Properties
     */
    private Properties properties;
    /**
     * Properties File
     */
    private File propertiesFile;
    /**
     * Listener used for the callbacks
     */
    private TouchPortalPluginListener touchPortalPluginListener;
    /**
     * Thread that continuously reads messages coming from Touch Portal Plugin System
     */
    private Thread listenerThread;
    /**
     * Last sent States HashMap (Key, Value)
     */
    private final HashMap<String, String> currentStates = new HashMap<>();
    /**
     * Executor Service for callbacks
     */
    private final ExecutorService callbacksExecutor = Executors.newFixedThreadPool(5);

    /**
     * Constructor
     *
     * @param args String[]
     */
    protected TouchPortalPlugin(String[] args) {
        this.touchPortalPluginFolder = args[1].trim();
        this.pluginClass = this.getClass();
    }

    /**
     * Create a new Listener Thread
     *
     * @return Thread listenerThread
     */
    private Thread createListenerThread() {
        return new Thread(() -> {
            while (true) {
                try {
                    if (TouchPortalPlugin.this.touchPortalSocket == null) {
                        throw new IOException("Can't Access Socket");
                    }
                    if (TouchPortalPlugin.this.bufferedReader == null) {
                        TouchPortalPlugin.this.bufferedReader = new BufferedReader(new InputStreamReader(TouchPortalPlugin.this.touchPortalSocket.getInputStream()));
                    }
                    String socketMessage = TouchPortalPlugin.this.bufferedReader.readLine();
                    if (socketMessage != null && !socketMessage.isEmpty()) {
                        JSONObject jsonMessage = new JSONObject(socketMessage);
                        if (ReceivedMessageHelper.isMessageForPlugin(jsonMessage, TouchPortalPlugin.this.pluginClass)) {
                            String messageType = ReceivedMessageHelper.getType(jsonMessage);
                            if (ReceivedMessageHelper.TYPE_CLOSE_PLUGIN.equals(messageType)) {
                                System.out.println("Close Message Received");
                                TouchPortalPlugin.this.close(null);
                                break;
                            }
                            else {
                                System.out.println("Message Received");
                                // TODO: Automatically Call Actions Methods (This require to Annotate an Interface that would be passed to the SDK)
                                if (TouchPortalPlugin.this.touchPortalPluginListener != null) {
                                    TouchPortalPlugin.this.callbacksExecutor.submit(() -> TouchPortalPlugin.this.touchPortalPluginListener.onReceive(jsonMessage));
                                }
                            }
                        }
                    }
                }
                catch (IOException ioException) {
                    TouchPortalPlugin.this.close(ioException);
                    break;
                }
                catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
        });
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
            pairingMessage.put(SentMessageHelper.TYPE, SentMessageHelper.TYPE_PAIR);
            pairingMessage.put(SentMessageHelper.ID, PluginHelper.getPluginId(this.pluginClass));

            paired = this.send(pairingMessage);
            System.out.println("Pairing Message Sent");
        }
        catch (JSONException ignored) {
        }
        return paired;
    }

    /**
     * Close the Socket connection
     * <p>
     * The {@link TouchPortalPluginListener}.onDisconnect method will be called if the connection was alive.
     * </p>
     *
     * @param exception Exception
     */
    public void close(Exception exception) {
        try {
            this.storeProperties();
        }
        catch (IOException ignored) {}


        System.out.println("closing");
        if (!this.callbacksExecutor.isShutdown()) {
            this.callbacksExecutor.shutdownNow();
        }
        if (this.printWriter != null) {
            this.printWriter.close();
            this.printWriter = null;
        }
        if (this.bufferedReader != null) {
            try {
                this.bufferedReader.close();
            }
            catch (IOException ignored) {}
            this.bufferedReader = null;
        }
        if (this.touchPortalSocket != null) {
            try {
                this.touchPortalSocket.close();
            }
            catch (IOException ignored) {}
            this.touchPortalSocket = null;

            if (this.touchPortalPluginListener != null) {
                this.touchPortalPluginListener.onDisconnect(exception);
            }
        }

        this.listenerThread = null;
    }

    /**
     * Connect to the Touch Portal Plugin System Socket Server.
     * Then, Send the Pairing message.
     *
     * @return boolean pluginIsConnectedAndPaired
     */
    private boolean connectThenPair() {
        System.out.println("connectingAndPairing");
        boolean connectedAndPaired = this.isConnected();

        if (!connectedAndPaired) {
            try {
                this.touchPortalSocket = new Socket(InetAddress.getByName(TouchPortalPlugin.SOCKET_IP), TouchPortalPlugin.SOCKET_PORT);
                connectedAndPaired = this.isConnected() && this.sendPair();
            }
            catch (IOException ignored) {}
        }

        return connectedAndPaired;
    }

    /**
     * Start listening for incoming messages from the Touch Portal Plugin System
     *
     * @return boolean Started listening
     */
    private boolean listen() {
        System.out.println("Start listening");
        if (this.listenerThread == null) {
            this.listenerThread = this.createListenerThread();
        }
        if (!this.listenerThread.isAlive()) {
            this.listenerThread.start();
        }

        return this.listenerThread.isAlive();
    }

    /**
     * Connect to the Touch Portal Plugin System Socket Server.
     * Then, Send the Pairing message.
     * Finally, start listening for incoming messages.
     *
     * @param touchPortalPluginListener {@link TouchPortalPluginListener}
     * @return boolean pluginIsConnectedPairedAndListening
     */
    public boolean connectThenPairAndListen(TouchPortalPluginListener touchPortalPluginListener) {
        this.touchPortalPluginListener = touchPortalPluginListener;

        return this.connectThenPair() && this.listen();
    }

    /**
     * Internal Send a Message to the Touch Portal Plugin System
     *
     * @param message {@link JSONObject}
     * @return boolean isMessageSent
     */
    private boolean send(JSONObject message) {
        boolean sent = false;
        if (this.isConnected()) {
            try {
                if (this.printWriter == null) {
                    this.printWriter = new PrintWriter(this.touchPortalSocket.getOutputStream(), true);
                }
                this.printWriter.println(message);

                sent = true;
            }
            catch (IOException ignored) {}
        }

        return sent;
    }

    /**
     * Send a Choice Update Message to the Touch Portal Plugin System
     *
     * @param categoryId     String
     * @param stateFieldName String
     * @param values         String[]
     * @return boolean choiceUpdateMessageSent
     */
    public boolean sendChoiceUpdate(String categoryId, String stateFieldName, String[] values) {
        String stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateFieldName);
        return this.sendChoiceUpdate(stateId, values);
    }

    /**
     * Send a Choice Update Message to the Touch Portal Plugin System
     *
     * @param listId String
     * @param values String[]
     * @return boolean choiceUpdateMessageSent
     */
    public boolean sendChoiceUpdate(String listId, String[] values) {
        boolean sent = false;
        try {
            JSONObject choiceUpdateMessage = new JSONObject()
                    .put(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CHOICE_UPDATE)
                    .put(SentMessageHelper.ID, listId)
                    .put(SentMessageHelper.VALUE, new JSONArray(values));
            sent = this.send(choiceUpdateMessage);
            System.out.println("Update Choices [" + listId + "] Sent [" + sent + "]");
        }
        catch (JSONException ignored) {}

        return sent;
    }

    /**
     * Send a Specific Choice Update Message to the Touch Portal Plugin System
     *
     * @param categoryId     String
     * @param stateFieldName String
     * @param instanceId     String
     * @param values         String[]
     * @return boolean specificChoiceUpdateMessageSent
     */
    public boolean sendSpecificChoiceUpdate(String categoryId, String stateFieldName, String instanceId, String[] values) {
        String stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateFieldName);
        return this.sendSpecificChoiceUpdate(stateId, instanceId, values);
    }

    /**
     * Send a Specific Choice Update Message to the Touch Portal Plugin System
     *
     * @param choiceId   String
     * @param instanceId String
     * @param values     String[]
     * @return boolean specificChoiceUpdateMessageSent
     */
    public boolean sendSpecificChoiceUpdate(String choiceId, String instanceId, String[] values) {
        boolean sent = false;
        try {
            JSONObject specificChoiceUpdateMessage = new JSONObject()
                    .put(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CHOICE_UPDATE)
                    .put(SentMessageHelper.ID, choiceId)
                    .put(SentMessageHelper.INSTANCE_ID, instanceId)
                    .put(SentMessageHelper.VALUE, new JSONArray(values));
            sent = this.send(specificChoiceUpdateMessage);
            System.out.println("Update Specific Choices [" + choiceId + "] Sent [" + sent + "]");
        }
        catch (JSONException ignored) {}

        return sent;
    }

    /**
     * Send a State Update Message to the Touch Portal Plugin System
     *
     * @param categoryId     String
     * @param stateFieldName String
     * @param value          String
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendStateUpdate(String categoryId, String stateFieldName, String value) {
        String stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateFieldName);
        return this.sendStateUpdate(stateId, value);
    }

    /**
     * Send a State Update Message to the Touch Portal Plugin System
     *
     * @param stateId String
     * @param value   String
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendStateUpdate(String stateId, String value) {
        boolean sent = false;
        boolean isLastValue = false;
        if (stateId != null && !stateId.isEmpty() && value != null && !value.isEmpty()) {
            if (this.currentStates.containsKey(stateId) && this.currentStates.get(stateId).equals(value)) {
                isLastValue = true;
            }
            else {
                try {
                    JSONObject stateUpdateMessage = new JSONObject()
                            .put(SentMessageHelper.TYPE, SentMessageHelper.TYPE_STATE_UPDATE)
                            .put(SentMessageHelper.ID, stateId)
                            .put(SentMessageHelper.VALUE, value);
                    sent = this.send(stateUpdateMessage);
                    if (sent) {
                        this.currentStates.put(stateId, value);
                    }
                }
                catch (JSONException ignored) {}
            }
        }
        System.out.println("Update State [" + stateId + "] to Value [" + value + "] Sent [" + sent + "] Is Last Value [" + isLastValue + "]");

        return sent;
    }

    /**
     * Is the Plugin connected to the Touch Portal Plugin System
     *
     * @return isPluginConnected
     */
    public boolean isConnected() {
        return this.touchPortalSocket != null && this.touchPortalSocket.isConnected();
    }

    /**
     * Get a Resource File that is stored in the Plugin directory
     *
     * @param resourcePluginFilePath String
     * @return File resourceFile
     */
    public File getResourceFile(String resourcePluginFilePath) {
        return Paths.get(this.touchPortalPluginFolder + this.pluginClass.getSimpleName() + "/" + resourcePluginFilePath).toFile();
    }

    /**
     * Loads a Properties file from the Plugin directory
     *
     * @param propertiesPluginFilePath String - Relative path of the properties File
     * @throws IOException ioException
     */
    public void loadProperties(String propertiesPluginFilePath) throws IOException {
        this.propertiesFile = this.getResourceFile(propertiesPluginFilePath);
        this.loadProperties();
    }

    /**
     * Internal Load the set Properties File
     *
     * @throws IOException ioException
     */
    private void loadProperties() throws IOException {
        if (this.propertiesFile != null) {
            FileInputStream fis = new FileInputStream(this.propertiesFile.getAbsolutePath());
            this.properties = new Properties();
            this.properties.load(fis);
        }
    }

    /**
     * Reloads the Properties File previously set
     *
     * @throws IOException ioException
     */
    public void reloadProperties() throws IOException {
        this.loadProperties();
    }

    /**
     * Read a property by the key
     *
     * @param key String
     * @return String value
     */
    public String getProperty(String key) {
        String value = null;

        if (this.properties != null) {
            value = this.properties.getProperty(key);
        }

        return value;
    }

    /**
     * Set a property key to the specified value
     *
     * @param key   String
     * @param value String
     * @return String oldKey - If present
     */
    public String setProperty(String key, String value) {
        String oldValue = null;

        if (this.properties != null) {
            oldValue = (String) this.properties.setProperty(key, value);
        }

        return oldValue;
    }

    /**
     * Remove a property
     *
     * @param key String
     * @return String oldValue - If present
     */
    public String removeProperty(String key) {
        String oldValue = null;

        if (this.properties != null) {
            oldValue = (String) this.properties.remove(key);
        }

        return oldValue;
    }

    /**
     * Save the current state of the Properties
     *
     * @throws IOException ioException
     */
    public void storeProperties() throws IOException {
        if (this.properties != null) {
            this.properties.store(new FileOutputStream(this.propertiesFile), "");
        }
    }

    /**
     * Get the Properties File
     *
     * @return File propertiesFile
     */
    public File getPropertiesFile() {
        return this.propertiesFile;
    }

    /**
     * Get the Last Sent State Value
     *
     * @param stateId String
     * @return String lastStateValue
     */
    public String getLastStateValue(String stateId) {
        return this.currentStates.get(stateId);
    }

    /**
     * Interface Definition for Callbacks
     */
    public interface TouchPortalPluginListener {
        /**
         * Called when the Socket connection is lost or the plugin has received the close Message
         *
         * @param exception {@link Exception} raised or null if disconnection comes from the close Message
         */
        void onDisconnect(Exception exception);

        /**
         * Called when receiving a message from the Touch Portal Plugin System
         *
         * @param jsonMessage {@link JSONObject}
         */
        void onReceive(JSONObject jsonMessage);
    }
}
