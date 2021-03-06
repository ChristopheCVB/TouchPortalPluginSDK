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

package com.github.ChristopheCVB.TouchPortal.test;

import com.github.ChristopheCVB.TouchPortal.Helpers.*;
import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin;
import com.github.ChristopheCVB.TouchPortal.model.TPBroadcastMessage;
import com.github.ChristopheCVB.TouchPortal.model.TPInfoMessage;
import com.github.ChristopheCVB.TouchPortal.model.TPListChangeMessage;
import com.github.ChristopheCVB.TouchPortal.model.TPSettingsMessage;
import com.github.ChristopheCVB.TouchPortal.oauth2.OAuth2LocalServerReceiver;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.*;

public class LibraryTests {
    private ServerSocket serverSocket;
    private Socket serverSocketClient;
    private TouchPortalPluginTest touchPortalPluginTest;
    private final TouchPortalPlugin.TouchPortalPluginListener touchPortalPluginListener = new TouchPortalPlugin.TouchPortalPluginListener() {
        @Override
        public void onDisconnected(Exception exception) {
        }

        @Override
        public void onReceived(JsonObject jsonMessage) {
        }

        @Override
        public void onInfo(TPInfoMessage tpInfoMessage) {
        }

        @Override
        public void onListChanged(TPListChangeMessage tpListChangeMessage) {
        }

        @Override
        public void onBroadcast(TPBroadcastMessage tpBroadcastMessage) {
        }

        @Override
        public void onSettings(TPSettingsMessage tpSettingsMessage) {
        }
    };

    @Before
    public void initialize() throws IOException {
        // Mock Server
        this.serverSocket = new ServerSocket(12136, 50, InetAddress.getByName("127.0.0.1"));

        this.touchPortalPluginTest = new TouchPortalPluginTest();

        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(this.touchPortalPluginListener);
        this.serverSocketAccept();
        assertTrue(connectedPairedAndListening);
    }

