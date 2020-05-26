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

public abstract class TouchPortalPlugin {
    private static final String SOCKET_IP = "127.0.0.1";
    private static final int SOCKET_PORT = 12136;

    protected static final String VERSION = "2.0";

    protected static final String GENERIC_ID = "id";
    protected static final String GENERIC_NAME = "name";
    protected static final String GENERIC_TYPE = "type";
    protected static final String GENERIC_DESCRIPTION = "description";
    protected static final String GENERIC_VALUE = "value";
    protected static final String GENERIC_DEFAULT = "default";

    protected class Category {
        protected static final String ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String NAME = TouchPortalPlugin.GENERIC_NAME;
        protected static final String IMAGE_PATH = "imagepath";
        protected static final String ACTIONS = "actions";
        protected static final String EVENTS = "events";
        protected static final String STATES = "states";
    }

    protected class State {
        protected static final String ID = TouchPortalPlugin.GENERIC_ID;
        protected static final String TYPE = TouchPortalPlugin.GENERIC_TYPE;
        protected static final String TYPE_CHOICE = "choice";
        protected static final String TYPE_TEXT = "text";
        protected static final String DESCRIPTION = "desc";
        protected static final String DEFAULT = TouchPortalPlugin.GENERIC_DEFAULT;
        protected static final String VALUE_CHOICES = "valueChoices";
    }

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

    private final Class pluginClass;
    private Socket touchPortalSocket;
    private BufferedOutputStream bufferedOutputStream;
    private BufferedReader bufferedReader;
    private TouchPortalPluginListener touchPortalPluginListener;

    private final Thread listenerThread = new Thread(() -> {
        while (true) {
            try {
                String socketMessage = TouchPortalPlugin.this.bufferedReader.readLine();
                JSONObject jsonMessage = new JSONObject(socketMessage);
                if (jsonMessage.getString(Message.PLUGIN_ID).equals(TouchPortalPlugin.this.pluginClass.getName())) {
                    if (TouchPortalPlugin.this.touchPortalPluginListener != null) {
                        TouchPortalPlugin.this.touchPortalPluginListener.onReceive(jsonMessage);
                    }
                }
                else {
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

    protected TouchPortalPlugin(){
        this.pluginClass = this.getClass();
    }

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
            paired = this.sendPair();;
        }

        return connected && paired && this.listen();
    }

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

    private void close() {
        if (this.touchPortalSocket != null && this.touchPortalSocket.isConnected()) {
            try {
                this.bufferedOutputStream = null;
                this.bufferedReader = null;
                this.touchPortalSocket.close();
                this.touchPortalSocket = null;
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

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

    public boolean sendChoiceUpdate(String id, String[] values) {
        boolean sent = false;
        try {
            JSONObject choiceUpdateMessage = new JSONObject()
                    .put(Message.TYPE, Message.TYPE_CHOICE_UPDATE)
                    .put(Message.ID, id)
                    .put(Message.VALUE, new JSONArray(values));
            sent = this.send(choiceUpdateMessage);
            System.out.println("Update Choices[" + id + "] sent");
        }catch (JSONException e) {
            e.printStackTrace();
        }

        return sent;
    }

    public boolean sendSpecificChoiceUpdate(String id, String instanceId, String[] values) {
        boolean sent = false;
        try {
            JSONObject specificChoiceUpdateMessage = new JSONObject()
                    .put(Message.TYPE, Message.TYPE_CHOICE_UPDATE)
                    .put(Message.ID, id)
                    .put(Message.INSTANCE_ID, instanceId)
                    .put(Message.VALUE, new JSONArray(values));
            sent = this.send(specificChoiceUpdateMessage);
            System.out.println("Update Choices[" + id + "] sent");
        }catch (JSONException e) {
            e.printStackTrace();
        }

        return sent;
    }

    public boolean sendStateUpdate(String id, String value) {
        boolean sent = false;
        try {
            JSONObject stateUpdateMessage = new JSONObject()
                    .put(Message.TYPE, Message.TYPE_STATE_UPDATE)
                    .put(Message.ID, id)
                    .put(Message.VALUE, value);
            sent = this.send(stateUpdateMessage);
            System.out.println("Update State[" + id + "] sent");
        }catch (JSONException e) {
            e.printStackTrace();
        }

        return sent;
    }

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

    public static String getActionId(JSONObject jsonAction) {
        String actionId = null;

        try {
            actionId = jsonAction.getString(Message.ACTION_ID);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return actionId;
    }

    public static String getMessageType(JSONObject jsonMessage) {
        String messageType = null;

        try {
            messageType = jsonMessage.getString(Message.TYPE);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return messageType;
    }

    public interface TouchPortalPluginListener {
        void onDisconnect(Exception exception);
        void onReceive(JSONObject message);
    }
}
