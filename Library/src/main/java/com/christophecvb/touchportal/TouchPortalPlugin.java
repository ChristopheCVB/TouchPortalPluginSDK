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

package com.christophecvb.touchportal;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.helpers.*;
import com.christophecvb.touchportal.model.*;
import com.christophecvb.touchportal.model.deserializer.TPMessageDeserializer;
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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

/**
 * This is the class you need to extend in order to create a Touch Portal Plugin
 *
 * @see <a href="https://www.touch-portal.com/api/index.php">Documentation: Touch Portal API</a>
 */
public abstract class TouchPortalPlugin {
    /**
     * Logger
     */
    private final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());
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

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(new CustomConsoleHandler());
    }

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
     * Current Connector Values HashMap (Key, Value)
     */
    private final HashMap<String, Integer> currentConnectorValues = new HashMap<>();
    /**
     * Connector IDs mapping HashMap (ConstructedId, ShortId)
     */
    private final HashMap<String, String> connectorIdsMapping = new HashMap<>();
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
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_CONNECTOR_CHANGE, TPConnectorChangeMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_NOTIFICATION_OPTION_CLICKED, TPNotificationOptionClickedMessage.class);
                        tpMessageDeserializer.registerTPMessageType(ReceivedMessageHelper.TYPE_SHORT_CONNECTOR_ID_NOTIFICATION, TPShortConnectorIdNotification.class);
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

                    case ReceivedMessageHelper.TYPE_NOTIFICATION_OPTION_CLICKED:
                        TPNotificationOptionClickedMessage tpNotificationOptionClickedMessage = (TPNotificationOptionClickedMessage) tpMessage;
                        if (this.touchPortalPluginListener != null) {
                            this.touchPortalPluginListener.onNotificationOptionClicked(tpNotificationOptionClickedMessage);
                        }
                        break;

                    case ReceivedMessageHelper.TYPE_SHORT_CONNECTOR_ID_NOTIFICATION:
                        TPShortConnectorIdNotification tpShortConnectorIdNotification = (TPShortConnectorIdNotification) tpMessage;
                        this.connectorIdsMapping.put(tpShortConnectorIdNotification.connectorId, tpShortConnectorIdNotification.shortId);
                        break;

                    default:
                        JsonObject jsonMessage = JsonParser.parseString(socketMessage).getAsJsonObject();
                        if (this.pluginClass.getName().equals(jsonMessage.get(ReceivedMessageHelper.PLUGIN_ID).getAsString())) {
                            boolean called = false;
                            switch (tpMessage.type) {
                                case ReceivedMessageHelper.TYPE_ACTION:
                                    called = this.onActionReceived((TPActionMessage) tpMessage, jsonMessage, null);
                                    break;

                                case ReceivedMessageHelper.TYPE_HOLD_DOWN:
                                    called = this.onActionReceived((TPActionMessage) tpMessage, jsonMessage, true);
                                    break;

                                case ReceivedMessageHelper.TYPE_HOLD_UP:
                                    called = this.onActionReceived((TPActionMessage) tpMessage, jsonMessage, false);
                                    break;

                                case ReceivedMessageHelper.TYPE_CONNECTOR_CHANGE:
                                    called = this.onConnectorChangeReceived((TPConnectorChangeMessage) tpMessage, jsonMessage);
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
                                throw new MethodDataParameterException(method, parameter);
                            }
                        }
                        this.heldActionsStates.put(tpActionMessage.actionId, held);
                        this.callbacksExecutor.submit(() -> {
                            try {
                                method.setAccessible(true);
                                method.invoke(this, arguments);
                            }
                            catch (Exception e) {
                                TouchPortalPlugin.LOGGER.log(Level.SEVERE, "Action method could not be invoked", e);
                            }
                            finally {
                                if (held == null || !held) {
                                    this.heldActionsStates.remove(tpActionMessage.actionId);
                                }
                            }
                        });
                        called = true;
                    }
                    catch (MethodDataParameterException e) {
                        TouchPortalPlugin.LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                    break;
                }
            }
        }
        return called;
    }

    private boolean onConnectorChangeReceived(TPConnectorChangeMessage tpConnectorChangeMessage, JsonObject jsonAction) {
        boolean called = false;
        if (tpConnectorChangeMessage.connectorId != null && !tpConnectorChangeMessage.connectorId.isEmpty()) {
            Method[] pluginConnectorMethods = Arrays.stream(this.pluginClass.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(Connector.class)).toArray(Method[]::new);
            for (Method method : pluginConnectorMethods) {
                String methodConnectorId = ConnectorHelper.getConnectorId(this.pluginClass, method);
                if (tpConnectorChangeMessage.connectorId.equals(methodConnectorId)) {
                    try {
                        Parameter[] parameters = method.getParameters();
                        Object[] arguments = new Object[parameters.length];
                        for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
                            Parameter parameter = parameters[parameterIndex];
                            if (parameter.isAnnotationPresent(Data.class)) {
                                arguments[parameterIndex] = tpConnectorChangeMessage.getTypedDataValue(this.pluginClass, method, parameter);
                            }
                            else if (parameter.isAnnotationPresent(ConnectorValue.class)) {
                                arguments[parameterIndex] = tpConnectorChangeMessage.value;
                            }
                            else if (parameter.getType().isAssignableFrom(JsonObject.class)) {
                                arguments[parameterIndex] = jsonAction;
                            }
                            else if (parameter.getType().isAssignableFrom(TPConnectorChangeMessage.class)) {
                                arguments[parameterIndex] = tpConnectorChangeMessage;
                            }
                            if (arguments[parameterIndex] == null) {
                                throw new MethodDataParameterException(method, parameter);
                            }
                        }
                        this.currentConnectorValues.put(tpConnectorChangeMessage.getConstructedId(), tpConnectorChangeMessage.value);
                        this.callbacksExecutor.submit(() -> {
                            try {
                                method.setAccessible(true);
                                method.invoke(this, arguments);
                            }
                            catch (Exception e) {
                                TouchPortalPlugin.LOGGER.log(Level.SEVERE, "Connector method could not be invoked", e);
                            }
                        });
                        called = true;
                    }
                    catch (MethodDataParameterException e) {
                        TouchPortalPlugin.LOGGER.log(Level.WARNING, e.getMessage(), e);
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
        TouchPortalPlugin.LOGGER.log(Level.INFO, "Pairing Message Sent");

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
        TouchPortalPlugin.LOGGER.log(Level.INFO, "Closing" + (exception != null ? " because " + exception.getMessage() : ""));

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
        TouchPortalPlugin.LOGGER.log(Level.INFO, "connectingAndPairing");
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
        TouchPortalPlugin.LOGGER.log(Level.INFO, "Start listening");
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
                TouchPortalPlugin.LOGGER.log(Level.INFO, "Update Choices [" + listId + "] Sent [" + sent + "]");
            }
        }

        return sent;
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
                TouchPortalPlugin.LOGGER.log(Level.INFO, "Update Specific Choices [" + choiceId + "] Sent [" + sent + "]");
            }
        }
        return sent;
    }

    /**
     * Send a State Update Message to the Touch Portal Plugin System not allowing empty value
     *
     * @param stateId String
     * @param value   Object
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendStateUpdate(String stateId, Object value) {
        return this.sendStateUpdate(stateId, value, false, false);
    }

    /**
     * Send a State Update Message to the Touch Portal Plugin System
     *
     * @param stateId         String
     * @param value           Object
     * @param allowEmptyValue boolean
     * @param forceUpdate     boolean
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendStateUpdate(String stateId, Object value, boolean allowEmptyValue, boolean forceUpdate) {
        boolean sent = false;
        String valueStr = value != null ? String.valueOf(value) : null;
        if (stateId != null && !stateId.isEmpty() && valueStr != null && (allowEmptyValue || !valueStr.isEmpty())) {
            if (forceUpdate || (!this.currentStates.containsKey(stateId) || !this.currentStates.get(stateId).equals(valueStr))) {
                JsonObject stateUpdateMessage = new JsonObject();
                stateUpdateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_STATE_UPDATE);
                stateUpdateMessage.addProperty(SentMessageHelper.ID, stateId);
                stateUpdateMessage.addProperty(SentMessageHelper.VALUE, valueStr);
                sent = this.send(stateUpdateMessage);
                if (sent) {
                    this.currentStates.put(stateId, valueStr);
                }
                TouchPortalPlugin.LOGGER.log(Level.INFO, "Update State [" + stateId + "] Sent [" + sent + "]");
            }
        }
        return sent;
    }

    /**
     * Send a Create a State Message to the Touch Portal Plugin System not allowing empty value
     *
     * @param categoryId  String
     * @param stateId     String
     * @param description String
     * @param value       Object
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendCreateState(String categoryId, String stateId, String description, Object value) {
        return this.sendCreateState(categoryId, stateId, null, description, value, false, false);
    }

    /**
     * Send a Create a State Message to the Touch Portal Plugin System not allowing empty value
     *
     * @param categoryId  String
     * @param stateId     String
     * @param parentGroup String
     * @param description String
     * @param value       Object
     * @return boolean stateUpdateMessageSent
     */
    public boolean sendCreateState(String categoryId, String stateId, String parentGroup, String description, Object value) {
        return this.sendCreateState(categoryId, stateId, parentGroup, description, value, false, false);
    }

    /**
     * Send a Create a State Message to the Touch Portal Plugin System
     *
     * @param categoryId        String
     * @param stateId           String
     * @param description       String
     * @param value             Object
     * @param allowEmptyValue   boolean
     * @param forceUpdate       boolean
     * @return boolean stateCreateSent
     */
    public boolean sendCreateState(String categoryId, String stateId, String description, Object value, boolean allowEmptyValue, boolean forceUpdate) {
        return this.sendCreateState(categoryId, stateId, null, description, value, allowEmptyValue, forceUpdate);
    }

    /**
     * Send a Create a State Message to the Touch Portal Plugin System
     *
     * @param categoryId        String
     * @param stateId           String
     * @param parentGroup       String
     * @param description       String
     * @param value             Object
     * @param allowEmptyValue   boolean
     * @param forceUpdate       boolean
     * @return boolean stateCreateSent
     */
    public boolean sendCreateState(String categoryId, String stateId, String parentGroup, String description, Object value, boolean allowEmptyValue, boolean forceUpdate) {
        boolean sent = false;
        String valueStr = value != null ? String.valueOf(value) : null;
        if (categoryId != null && !categoryId.isEmpty() && stateId != null && !stateId.isEmpty() && description != null && !description.isEmpty() && valueStr != null && (allowEmptyValue || !valueStr.isEmpty())) {
            stateId = StateHelper.getStateId(this.pluginClass, categoryId, stateId);
            if (!this.currentStates.containsKey(stateId)) {
                JsonObject createStateMessage = new JsonObject();
                createStateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CREATE_STATE);
                createStateMessage.addProperty(SentMessageHelper.ID, stateId);
                createStateMessage.addProperty(SentMessageHelper.DESCRIPTION, description);
                createStateMessage.addProperty(SentMessageHelper.DEFAULT_VALUE, valueStr);
                if (parentGroup == null || parentGroup.isEmpty()) {
                    for (Class<?> subClass : this.pluginClass.getDeclaredClasses()) {
                        for (Field subClassField : subClass.getDeclaredFields()) {
                            if (subClassField.isAnnotationPresent(Category.class)) {
                                Category category = subClassField.getAnnotation(Category.class);

                                if(category.name().equals(categoryId) || category.id().equals(categoryId) || subClassField.getName().equals(categoryId)) {
                                    createStateMessage.addProperty(SentMessageHelper.PARENT_GROUP, category.name());
                                }
                            }
                        }
                    }
                } else {
                    createStateMessage.addProperty(SentMessageHelper.PARENT_GROUP, parentGroup);
                }

                sent = this.send(createStateMessage);
                if (sent) {
                    this.currentStates.put(stateId, valueStr);
                }
                TouchPortalPlugin.LOGGER.info("Create State [" + stateId + "] Sent [" + sent + "]");
            }
            else {
                sent = this.sendStateUpdate(stateId, value, allowEmptyValue, forceUpdate);
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
            TouchPortalPlugin.LOGGER.log(Level.INFO, "Remove State [" + stateId + "] Sent [" + sent + "]");
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
            actionDataUpdateDataObject.addProperty(DataHelper.TYPE, GenericHelper.TP_TYPE_NUMBER);
            for (String property : properties.keySet()) {
                actionDataUpdateDataObject.addProperty(property, properties.get(property));
            }
            actionDataUpdate.add(SentMessageHelper.DATA, actionDataUpdateDataObject);

            sent = this.send(actionDataUpdate);
            TouchPortalPlugin.LOGGER.log(Level.INFO, "Action Data Update [" + actionDataId + "] Sent [" + sent + "]");
        }

        return sent;
    }

    /**
     * Send a Setting Update Message to the Touch Portal Plugin System
     *
     * @param settingName     String
     * @param value           String
     * @param allowEmptyValue boolean
     * @return boolean settingUpdateMessageSent
     */
    public boolean sendSettingUpdate(String settingName, String value, boolean allowEmptyValue) {
        boolean sent = false;
        if (settingName != null && !settingName.isEmpty() && value != null && (allowEmptyValue || !value.isEmpty())) {
            if (this.tpInfoMessage != null && this.tpInfoMessage.settings.containsKey(settingName) && !this.tpInfoMessage.settings.get(settingName).equals(value)) {
                JsonObject settingUpdateMessage = new JsonObject();
                settingUpdateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_SETTING_UPDATE);
                settingUpdateMessage.addProperty(SentMessageHelper.NAME, settingName);
                settingUpdateMessage.addProperty(SentMessageHelper.VALUE, value);
                sent = this.send(settingUpdateMessage);
                if (sent) {
                    this.tpInfoMessage.settings.put(settingName, value);
                }
                TouchPortalPlugin.LOGGER.log(Level.INFO, "Update Setting [" + settingName + "] Sent [" + sent + "]");
            }
        }
        return sent;
    }

    /**
     * Send a Show Notification Message to the Touch Portal Plugin System
     *
     * @param notificationId String
     * @param title          String
     * @param msg            String
     * @param options        {@link TPNotificationOption}[]
     * @return boolean showNotificationMessageSent
     */
    public boolean sendShowNotification(String notificationId, String title, String msg, TPNotificationOption[] options) {
        boolean sent = false;
        if (notificationId != null && !notificationId.isEmpty() && title != null && !title.isEmpty() && msg != null && !msg.isEmpty() && options != null && options.length >= 1) {
            JsonObject showNotificationMessage = new JsonObject();
            showNotificationMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_SHOW_NOTIFICATION);
            showNotificationMessage.addProperty(SentMessageHelper.NOTIFICATION_ID, notificationId);
            showNotificationMessage.addProperty(SentMessageHelper.TITLE, title);
            showNotificationMessage.addProperty(SentMessageHelper.MSG, msg);

            JsonArray jsonOptions = new JsonArray();
            for (TPNotificationOption option : options) {
                JsonObject jsonOption = new JsonObject();
                jsonOption.addProperty(SentMessageHelper.ID, option.id);
                jsonOption.addProperty(SentMessageHelper.TITLE, option.title);

                jsonOptions.add(jsonOption);
            }
            showNotificationMessage.add(SentMessageHelper.OPTIONS, jsonOptions);

            sent = this.send(showNotificationMessage);
            TouchPortalPlugin.LOGGER.info("Show Notification [" + notificationId + "] Sent [" + sent + "]");
        }

        return sent;
    }

    /**
     * Send a Connector Update Message to the Touch Portal Plugin System
     *
     * @param pluginId      String
     * @param connectorId   String
     * @param value         Integer
     * @param data          Map&lt;String, Object&gt;
     * @return Boolean sent
     */
    public boolean sendConnectorUpdate(String pluginId, String connectorId, Integer value, Map<String, Object> data) {
        return this.sendConnectorUpdate(ConnectorHelper.getConstructedId(pluginId, connectorId, value, data), value);
    }

    /**
     * Send a Connector Update Message to the Touch Portal Plugin System
     *
     * @param constructedConnectorId    String
     * @param value                     Integer
     * @return boolean sendConnectorUpdateSent
     */
    private boolean sendConnectorUpdate(String constructedConnectorId, Integer value) {
        boolean sent = false;
        if (constructedConnectorId != null && !constructedConnectorId.isEmpty() && value != null && value >= 0 && value <= 100 && !value.equals(this.currentConnectorValues.get(constructedConnectorId))) {
            JsonObject connectorUpdateMessage = new JsonObject();
            connectorUpdateMessage.addProperty(SentMessageHelper.TYPE, SentMessageHelper.TYPE_CONNECTOR_UPDATE);
            String shortId = this.getConnectorShortId(constructedConnectorId);
            if (shortId != null) {
                connectorUpdateMessage.addProperty(SentMessageHelper.SHORT_ID, shortId);
            }
            else if (constructedConnectorId.length() <= 200){
                connectorUpdateMessage.addProperty(SentMessageHelper.CONNECTOR_ID, constructedConnectorId);
            }
            connectorUpdateMessage.addProperty(SentMessageHelper.VALUE, value);

            if (connectorUpdateMessage.has(SentMessageHelper.SHORT_ID) || connectorUpdateMessage.has(SentMessageHelper.CONNECTOR_ID)) {
                sent = this.send(connectorUpdateMessage);
            }
            if (sent) {
                this.currentConnectorValues.put(constructedConnectorId, value);
            }
            if (shortId != null || constructedConnectorId.length() <= 200) {
                TouchPortalPlugin.LOGGER.log(Level.INFO, "Connector Update [" + constructedConnectorId + "] Sent [" + sent + "]");
            }
        }

        return sent;
    }

    private String getConnectorShortId(String constructedConnectorId) {
        String shortId = null;
        HashMap<String, String> deconstructedConnectorId = this.deconstructConnectorId(constructedConnectorId);
        for (String mappedConnectorId : this.connectorIdsMapping.keySet()) {
            HashMap<String, String> deconstructedMappedConnectorId = this.deconstructConnectorId(mappedConnectorId);
            if (deconstructedConnectorId.equals(deconstructedMappedConnectorId)) {
                shortId = this.connectorIdsMapping.get(mappedConnectorId);
                break;
            }
        }
        return shortId;
    }

    private HashMap<String, String> deconstructConnectorId(String constructedConnectorId) {
        HashMap<String, String> deconstructedConnectorId = new HashMap<>();
        Arrays.stream(constructedConnectorId.split("\\|")).map(elem -> elem.split("=")).forEach(pair -> deconstructedConnectorId.put(pair[0], pair.length > 1 ? pair[1] : null));
        return deconstructedConnectorId;
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
     * @return boolean isUpdateAvailable
     */
    public boolean isUpdateAvailable(String pluginConfigURL, long pluginVersionCode) {
        boolean isUpdateAvailable = false;

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
                    isUpdateAvailable = lastPluginVersion > pluginVersionCode;
                    responseBody.close();
                }
            }
        }
        catch (IllegalArgumentException | IOException exception) {
            TouchPortalPlugin.LOGGER.log(Level.WARNING, "Check Update failed: " + exception.getMessage());
        }
        finally {
            if (cloudPropertiesStream != null) {
                try {
                    cloudPropertiesStream.close();
                }
                catch (IOException ignored) {}
            }
        }

        return isUpdateAvailable;
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
         * Called when the Info Message is received when Touch Portal confirms our initial connection is successful
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

        /**
         * Called when a Notification Option Clicked Message is received
         *
         * @param tpNotificationOptionClickedMessage TPNotificationOptionClickedMessage
         */
        void onNotificationOptionClicked(TPNotificationOptionClickedMessage tpNotificationOptionClickedMessage);
    }

    /**
     * Signals that the @Action Annotated Method have a parameter which is not @Data Annotated.
     */
    public static class MethodDataParameterException extends ReflectiveOperationException {
        /**
         * Constructor with a detail message.
         *
         * @param method    Method
         * @param parameter Parameter
         */
        public MethodDataParameterException(Method method, Parameter parameter) {
            super("Impossible to retrieve Action Data Item for Method [" + method.getName() + "] and parameter [" + parameter.getName() + "]");
        }
    }
    /**
     * Custom ConsoleHandler
     */
    private static class CustomConsoleHandler extends ConsoleHandler {
        public CustomConsoleHandler() {
            super();
            setFormatter(new SimpleFormatter() {
                private static final String format = "[%1$-7s] %2$s %3$s %n";

                @Override
                public synchronized String format(LogRecord lr) {
                    String[] path =  lr.getSourceClassName().split("\\.");
                    return String.format(format,
                            lr.getLevel().getLocalizedName(),
                            path[path.length - 1] + "." + lr.getSourceMethodName(),
                            lr.getMessage()
                    );
                }
            });
            setOutputStream(System.out);
        }
    }
}
