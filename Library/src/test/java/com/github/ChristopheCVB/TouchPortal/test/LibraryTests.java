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
import com.github.ChristopheCVB.TouchPortal.model.TPInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class LibraryTests {
    private ServerSocket serverSocket;
    private Socket serverSocketClient;
    private TouchPortalPluginTest touchPortalPluginTest;
    private final TouchPortalPlugin.TouchPortalPluginListener touchPortalPluginListener = new TouchPortalPlugin.TouchPortalPluginListener() {
        @Override
        public void onDisconnect(Exception exception) {
        }

        @Override
        public void onReceive(JSONObject jsonMessage) {
        }
    };

    @Before
    public void initialize() throws IOException {
        // Mock Server
        this.serverSocket = new ServerSocket(12136, 50, InetAddress.getByName("127.0.0.1"));

        File testResourcesDirectory = new File("src/test/resources/");
        this.touchPortalPluginTest = new TouchPortalPluginTest(new String[]{"start", testResourcesDirectory.getAbsolutePath() + "/"});

        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(this.touchPortalPluginListener);
        this.serverSocketAccept();
        assertTrue(connectedPairedAndListening);
    }

    private void serverSocketAccept() {
        new Thread(() -> {
            try {
                LibraryTests.this.serverSocketClient = LibraryTests.this.serverSocket.accept();
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
    public void close() throws IOException {
        if (this.touchPortalPluginTest != null) {
            this.touchPortalPluginTest.close(null);
        }
        if (this.serverSocketClient != null) {
            this.serverSocketClient.close();
        }
        if (this.serverSocket != null) {
            this.serverSocket.close();
        }
    }

    @Test
    public void testConnection() {
        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testConnectionFail() throws IOException {
        this.close();

        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(null);
        assertFalse(connectedPairedAndListening);
        assertFalse(this.touchPortalPluginTest.isConnected());

        connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(this.touchPortalPluginListener);
        assertFalse(connectedPairedAndListening);
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

        this.serverSocketAccept();
        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(null);
        assertTrue(connectedPairedAndListening);
    }

    @Test
    public void testConnectionNoListener() {
        this.touchPortalPluginTest.close(null);

        this.serverSocketAccept();
        boolean connectedPairedAndListening = this.touchPortalPluginTest.connectThenPairAndListen(null);
        assertTrue(connectedPairedAndListening);
    }

    @Test
    public void testClose() {
        this.touchPortalPluginTest.close(null);
    }

    @Test
    public void testSend() {
        // Send State Update by ID from Constants
        assertTrue(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "New Value 01"));

        // Send Choice Update by ID from Constants
        assertTrue(this.touchPortalPluginTest.sendChoiceUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, new String[]{"New Value 02"}));

        // Send Specific Choice Update by ID from Constants
        assertTrue(this.touchPortalPluginTest.sendSpecificChoiceUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "instanceId", new String[]{"New Value 03"}));

        // Send State Update by Reflection Generated ID
        assertTrue(this.touchPortalPluginTest.sendStateUpdate("BaseCategory", "customState", "New Value 04"));

        // Send Choice Update by Reflection Generated ID
        assertTrue(this.touchPortalPluginTest.sendChoiceUpdate("BaseCategory", "customState", new String[]{"New Value 05"}));

        // Send Specific Choice Update by Reflection Generated ID
        assertTrue(this.touchPortalPluginTest.sendSpecificChoiceUpdate("BaseCategory", "customState", "instanceId", new String[]{"New Value 06"}));
    }

    @Test
    public void testSendFalseStates() {
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(null, null));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate("", ""));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(null, ""));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate("", null));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, null));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, ""));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(null, "Not Null"));
    }

    @Test
    public void testLastStateValue() {
        String stateValue = System.currentTimeMillis() + "";
        assertTrue(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, stateValue));
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, stateValue));
        assertEquals(stateValue, this.touchPortalPluginTest.getLastStateValue(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID));
    }

    @Test
    public void testSendFail() {
        this.touchPortalPluginTest.close(null);
        assertFalse(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "New Value"));
    }

    @Test
    public void testReceiveAction() throws JSONException, IOException, InterruptedException {
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.put(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());
        Thread.sleep(10);
        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testReceiveActionNoListener() throws JSONException, IOException, InterruptedException {
        this.touchPortalPluginTest.connectThenPairAndListen(null);
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.put(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());
        Thread.sleep(10);
        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testReceiveBadPlugin() throws JSONException, IOException, InterruptedException {
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put(ReceivedMessageHelper.PLUGIN_ID, "falsePluginId");
        jsonMessage.put(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());
        Thread.sleep(10);
        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testReceiveInfo() throws JSONException, IOException, InterruptedException {
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_INFO);
        long sdkVersion = 2;
        long tpVersionCode = 202000;
        String tpVersionString = "2.2.000";
        jsonMessage.put(TPInfo.SDK_VERSION, sdkVersion);
        jsonMessage.put(TPInfo.TP_VERSION_CODE, tpVersionCode);
        jsonMessage.put(TPInfo.TP_VERSION_STRING, tpVersionString);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());
        Thread.sleep(10);
        assertTrue(this.touchPortalPluginTest.isConnected());

        TPInfo tpInfo = this.touchPortalPluginTest.getTPInfo();
        assertNotNull(tpInfo);
        assertEquals(sdkVersion, tpInfo.sdkVersion);
        assertEquals(tpVersionCode, tpInfo.tpVersionCode);
        assertEquals(tpVersionString, tpInfo.tpVersionString);
    }

    @Test
    public void testReceiveClose() throws JSONException, IOException, InterruptedException {
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.put(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_CLOSE_PLUGIN);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());

        // Wait for the listenerThread to catch up
        Thread.sleep(10);

        assertFalse(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testReceiveJSONFail() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println("Not a JSON Object");

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testReceiveEmpty() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println();

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testReceivePart() throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.print("");
        out.println();

        Thread.sleep(10);

        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testAnnotations() {
        assertEquals(TouchPortalPluginTestConstants.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithData.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.action.dummyWithData");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithoutData.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.action.dummyWithoutData");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.state.customState");
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Events.CustomState.ID, "com.github.ChristopheCVB.TouchPortal.test.TouchPortalPluginTest.BaseCategory.event.customState");
    }

    @Test
    public void testEntryTP() throws IOException, JSONException {
        File testGeneratedResourcesDirectory = new File("build/generated/sources/annotationProcessor/java/test/resources");

        JSONObject entry = new JSONObject(new String(Files.readAllBytes(Paths.get(new File(testGeneratedResourcesDirectory.getAbsolutePath() + "/entry.tp").getAbsolutePath()))));

        // Plugin
        assertEquals(TouchPortalPluginTestConstants.ID, entry.getString(PluginHelper.ID));

        // Categories
        JSONArray jsonCategories = entry.getJSONArray(PluginHelper.CATEGORIES);
        assertEquals(1, jsonCategories.length());

        // Base Category
        JSONObject baseCategory = jsonCategories.getJSONObject(0);
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.ID, baseCategory.getString(CategoryHelper.ID));

        // Base Category Actions
        JSONArray baseCategoryActions = baseCategory.getJSONArray(CategoryHelper.ACTIONS);
        assertEquals(2, baseCategoryActions.length());

        // Base Category Action DummyWithData
        JSONObject baseCategoryActionDummyWithData = baseCategoryActions.getJSONObject(0);
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithData.ID, baseCategoryActionDummyWithData.getString(ActionHelper.ID));

        // Base Category Action DummyWithData Data items
        JSONArray baseCategoryActionDummyWithDataItems = baseCategoryActionDummyWithData.getJSONArray(ActionHelper.DATA);
        assertEquals(1, baseCategoryActionDummyWithDataItems.length());

        // Base Category Action DummyWithData Data Text item
        JSONObject baseCategoryActionDummyWithDataItemText = baseCategoryActionDummyWithDataItems.getJSONObject(0);
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithData.Text.ID, baseCategoryActionDummyWithDataItemText.getString(DataHelper.ID));

        // Base Category Action DummyWithoutData
        JSONObject baseCategoryActionDummyWithoutData = baseCategoryActions.getJSONObject(1);
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithoutData.ID, baseCategoryActionDummyWithoutData.getString(ActionHelper.ID));

        // Base Category Action DummyWithoutData Data items
        JSONArray baseCategoryActionDummyWithoutDataItems = baseCategoryActionDummyWithoutData.getJSONArray(ActionHelper.DATA);
        assertEquals(0, baseCategoryActionDummyWithoutDataItems.length());

        // Base Category Events
        JSONArray baseCategoryEvents = baseCategory.getJSONArray(CategoryHelper.EVENTS);
        assertEquals(1, baseCategoryEvents.length());

        // Base Category Event Custom Event
        JSONObject baseCategoryEventCustomState = baseCategoryEvents.getJSONObject(0);
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Events.CustomState.ID, baseCategoryEventCustomState.getString(EventHelper.ID));

        // Base Category States
        JSONArray baseCategoryStates = baseCategory.getJSONArray(CategoryHelper.STATES);
        assertEquals(1, baseCategoryStates.length());

        // Base Category State Custom State
        JSONObject baseCategoryStateCustomState = baseCategoryStates.getJSONObject(0);
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, baseCategoryStateCustomState.getString(StateHelper.ID));
    }

    @Test
    public void testProperties() throws IOException {
        // Test reloadProperties without a Properties File
        this.touchPortalPluginTest.reloadProperties();

        this.touchPortalPluginTest.loadProperties("plugin.config");
        assertTrue(this.touchPortalPluginTest.getPropertiesFile().exists());

        assertEquals("Sample Value", this.touchPortalPluginTest.getProperty("samplekey"));

        String key = "key";
        String value = "value:" + System.currentTimeMillis();
        this.touchPortalPluginTest.setProperty(key, value);

        assertEquals(value, this.touchPortalPluginTest.getProperty(key));
        this.touchPortalPluginTest.storeProperties();
        this.touchPortalPluginTest.reloadProperties();
        assertEquals(value, this.touchPortalPluginTest.getProperty(key));

        this.touchPortalPluginTest.removeProperty(key);
        this.touchPortalPluginTest.storeProperties();
        assertNull(this.touchPortalPluginTest.getProperty(key));
    }

    @Test(expected = FileNotFoundException.class)
    public void testPropertiesFail() throws IOException {
        this.touchPortalPluginTest.loadProperties("doesNot.exists");
    }

    @Test
    public void testPropertiesAccess() {
        // No Loaded Properties File
        this.touchPortalPluginTest.removeProperty("non existent");
        this.touchPortalPluginTest.getProperty("non existent");
        this.touchPortalPluginTest.setProperty("non existent", "value");
    }
}