    private void serverSocketAccept() {
        new Thread(() -> {
            try {
                if (this.serverSocketClient != null) {
                    this.serverSocketClient.close();
                }
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
            try {
                this.serverSocketClient = this.serverSocket.accept();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }).start();
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException ignored) {}
    }

    @After
    public void close() {
        try {
            if (this.serverSocketClient != null) {
                this.serverSocketClient.close();
            }
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        try {
            Thread.sleep(10);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
//        if (this.touchPortalPluginTest != null) {
//            this.touchPortalPluginTest.close(null);
//        }
    }

    @Test
    public void testConnection() {
        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testConnectionFail() {
        this.close();

        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(null);
        assertFalse(connectedPairedAndListening);
        assertFalse(this.touchPortalPluginTest.isListening());
        assertFalse(this.touchPortalPluginTest.isConnected());

        connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(this.touchPortalPluginListener);
        assertFalse(connectedPairedAndListening);
        assertFalse(this.touchPortalPluginTest.isListening());
        assertFalse(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testMultipleConnect() {
        // A connectThenPairAndListen is already done in the @Before
        assertTrue(this.touchPortalPluginTest.connectThenPairAndListen(null));
        assertTrue(this.touchPortalPluginTest.connectThenPairAndListen(this.touchPortalPluginListener));
    }

    @Test
    public void testCloseAndReConnect() {
        this.touchPortalPluginTest.close(null);

        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(null);
        this.serverSocketAccept();
        assertTrue(connectedPairedAndListening);
    }

    @Test
    public void testConnectionNoListener() {
        this.touchPortalPluginTest.close(null);

        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(null);
        this.serverSocketAccept();
        assertTrue(connectedPairedAndListening);
    }

    @Test
    public void testClose() {
        this.touchPortalPluginTest.close(null);
    }

    @Test
    public void testServerSocketCloses() throws IOException, InterruptedException {
        this.serverSocketClient.close();
        this.serverSocket.close();
        Thread.sleep(100);
        assertFalse(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testSend() {
        // Send State Update by ID from Constants
        assertTrue(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "New Value 01"));

        // Send Choice Update by ID from Constants
        assertTrue(this.touchPortalPluginTest.sendChoiceUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, new String[]{"New Value 02", null}));

        // Send Specific Choice Update by ID from Constants
        assertTrue(this.touchPortalPluginTest.sendSpecificChoiceUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "instanceId", new String[]{"New Value 03", null}));

        // Send State Update by Reflection Generated ID
        assertTrue(this.touchPortalPluginTest.sendStateUpdate("BaseCategory", "customState", "New Value 04"));

        // Send Choice Update by Reflection Generated ID
        assertTrue(this.touchPortalPluginTest.sendChoiceUpdate("BaseCategory", "customState", new String[]{"New Value 05"}));

        // Send Specific Choice Update by Reflection Generated ID
        assertTrue(this.touchPortalPluginTest.sendSpecificChoiceUpdate("BaseCategory", "customState", "instanceId", new String[]{"New Value 06"}));
    }

    @Test
    public void testSendStates() {
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(null, null));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate("", null));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate("", ""));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(null, ""));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, null));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, ""));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(null, "Not Null"));
        assertTrue(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "", true));
    }

    @Test
    public void testSendChoices() {
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate(null, null));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate("", new String[0]));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate(null, new String[0]));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate("", null));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate("listId", null));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate("listId", new String[0]));
        assertTrue(this.touchPortalPluginTest.sendChoiceUpdate("listId", new String[0], true));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate("listId", new String[0], true));
        assertTrue(this.touchPortalPluginTest.sendChoiceUpdate("listId", null, true));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate("listId", null, true));
    }

    @Test
    public void testSendSpecificChoices() {
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate(null, null, null));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("", null, null));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("", "", null));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("", "", new String[0]));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", null, null));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "", null));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "instanceId", new String[0]));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "instanceId", null));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate(null, null, new String[0]));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate(null, "instanceId", null));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("", "instanceId", null));
        assertTrue(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "instanceId", new String[0], true));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "instanceId", new String[0], true));
        assertTrue(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "instanceId", null, true));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "instanceId", null, true));
    }

    @Test
    public void testSendActionDataUpdate() {
        HashMap<String, Number> props = new HashMap<>();
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate(null, null, null));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("", null, null));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("", "", null));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("", "", props));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("listId", null, null));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("listId", "", null));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("listId", "instanceId", props));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("listId", "instanceId", null));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate(null, null, props));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate(null, "instanceId", null));
        assertFalse(this.touchPortalPluginTest.sendActionDataUpdate("", "instanceId", null));
        props.put(DataHelper.MIN_VALUE, -1);
        assertTrue(this.touchPortalPluginTest.sendActionDataUpdate("listId", "instanceId", props));
    }

    @Test
    public void testLastStateValue() {
        String stateValue = System.currentTimeMillis() + "";
        assertTrue(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, stateValue));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, stateValue));
        assertEquals(stateValue, this.touchPortalPluginTest.getLastStateValue(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID));
    }

    @Test
    public void testDynamicStates() {
        assertFalse(this.touchPortalPluginTest.sendCreateState(null, null, null, null));

        assertFalse(this.touchPortalPluginTest.sendCreateState("", null, null, null));
        assertFalse(this.touchPortalPluginTest.sendCreateState(null, "", null, null));
        assertFalse(this.touchPortalPluginTest.sendCreateState(null, null, "", null));
        assertFalse(this.touchPortalPluginTest.sendCreateState(null, null, null, ""));
        assertFalse(this.touchPortalPluginTest.sendCreateState("", "", "", ""));

        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", null, null, null));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "", null, null));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", null, "", null));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", null, null, ""));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "", "", ""));

        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", null, null));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "", null));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", null, ""));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "", ""));

        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "Dynamically Created State", null));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "Dynamically Created State", ""));

        assertTrue(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "Dynamically Created State", "Default Value 01"));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "Dynamically Created State", "Default Value 01"));
        assertTrue(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "Dynamically Created State", "Default Value 02"));

        assertFalse(this.touchPortalPluginTest.sendRemoveState(null, null));
        assertFalse(this.touchPortalPluginTest.sendRemoveState("", null));
        assertFalse(this.touchPortalPluginTest.sendRemoveState(null, ""));
        assertFalse(this.touchPortalPluginTest.sendRemoveState("", ""));
        assertFalse(this.touchPortalPluginTest.sendRemoveState("CategoryId", null));
        assertFalse(this.touchPortalPluginTest.sendRemoveState("CategoryId", ""));

        assertTrue(this.touchPortalPluginTest.sendRemoveState("CategoryId", "SateId"));
    }

    @Test
    public void testSendFail() {
        this.touchPortalPluginTest.close(null);
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "New Value"));
        assertFalse(this.touchPortalPluginTest.sendChoiceUpdate("listId", null, true));
        assertFalse(this.touchPortalPluginTest.sendSpecificChoiceUpdate("listId", "instanceId", null, true));
        assertFalse(this.touchPortalPluginTest.sendCreateState("CategoryId", "SateId", "Dynamically Created State", "Default Value 01"));
        assertFalse(this.touchPortalPluginTest.sendRemoveState("CategoryId", "SateId"));
    }

    @Test
    public void testReceiveActionNoId() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveActionEmptyId() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        jsonMessage.addProperty(ReceivedMessageHelper.ACTION_ID, "");
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveDummyWithDataTextAndNumberAction() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        jsonMessage.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataTextAndNumber.ID);
        JsonArray data = new JsonArray();
        JsonObject textDataItem = new JsonObject();
        textDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataTextAndNumber.Text.ID);
        textDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_VALUE, "Text from Tests !");
        data.add(textDataItem);
        JsonObject numberDataItem = new JsonObject();
        numberDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataTextAndNumber.Number.ID);
        numberDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_VALUE, 42);
        data.add(numberDataItem);
        jsonMessage.add(ActionHelper.DATA, data);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveDummyWithDataFileAction() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        jsonMessage.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataFileAndDirectory.ID);
        JsonArray data = new JsonArray();
        JsonObject fileDataItem = new JsonObject();
        fileDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataFileAndDirectory.File.ID);
        fileDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_VALUE, "/");
        data.add(fileDataItem);
        jsonMessage.add(ActionHelper.DATA, data);
        JsonObject directoryDataItem = new JsonObject();
        directoryDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataFileAndDirectory.Directory.ID);
        directoryDataItem.addProperty(ReceivedMessageHelper.ACTION_DATA_VALUE, "/");
        data.add(directoryDataItem);
        jsonMessage.add(ActionHelper.DATA, data);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveDummyDummyWithJsonObject() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        jsonMessage.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithJsonObject.ID);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveDummyDummyWithTPActionMessage() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        jsonMessage.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithTPActionMessage.ID);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveDummyDummyWithParam() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        jsonMessage.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithParam.ID);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveActionHoldableDownAndUp() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);

        JsonObject jsonMessageHoldDown = new JsonObject();
        jsonMessageHoldDown.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessageHoldDown.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_HOLD_DOWN);
        jsonMessageHoldDown.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID);
        out.println(jsonMessageHoldDown.toString());

        Thread.sleep(150);

        assertTrue(this.touchPortalPluginTest.isActionBeingHeld(TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID));

        JsonObject jsonMessageHoldUp = new JsonObject();
        jsonMessageHoldUp.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessageHoldUp.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_HOLD_UP);
        jsonMessageHoldUp.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID);
        out.println(jsonMessageHoldUp.toString());

        Thread.sleep(150);

        assertNull(this.touchPortalPluginTest.isActionBeingHeld(TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID));

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveActionHoldablePress() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);

        JsonObject jsonMessageHoldDown = new JsonObject();
        jsonMessageHoldDown.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessageHoldDown.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        jsonMessageHoldDown.addProperty(ReceivedMessageHelper.ACTION_ID, TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID);
        out.println(jsonMessageHoldDown.toString());

        Thread.sleep(10);

        assertNull(this.touchPortalPluginTest.isActionBeingHeld(TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID));

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveListChange() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_LIST_CHANGE);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveListChangeNoListener() throws IOException, InterruptedException {
        this.touchPortalPluginTest.connectThenPairAndListen(null);
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_LIST_CHANGE);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveActionNoListener() throws IOException, InterruptedException {
        this.touchPortalPluginTest.connectThenPairAndListen(null);
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveBadPlugin() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, "falsePluginId");
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveNoMessageType() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveUnknownMessageType() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, "unknown");
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveInfo() throws IOException, InterruptedException {
        TPInfoMessage sentTPInfoMessage = new TPInfoMessage();
        sentTPInfoMessage.status = "paired";
        sentTPInfoMessage.sdkVersion = 3L;
        sentTPInfoMessage.tpVersionString = "2.3.000";
        sentTPInfoMessage.tpVersionCode = 203000L;
        sentTPInfoMessage.pluginVersion = 1L;
        sentTPInfoMessage.settings = new HashMap<>();
        sentTPInfoMessage.settings.put("IP", "localhost");

        JsonArray jsonSettings = new JsonArray();
        JsonObject jsonSettingIP = new JsonObject();
        jsonSettingIP.addProperty("IP", "localhost");
        jsonSettings.add(jsonSettingIP);

        JsonObject jsonMessage = new Gson().toJsonTree(sentTPInfoMessage).getAsJsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_INFO);
        jsonMessage.add(ReceivedMessageHelper.SETTINGS, jsonSettings);

        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());

        TPInfoMessage receivedTPInfoMessage = this.touchPortalPluginTest.getTPInfo();
        assertNotNull(receivedTPInfoMessage);
        assertEquals(sentTPInfoMessage.status, receivedTPInfoMessage.status);
        assertEquals(sentTPInfoMessage.sdkVersion, receivedTPInfoMessage.sdkVersion);
        assertEquals(sentTPInfoMessage.tpVersionCode, receivedTPInfoMessage.tpVersionCode);
        assertEquals(sentTPInfoMessage.tpVersionString, receivedTPInfoMessage.tpVersionString);
        assertEquals(sentTPInfoMessage.pluginVersion, receivedTPInfoMessage.pluginVersion);
        assertEquals(sentTPInfoMessage.settings, receivedTPInfoMessage.settings);
    }

    @Test
    public void testReceiveInfoMissingPropsAndNoListener() throws IOException, InterruptedException {
        this.touchPortalPluginTest.connectThenPairAndListen(null);
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_INFO);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());

        TPInfoMessage tpInfoMessage = this.touchPortalPluginTest.getTPInfo();
        assertNotNull(tpInfoMessage);
        assertNull(tpInfoMessage.status);
        assertNull(tpInfoMessage.sdkVersion);
        assertNull(tpInfoMessage.tpVersionCode);
        assertNull(tpInfoMessage.tpVersionString);
        assertNull(tpInfoMessage.pluginVersion);
    }

    @Test
    public void testReceiveClose() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_CLOSE_PLUGIN);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        // Wait for the listenerThread to catch up
        Thread.sleep(10);

        assertFalse(this.touchPortalPluginTest.isConnected());
        assertFalse(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveJSONFail() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println("Not a JSON Object");

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveEmpty() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println();

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceivePart() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.print("This");
        out.print("is");
        out.print("partial");
        out.print("data");
        out.println();

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testReceiveBroadcast() throws IOException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_BROADCAST);
        jsonMessage.addProperty(ReceivedMessageHelper.EVENT, ReceivedMessageHelper.EVENT_PAGE_CHANGE);
        jsonMessage.addProperty(ReceivedMessageHelper.PAGE_NAME, "Page ONE");
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());
    }

    @Test
    public void testReceiveBroadcastMissingPropsAndNoListener() throws IOException {
        this.touchPortalPluginTest.connectThenPairAndListen(null);
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_BROADCAST);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());
    }

    @Test
    public void testReceiveSettings() throws IOException, InterruptedException {
        JsonArray jsonSettings = new JsonArray();

        JsonObject jsonSettingIP = new JsonObject();
        String newIPSetting = "192.168.0.1";
        jsonSettingIP.addProperty(TouchPortalPluginTestConstants.Settings.IpSetting.NAME, newIPSetting);
        jsonSettings.add(jsonSettingIP);

        JsonObject jsonSettingOther = new JsonObject();
        String newOtherSetting = "9";
        jsonSettingOther.addProperty(TouchPortalPluginTestConstants.Settings.OtherSetting.NAME, newOtherSetting);
        jsonSettings.add(jsonSettingOther);

        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_SETTINGS);
        jsonMessage.add(ReceivedMessageHelper.VALUES, jsonSettings);

        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());

        assertEquals(this.touchPortalPluginTest.ipSetting, newIPSetting);
        assertEquals(this.touchPortalPluginTest.otherSetting, Double.valueOf(newOtherSetting).floatValue(), 0);
    }

    @Test
    public void testReceiveSettingsNoListener() throws IOException, InterruptedException {
        this.touchPortalPluginTest.connectThenPairAndListen(null);
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_SETTINGS);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
        assertTrue(this.touchPortalPluginTest.isListening());
    }

    @Test
    public void testSendSettingUpdate() throws IOException, InterruptedException {
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("DOES NOT EXISTS", "VALUE", false));

        TPInfoMessage sentTPInfoMessage = new TPInfoMessage();
        sentTPInfoMessage.status = "paired";
        sentTPInfoMessage.sdkVersion = 3L;
        sentTPInfoMessage.tpVersionString = "2.3.000";
        sentTPInfoMessage.tpVersionCode = 203000L;
        sentTPInfoMessage.pluginVersion = 1L;

        JsonArray jsonSettings = new JsonArray();
        JsonObject jsonSettingReadOnly = new JsonObject();
        jsonSettingReadOnly.addProperty(TouchPortalPluginTestConstants.Settings.ReadOnlySetting.NAME, TouchPortalPluginTestConstants.Settings.ReadOnlySetting.DEFAULT);
        jsonSettings.add(jsonSettingReadOnly);

        JsonObject jsonMessage = new Gson().toJsonTree(sentTPInfoMessage).getAsJsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_INFO);
        jsonMessage.add(ReceivedMessageHelper.SETTINGS, jsonSettings);

        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        Thread.sleep(10);

        assertFalse(this.touchPortalPluginTest.sendSettingUpdate(null, null, false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("", null, false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate(null, "", false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("", "", false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("DOES NOT EXISTS", "", false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("DOES NOT EXISTS", null, false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("DOES NOT EXISTS", "", true));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("DOES NOT EXISTS", "VALUE", false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate("DOES NOT EXISTS", "VALUE", true));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate(TouchPortalPluginTestConstants.Settings.ReadOnlySetting.NAME, "", false));
        assertTrue(this.touchPortalPluginTest.sendSettingUpdate(TouchPortalPluginTestConstants.Settings.ReadOnlySetting.NAME, "", true));
        assertTrue(this.touchPortalPluginTest.sendSettingUpdate(TouchPortalPluginTestConstants.Settings.ReadOnlySetting.NAME, "New value", false));
        assertFalse(this.touchPortalPluginTest.sendSettingUpdate(TouchPortalPluginTestConstants.Settings.ReadOnlySetting.NAME, "New value", false));

        this.touchPortalPluginTest.close(null);

        assertFalse(this.touchPortalPluginTest.sendSettingUpdate(TouchPortalPluginTestConstants.Settings.ReadOnlySetting.NAME, "New value 2", false));
    }

    @Test
    public void testAnnotations() {
        assertEquals(TouchPortalPluginTestConstants.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataTextAndNumber.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.action.dummyWithDataTextAndNumber");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithJsonObject.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.action.dummyWithJsonObject");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.state.customState");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Events.CustomState.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.event.customState");
    }

    @Test
    public void testEntryTPAndConstants() throws IOException {
        File testGeneratedResourcesDirectory = new File("../../../../build/generated/sources/annotationProcessor/java/test/resources");

        BufferedReader reader = Files.newBufferedReader(Paths.get(new File(testGeneratedResourcesDirectory.getAbsolutePath() + "/entry.tp").getAbsolutePath()));
        JsonObject entry = JsonParser.parseReader(reader).getAsJsonObject();
        reader.close();

        // Plugin
        assertEquals(TouchPortalPluginTestConstants.ID, entry.get(PluginHelper.ID).getAsString());

        // Settings
        assertEquals(TouchPortalPluginTestConstants.Settings.IpSetting.NAME, entry.get(PluginHelper.SETTINGS).getAsJsonArray().get(0).getAsJsonObject().get(SettingHelper.NAME).getAsString());
        assertEquals(3, entry.get(PluginHelper.SETTINGS).getAsJsonArray().size());

        // Categories
        JsonArray jsonCategories = entry.getAsJsonArray(PluginHelper.CATEGORIES);
        assertEquals(1, jsonCategories.size());

        // Base Category
        JsonObject baseCategory = jsonCategories.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.ID, baseCategory.get(CategoryHelper.ID).getAsString());

        // Base Category Actions
        JsonArray baseCategoryActions = baseCategory.getAsJsonArray(CategoryHelper.ACTIONS);
        assertEquals(7, baseCategoryActions.size());

        // Base Category Action DummyWithoutData
        JsonObject baseCategoryActionDummyWithoutData = baseCategoryActions.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithJsonObject.ID, baseCategoryActionDummyWithoutData.get(ActionHelper.ID).getAsString());

        // Base Category Action DummyWithoutData Data items
        assertFalse(baseCategoryActionDummyWithoutData.has(ActionHelper.DATA));

        // Base Category Action DummyWithDataTextAndNumber
        JsonObject baseCategoryActionDummyWithData = baseCategoryActions.get(2).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataTextAndNumber.ID, baseCategoryActionDummyWithData.get(ActionHelper.ID).getAsString());

        // Base Category Action DummyWithDataTextAndNumber Data items
        JsonArray baseCategoryActionDummyWithDataItems = baseCategoryActionDummyWithData.getAsJsonArray(ActionHelper.DATA);
        assertEquals(2, baseCategoryActionDummyWithDataItems.size());

        // Base Category Action DummyWithData Data Text item
        JsonObject baseCategoryActionDummyWithDataItemText = baseCategoryActionDummyWithDataItems.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataTextAndNumber.Text.ID, baseCategoryActionDummyWithDataItemText.get(DataHelper.ID).getAsString());

        // Base Category Action dummyWithData File And Directory
        JsonObject baseCategoryActionDummyWithDataFileAndDirectory = baseCategoryActions.get(3).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithDataFileAndDirectory.ID, baseCategoryActionDummyWithDataFileAndDirectory.get(ActionHelper.ID).getAsString());

        // Base Category Action DummyWithDataFile Data items
        JsonArray baseCategoryActionDummyWithDataFileItems = baseCategoryActionDummyWithDataFileAndDirectory.getAsJsonArray(ActionHelper.DATA);
        assertEquals(2, baseCategoryActionDummyWithDataFileItems.size());

        // Base Category Events
        JsonArray baseCategoryEvents = baseCategory.getAsJsonArray(CategoryHelper.EVENTS);
        assertEquals(1, baseCategoryEvents.size());

        // Base Category Event Custom Event
        JsonObject baseCategoryEventCustomState = baseCategoryEvents.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Events.CustomState.ID, baseCategoryEventCustomState.get(EventHelper.ID).getAsString());

        // Base Category States
        JsonArray baseCategoryStates = baseCategory.getAsJsonArray(CategoryHelper.STATES);
        assertEquals(2, baseCategoryStates.size());

        // Base Category State Custom State
        JsonObject baseCategoryStateCustomState = baseCategoryStates.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, baseCategoryStateCustomState.get(StateHelper.ID).getAsString());
    }

    @Test
    public void testProperties() {
        // Test reloadProperties without a Properties File
        assertFalse(this.touchPortalPluginTest.reloadProperties());

        assertTrue(this.touchPortalPluginTest.loadProperties("plugin.config"));
        assertTrue(this.touchPortalPluginTest.getPropertiesFile().exists());

        assertEquals("Sample Value", this.touchPortalPluginTest.getProperty("samplekey"));

        String key = "key";
        String value = "value:" + System.currentTimeMillis();
        this.touchPortalPluginTest.setProperty(key, value);

        assertEquals(value, this.touchPortalPluginTest.getProperty(key));
        assertEquals(value, this.touchPortalPluginTest.getProperty(key, ""));
        assertTrue(this.touchPortalPluginTest.storeProperties());
        assertTrue(this.touchPortalPluginTest.reloadProperties());
        assertEquals(value, this.touchPortalPluginTest.getProperty(key));

        this.touchPortalPluginTest.removeProperty(key);
        assertTrue(this.touchPortalPluginTest.storeProperties());
        assertNull(this.touchPortalPluginTest.getProperty(key));

        String defaultValue = "default";
        assertEquals(defaultValue, this.touchPortalPluginTest.getProperty("does not exists", defaultValue));
    }

    @Test
    public void testPropertiesFail() {
        assertFalse(this.touchPortalPluginTest.loadProperties("doesNot.exists"));
    }

    @Test
    public void testPropertiesAccess() {
        // No Loaded Properties File
        assertNull(this.touchPortalPluginTest.removeProperty("non existent"));
        assertNull(this.touchPortalPluginTest.getProperty("non existent"));
        assertNull(this.touchPortalPluginTest.setProperty("non existent", "value"));
    }

    @Test
    public void testIsUpdateAvailable() {
        assertFalse(this.touchPortalPluginTest.isUpdateAvailable("", 0));
        assertFalse(this.touchPortalPluginTest.isUpdateAvailable("https://raw.githubusercontent.com/ChristopheCVB/TouchPortalPluginSDK/master/Library/src/test/resources/TouchPortalPluginTest/plugin.config", 1));
        assertTrue(this.touchPortalPluginTest.isUpdateAvailable("https://raw.githubusercontent.com/ChristopheCVB/TouchPortalPluginSDK/master/Library/src/test/resources/TouchPortalPluginTest/plugin.config", 0));
    }

    @Test
    public void testOAuth2() throws IOException {
        String host = "localhost";
        String callbackPath = "/oauth";
        int port = -1;
        OAuth2LocalServerReceiver.Builder builder = new OAuth2LocalServerReceiver.Builder().setHost(host).setPort(port).setCallbackPath(callbackPath);
        assertEquals(host, builder.getHost());
        assertEquals(callbackPath, builder.getCallbackPath());
        assertEquals(port, builder.getPort());
        OAuth2LocalServerReceiver oAuth2LocalServerReceiver = builder.build();
        assertEquals(host, oAuth2LocalServerReceiver.getHost());
        assertEquals(callbackPath, oAuth2LocalServerReceiver.getCallbackPath());
        assertNotEquals(port, oAuth2LocalServerReceiver.getPort());
        oAuth2LocalServerReceiver.waitForCode(System.out::println, URI.create("https://authorization-server.com/authorize?state=" + oAuth2LocalServerReceiver.getPort()), (oAuth2Code, oAuth2Error) -> System.out.println("Code: " + oAuth2Code + " Error: " + oAuth2Error));

        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Call callbackOptions = okHttpClient.newCall(new Request.Builder().url("http://localhost:" + oAuth2LocalServerReceiver.getPort() + oAuth2LocalServerReceiver.getCallbackPath() + "?code=CODE").method("OPTIONS", null).build());
        Response responseOptions = callbackOptions.execute();
        assertTrue(responseOptions.isSuccessful());

        Call callbackGet = okHttpClient.newCall(new Request.Builder().url("http://localhost:" + oAuth2LocalServerReceiver.getPort() + oAuth2LocalServerReceiver.getCallbackPath() + "?code=CODE").build());
        Response responseGet = callbackGet.execute();
        assertTrue(responseGet.isSuccessful());
    }
}
