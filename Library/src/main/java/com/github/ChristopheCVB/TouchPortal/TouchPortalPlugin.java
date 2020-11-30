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
import com.github.ChristopheCVB.TouchPortal.Annotations.Data;
import com.github.ChristopheCVB.TouchPortal.Annotations.Setting;
import com.github.ChristopheCVB.TouchPortal.Helpers.*;
import com.github.ChristopheCVB.TouchPortal.model.*;
import com.github.ChristopheCVB.TouchPortal.model.deserializer.TPMessageDeserializer;
import com.google.gson.*;
import okhttp3.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is the class you need to extend in order to create a Touch Portal Plugin
 *
 * @see <a href="https://www.touch-portal.com/sdk/index.php">Documentation: Touch Portal SDK</a>
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
     * Plugin Version Property Key
     */
    private static final String KEY_PLUGIN_VERSION = "plugin.version";

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
    private String touchPortalPluginFolder;
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
     * Last sent Choices HashMap (Key, Value)
     */
    private final HashMap<String, String[]> currentChoices = new HashMap<>();
    /**
     * Current Held Actions States
     */
    private final HashMap<String, Boolean> heldActionsStates = new HashMap<>();
    /**
     * Executor Service for callbacks
     */
    private final ExecutorService callbacksExecutor;

    /**
     * Internal Gson Serializer/Deserializer
     */
    private Gson gson;
    /**
     * Info sent by the Touch Portal Plugin System
     */
    private TPInfoMessage tpInfoMessage;

    /**
     * Constructor
     *
     * @param parallelizeActions boolean - Parallelize Actions execution
     */
    protected TouchPortalPlugin(boolean parallelizeActions) {
        try {
            this.touchPortalPluginFolder = new File(".").getCanonicalPath();
        }
        catch (IOException ioException) {
            this.touchPortalPluginFolder = new File(".").getAbsolutePath();
            if (this.touchPortalPluginFolder.endsWith(".")) {
                this.touchPortalPluginFolder = this.touchPortalPluginFolder.substring(0, this.touchPortalPluginFolder.length() - 2);
            }
        }
        this.pluginClass = this.getClass();
        this.callbacksExecutor = Executors.newFixedThreadPool(parallelizeActions ? 5 : 1);
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
                    if (this.bufferedReader == null) {
                        this.bufferedReader = new BufferedReader(new InputStreamReader(this.touchPortalSocket.getInputStream(), StandardCharsets.UTF_8));
                    }
                    if (this.gson == null) {
                        TPMessageDeserializer tpMessageDeserializer = new TPMessageDeserializer();
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_CLOSE_PLUGIN, TPClosePluginMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_INFO, TPInfoMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_LIST_CHANGE, TPListChangeMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_BROADCAST, TPBroadcastMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_SETTINGS, TPSettingsMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_ACTION, TPActionMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_HOLD_DOWN, TPActionMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_HOLD_UP, TPActionMessage.class);
                        this.gson = new GsonBuilder().registerTypeAdapter(TPMessage.class, tpMessageDeserializer).create();
                    }
                    String socketMessage = this.bufferedReader.readLine();
                    if (socketMessage == null) {
                        throw new SocketException("Server Socket Closed");
                    }
                    this.onMessage(socketMessage);
                }
                catch (IOException ioException) {
                    this.close(ioException);
                    break;
                }
                catch (Exception ignored) {}
            }
        });
    }

    private void onMessage(String socketMessage) throws SocketException, JsonParseException {
        if (!socketMessage.isEmpty()) {
            TPMessage tpMessage = this.gson.fromJson(socketMessage, TPMessage.class);
            if (tpMessage != null && tpMessage.type != null) {
                switch (tpMessage.type) {
                    case ReceivedMessageHelper.TYPE_CLOSE_PLUGIN:
                        throw new SocketException("Close Message Received");

                    case ReceivedMessageHelper.TYPE_INFO:
                        this.tpInfoMessage = (TPInfoMessage) tpMessage;

                        this.updateSettingFields(this.tpInfoMessage.settings);

                        if (this.touchPortalPluginListener != null) {
                            this.touchPortalPluginListener.onInfo(this.tpInfoMessage);
                        }
                        break;

                    case ReceivedMessageHelper.TYPE_LIST_CHANGE:
                        TPListChangeMessage listChangeMessage = (TPListChangeMessage) tpMessage;
                        if (this.touchPortalPluginListener != null) {
                            this.touchPortalPluginListener.onListChanged(listChangeMessage);
                        }
                        break;

                    case ReceivedMessageHelper.TYPE_BROADCAST:
                        TPBroadcastMessage tpBroadcastMessage = (TPBroadcastMessage) tpMessage;
                        if (this.touchPortalPluginListener != null) {
                            this.touchPortalPluginListener.onBroadcast(tpBroadcastMessage);
                        }
                        break;

                    case ReceivedMessageHelper.TYPE_SETTINGS:
                        TPSettingsMessage tpSettingsMessage = (TPSettingsMessage) tpMessage;

                        this.updateSettingFields(tpSettingsMessage.settings);

                        if (this.touchPortalPluginListener != null) {
                            this.touchPortalPluginListener.onSettings(tpSettingsMessage);
                        }
                        break;

                    default:
                        JsonObject jsonMessage = JsonParser.parseString(socketMessage).getAsJsonObject();
                        TPActionMessage tpActionMessage = (TPActionMessage) tpMessage;
                        if (this.pluginClass.getName().equals(tpActionMessage.pluginId)) {
                            boolean called = false;
                            switch (tpMessage.type) {
                                case ReceivedMessageHelper.TYPE_ACTION:
                                    called = this.onActionReceived(tpActionMessage, jsonMessage, null);
                                    break;

                                case ReceivedMessageHelper.TYPE_HOLD_DOWN:
                                    called = this.onActionReceived(tpActionMessage, jsonMessage, true);
                                    break;

                                case ReceivedMessageHelper.TYPE_HOLD_UP:
                                    called = this.onActionReceived(tpActionMessage, jsonMessage, false);
                                    break;
                            }
                            if (!called) {
                                if (this.touchPortalPluginListener != null) {
                                    this.touchPortalPluginListener.onReceived(jsonMessage);
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

    private void updateSettingFields(HashMap<String, String> settings) {
        Field[] pluginSettingFields = Arrays.stream(this.pluginClass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Setting.class)).toArray(Field[]::new);
        for (String settingName : settings.keySet()) {
            for (Field pluginSettingField : pluginSettingFields) {
                Setting setting = pluginSettingField.getAnnotation(Setting.class);
                String fieldSettingName = SettingHelper.getSettingName(pluginSettingField, setting);
                if (settingName.equals(fieldSettingName)) {
                    try {
                        pluginSettingField.setAccessible(true);
                        pluginSettingField.set(this, ReceivedMessageHelper.getTypedValue(pluginSettingField.getType().getName(), settings.get(settingName)));
                    }
                    catch (Exception ignored) {}
                    break;
                }
            }
        }
    }

    private boolean onActionReceived(TPActionMessage tpActionMessage, JsonObject jsonAction, Boolean held) {
        boolean called = false;
        if (tpActionMessage.actionId != null && !tpActionMessage.actionId.isEmpty()) {
            Method[] pluginActionMethods = Arrays.stream(this.pluginClass.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(Action.class)).toArray(Method[]::new);
            for (Method method : pluginActionMethods) {
                String methodActionId = ActionHelper.getActionId(this.pluginClass, method);
                if (tpActionMessage.actionId.equals(methodActionId)) {
                    try {
                        Parameter[] parameters = method.getParameters();
                        Object[] arguments = new Object[parameters.length];
                        for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
                            Parameter parameter = parameters[parameterIndex];
                            if (parameter.isAnnotationPresent(Data.class)) {
                                arguments[parameterIndex] = tpActionMessage.getTypedDataValue(this.pluginClass, method, parameter);
                            }
                            else if (parameter.getType().isAssignableFrom(JsonObject.class)) {
                                arguments[parameterIndex] = jsonAction;
                            }
                            else if (parameter.getType().isAssignableFrom(TPActionMessage.class)) {
                                arguments[parameterIndex] = tpActionMessage;
                            }
                            if (arguments[parameterIndex] == null) {
                                throw new ActionMethodDataParameterException(method, parameter);
                            }
                        }
                        this.heldActionsStates.put(tpActionMessage.actionId, held);
                        this.callbacksExecutor.submit(() -> {
                            try {
                                method.setAccessible(true);
                                method.invoke(this, arguments);
                                if (held == null || !held) {
                                    this.heldActionsStates.remove(tpActionMessage.actionId);
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        called = true;
                    }
                    catch (ActionMethodDataParameterException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        return called;
    }

    /**
     * Send the Pair Message
     *
     * @return boolean Pairing Message sent
     */
    private boolean sendPair() {
        JsonObject pairingMessage = new JsonObject();
        pairingMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_PAIR);
        pairingMessage.addProperty(SentMessageHelper.ID, PluginHelper.getPluginId(this.pluginClass));

        boolean paired = this.send(pairingMessage);
        System.out.println("Pairing Message Sent");

        return paired;
    }

    /**
     * Get TPInfo
     *
     * @return TPInfo tpInfo
     */
    public TPInfoMessage getTPInfo() {
        return this.tpInfoMessage;
    }

    /**
     * Close the Socket connection
     * <p>
     * The {@link TouchPortalPluginListener}.onDisconnect method will be called if the connection was alive.
     * </p>
     *
     * @param exception Exception
     */
    public synchronized void close(Exception exception) {
        System.out.println("Closing" + (exception != null ? " because " + exception.getMessage() : ""));

        if (this.touchPortalSocket != null) {
            this.storeProperties();

            if (this.listenerThread != null) {
                this.listenerThread.interrupt();
                this.listenerThread = null;
            }
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

            try {
                this.touchPortalSocket.close();
            }
            catch (IOException ignored) {}
            this.touchPortalSocket = null;

            if (this.touchPortalPluginListener != null) {
                this.touchPortalPluginListener.onDisconnected(exception);
            }
        }
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
     * Internal - Send a Message to the Touch Portal Plugin System
     *
     * @param message {@link JsonObject}
     * @return boolean isMessageSent
     */
    private boolean send(JsonObject message) {
        boolean sent = false;
        if (this.isConnected()) {
            try {
                if (this.printWriter == null) {
                    this.printWriter = new PrintWriter(new OutputStreamWriter(this.touchPortalSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                }
                this.printWriter.println(message);

                sent = true;
            }
            catch (IOException ignored) {}
        }

        return sent;
    }

    /**
     * Deprecated - Use the generated Constants Class to access IDs
     * Send a Choice Update Message to the Touch Portal Plugin System
     *
     * @param categoryId     String
     * @param stateFieldName String
     * @param values         String[]
     * @return boolean choiceUpdateMessageSent
     */
    @Deprecated
    public boolean sendChoiceUpdate(String categoryId, String stateFieldName, String[] values) {
        String stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateFieldName);
        return this.sendChoiceUpdate(stateId, values);
    }

    /**
     * Send a Choice Update Message to the Touch Portal Plugin System without allowing empty array values
     *
     * @param listId String
     * @param values String
     * @return boolean choiceUpdateMessageSent
     */
    public boolean sendChoiceUpdate(String listId, String[] values) {
        return this.sendChoiceUpdate(listId, values, false);
    }

    /**
     * Send a Choice Update Message to the Touch Portal Plugin System
     *
     * @param listId                String
     * @param values                String[]
     * @param allowEmptyArrayValues boolean
     * @return boolean choiceUpdateMessageSent
     */
    public boolean sendChoiceUpdate(String listId, String[] values, boolean allowEmptyArrayValues) {
        boolean sent = false;
        if (listId != null && !listId.isEmpty() && (allowEmptyArrayValues || (values != null && values.length > 0))) {
            if (!this.currentChoices.containsKey(listId) || !Arrays.equals(this.currentChoices.get(listId), values)) {
                JsonObject choiceUpdateMessage = new JsonObject();
                choiceUpdateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CHOICE_UPDATE);
                choiceUpdateMessage.addProperty(SentMessageHelper.ID, listId);
                JsonArray jsonValues = new JsonArray();
                if (values != null) {
                    for (String value : values) {
                        if (value == null) {
                            jsonValues.add(JsonNull.INSTANCE);
                        }
                        else {
                            jsonValues.add(new JsonPrimitive(value));
                        }
                    }
                }
                choiceUpdateMessage.add(SentMessageHelper.VALUE, jsonValues);
                sent = this.send(choiceUpdateMessage);
                if (sent) {
                    this.currentChoices.put(listId, values);
                }
                System.out.println("Update Choices [" + listId + "] Sent [" + sent + "]");
            }
        }

        return sent;
    }

    /**
     * Deprecated - Use the generated Constants Class to access IDs
     * Send a Specific Choice Update Message to the Touch Portal Plugin System
     *
     * @param categoryId     String
     * @param stateFieldName String
     * @param instanceId     String
     * @param values         String[]
     * @return boolean specificChoiceUpdateMessageSent
     */
    @Deprecated
    public boolean sendSpecificChoiceUpdate(String categoryId, String stateFieldName, String instanceId, String[] values) {
        String stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateFieldName);
        return this.sendSpecificChoiceUpdate(stateId, instanceId, values);
    }

    /**
     * Send a Specific Choice Update Message to the Touch Portal Plugin System without allowing empty array values
     *
     * @param choiceId   String
     * @param instanceId String
     * @param values     String[]
     * @return boolean specificChoiceUpdateMessageSent
     */
    public boolean sendSpecificChoiceUpdate(String choiceId, String instanceId, String[] values) {
        return this.sendSpecificChoiceUpdate(choiceId, instanceId, values, false);
    }

    /**
     * Send a Specific Choice Update Message to the Touch Portal Plugin System
     *
     * @param choiceId              String
     * @param instanceId            String
     * @param values                String[]
     * @param allowEmptyArrayValues boolean
     * @return boolean specificChoiceUpdateMessageSent
     */
    public boolean sendSpecificChoiceUpdate(String choiceId, String instanceId, String[] values, boolean allowEmptyArrayValues) {
        boolean sent = false;
        String choiceKey = choiceId + ":" + instanceId;
        if (choiceId != null && !choiceId.isEmpty() && instanceId != null && !instanceId.isEmpty() && (allowEmptyArrayValues || (values != null && values.length > 0))) {
            if (!this.currentChoices.containsKey(choiceKey) || !Arrays.equals(this.currentChoices.get(choiceKey), values)) {
                JsonObject specificChoiceUpdateMessage = new JsonObject();
                specificChoiceUpdateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CHOICE_UPDATE);
                specificChoiceUpdateMessage.addProperty(SentMessageHelper.ID, choiceId);
                specificChoiceUpdateMessage.addProperty(SentMessageHelper.INSTANCE_ID, instanceId);
                JsonArray jsonValues = new JsonArray();
                if (values != null) {
                    for (String value : values) {
                        if (value == null) {
                            jsonValues.add(JsonNull.INSTANCE);
                        }
                        else {
                            jsonValues.add(new JsonPrimitive(value));
                        }
                    }
                }
                specificChoiceUpdateMessage.add(SentMessageHelper.VALUE, jsonValues);
                sent = this.send(specificChoiceUpdateMessage);
                if (sent) {
                    this.currentChoices.put(choiceKey, values);
                }
                System.out.println("Update Specific Choices [" + choiceId + "] Sent [" + sent + "]");
            }
        }
        return sent;
    }

    /**
     * Deprecated - Use the generated Constants Class to access IDs
     * Send a State Update Message to the Touch Portal Plugin System
     *
     * @param categoryId     String
     * @param stateFieldName String
     * @param value          String
     * @return boolean stateUpdateMessageSent
     */
    @Deprecated
    public boolean sendStateUpdate(String categoryId, String stateFieldName, String value) {
        String stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateFieldName);
        return this.sendStateUpdate(stateId, value);
    }

    /**
     * Send a State Update Message to the Touch Portal Plugin System not allowing empty value
     *
     * @param stateId String
     * @param value   Object
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendStateUpdate(String stateId, Object value) {
        return this.sendStateUpdate(stateId, value, false);
    }

    /**
     * Send a State Update Message to the Touch Portal Plugin System
     *
     * @param stateId         String
     * @param value           Object
     * @param allowEmptyValue boolean
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendStateUpdate(String stateId, Object value, boolean allowEmptyValue) {
        boolean sent = false;
        String valueStr = value != null ? String.valueOf(value) : null;
        if (stateId != null && !stateId.isEmpty() && valueStr != null && (allowEmptyValue || !valueStr.isEmpty())) {
            if (!this.currentStates.containsKey(stateId) || !this.currentStates.get(stateId).equals(valueStr)) {
                JsonObject stateUpdateMessage = new JsonObject();
                stateUpdateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_STATE_UPDATE);
                stateUpdateMessage.addProperty(SentMessageHelper.ID, stateId);
                stateUpdateMessage.addProperty(SentMessageHelper.VALUE, valueStr);
                sent = this.send(stateUpdateMessage);
                if (sent) {
                    this.currentStates.put(stateId, valueStr);
                }
                System.out.println("Update State [" + stateId + "] Sent [" + sent + "]");
            }
        }
        return sent;
    }

    /**
     * Send a Create a State Message to the Touch Portal Plugin System
     *
     * @param categoryId  String
     * @param stateId     String
     * @param description String
     * @param value       Object
     * @return boolean stateCreateSent
     */
    public boolean sendCreateState(String categoryId, String stateId, String description, Object value) {
        boolean sent = false;
        String valueStr = value != null ? String.valueOf(value) : null;
        if (categoryId != null && !categoryId.isEmpty() && stateId != null && !stateId.isEmpty() && description != null && !description.isEmpty() && valueStr != null && !valueStr.isEmpty()) {
            stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateId);
            if (!this.currentStates.containsKey(stateId)) {
                JsonObject createStateMessage = new JsonObject();
                createStateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CREATE_STATE);
                createStateMessage.addProperty(SentMessageHelper.ID, stateId);
                createStateMessage.addProperty(SentMessageHelper.DESCRIPTION, description);
                createStateMessage.addProperty(SentMessageHelper.DEFAULT_VALUE, valueStr);
                sent = this.send(createStateMessage);
                if (sent) {
                    this.currentStates.put(stateId, valueStr);
                }
                System.out.println("Create State [" + stateId + "] Sent [" + sent + "]");
            }
            else {
                sent = this.sendStateUpdate(stateId, value, true);
            }
        }
        return sent;
    }

    /**
     * Send a Remove State Message to the Touch Portal Plugin System
     *
     * @param categoryId String
     * @param stateId    String
     * @return boolean removeStateSent
     */
    public boolean sendRemoveState(String categoryId, String stateId) {
        boolean sent = false;
        if (categoryId != null && !categoryId.isEmpty() && stateId != null && !stateId.isEmpty()) {
            stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateId);
            JsonObject removeStateMessage = new JsonObject();
            removeStateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_REMOVE_STATE);
            removeStateMessage.addProperty(SentMessageHelper.ID, stateId);
            sent = this.send(removeStateMessage);
            if (sent) {
                this.currentStates.remove(stateId);
            }
            System.out.println("Remove State [" + stateId + "] Sent [" + sent + "]");
        }
        return sent;
    }

    /**
     * Send an Action Data Update
     *
     * @param instanceId   String
     * @param actionDataId String
     * @param properties   HashMap&lt;String, Integer&gt;
     * @return boolean actionDataUpdateSent
     */
    public boolean sendActionDataUpdate(String instanceId, String actionDataId, HashMap<String, Number> properties) {
        boolean sent = false;

        if (instanceId != null && !instanceId.isEmpty() && actionDataId != null && !actionDataId.isEmpty() && properties != null && !properties.isEmpty()) {
            JsonObject actionDataUpdate = new JsonObject();
            actionDataUpdate.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_ACTION_DATA_UPDATE);
            actionDataUpdate.addProperty(SentMessageHelper.INSTANCE_ID, instanceId);
            JsonObject actionDataUpdateDataObject = new JsonObject();
            actionDataUpdateDataObject.addProperty(SentMessageHelper.ID, actionDataId);
            for (String property : properties.keySet()) {
                actionDataUpdateDataObject.addProperty(property, properties.get(property));
            }
            actionDataUpdate.add(SentMessageHelper.DATA, actionDataUpdateDataObject);

            sent = this.send(actionDataUpdate);
            System.out.println("Action Data Update [" + actionDataId + "] Sent [" + sent + "]");
        }

        return sent;
    }

    /**
     * Is the Plugin connected to the Touch Portal Plugin System
     *
     * @return boolean isPluginConnected
     */
    public boolean isConnected() {
        return this.touchPortalSocket != null && this.touchPortalSocket.isConnected();
    }

    /**
     * Is the Plugin listening to the Touch Portal Plugin System
     *
     * @return boolean isPluginListening
     */
    public boolean isListening() {
        return this.listenerThread != null && this.listenerThread.isAlive();
    }

    /**
     * Get a Resource File that is stored in the Plugin directory
     *
     * @param resourcePluginFilePath String
     * @return File resourceFile
     */
    public File getResourceFile(String resourcePluginFilePath) {
        return Paths.get(this.touchPortalPluginFolder + "/" + resourcePluginFilePath).toFile();
    }

    /**
     * Loads a Properties file from the Plugin directory
     *
     * @param propertiesPluginFilePath String - Relative path of the properties File
     * @return boolean loaded
     */
    public boolean loadProperties(String propertiesPluginFilePath) {
        this.propertiesFile = this.getResourceFile(propertiesPluginFilePath);
        return this.loadProperties();
    }

    /**
     * Internal - Load the Properties File
     *
     * @return boolean loaded
     */
    private boolean loadProperties() {
        boolean loaded = false;
        try {
            if (this.propertiesFile != null) {
                FileInputStream fileInputStream = new FileInputStream(this.propertiesFile.getAbsolutePath());
                this.properties = new Properties();
                this.properties.load(fileInputStream);
                fileInputStream.close();
                loaded = true;
            }
        }
        catch (IOException ignored) {}
        return loaded;
    }

    /**
     * Reloads the Properties File previously set
     *
     * @return boolean reloaded
     */
    public boolean reloadProperties() {
        return this.loadProperties();
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
     * Read a property with the key and fall back to default value
     *
     * @param key          String
     * @param defaultValue String
     * @return String value
     */
    public String getProperty(String key, String defaultValue) {
        String value = this.getProperty(key);
        return value == null ? defaultValue : value;
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
     * @return boolean stored
     */
    public boolean storeProperties() {
        boolean stored = false;
        try {
            if (this.properties != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(this.propertiesFile);
                this.properties.store(fileOutputStream, this.pluginClass.getSimpleName());
                fileOutputStream.close();
                stored = true;
            }
        }
        catch (IOException ignored) {}
        return stored;
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
     * Reads a Properties file and compare versions
     *
     * @param pluginConfigURL   String - The URL of the properties file
     * @param pluginVersionCode long - Current Plugin Version Code
     */
    public boolean isUpdateAvailable(String pluginConfigURL, long pluginVersionCode) {
        boolean updateAvailable = false;

        InputStream cloudPropertiesStream = null;
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build();
            Call call = okHttpClient.newCall(new Request.Builder().url(pluginConfigURL).build());
            Response response = call.execute();
            if (response.isSuccessful()) {
                Properties cloudProperties = new Properties();
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    cloudProperties.load(cloudPropertiesStream = responseBody.byteStream());
                    long lastPluginVersion = Long.parseLong(cloudProperties.getProperty(TouchPortalPlugin.KEY_PLUGIN_VERSION));
                    updateAvailable = lastPluginVersion > pluginVersionCode;
                    responseBody.close();
                }
            }
        }
        catch (IllegalArgumentException | IOException exception) {
            System.out.println("Check Update failed: " + exception.getMessage());
        }
        finally {
            if (cloudPropertiesStream != null) {
                try {
                    cloudPropertiesStream.close();
                }
                catch (IOException ignored) {}
            }
        }

        return updateAvailable;
    }

    /**
     * Returns null if the Action has been triggered from a Press or true/false if it's been triggered by a Hold (Down or Up)
     *
     * @param actionId String
     * @return Boolean isActionBeingHeld
     */
    public Boolean isActionBeingHeld(String actionId) {
        return this.heldActionsStates.get(actionId);
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
        void onDisconnected(Exception exception);

        /**
         * Called when receiving a message from the Touch Portal Plugin System
         *
         * @param jsonMessage {@link JsonObject}
         */
        void onReceived(JsonObject jsonMessage);

        /**
         * Called when the Info Message is received
         *
         * @param tpInfoMessage {@link TPInfoMessage}
         */
        void onInfo(TPInfoMessage tpInfoMessage);

        /**
         * Called when a List Change Message is received
         *
         * @param tpListChangeMessage TPListChangeMessage
         */
        void onListChanged(TPListChangeMessage tpListChangeMessage);

        /**
         * Called when a Broadcast Message is received
         *
         * @param tpBroadcastMessage TPBroadcastMessage
         */
        void onBroadcast(TPBroadcastMessage tpBroadcastMessage);

        /**
         * Called when a Settings Message is received
         *
         * @param tpSettingsMessage TPSettingsMessage
         */
        void onSettings(TPSettingsMessage tpSettingsMessage);
    }

    /**
     * Signals that the @Action Annotated Method have a parameter which is not @Data Annotated.
     */
    public static class ActionMethodDataParameterException extends ReflectiveOperationException {
        /**
         * Constructor with a detail message.
         *
         * @param method    Method
         * @param parameter Parameter
         */
        public ActionMethodDataParameterException(Method method, Parameter parameter) {
            super("Impossible to retrieve Action Data Item for Method [" + method.getName() + "] and parameter [" + parameter.getName() + "]");
        }
    }
}
