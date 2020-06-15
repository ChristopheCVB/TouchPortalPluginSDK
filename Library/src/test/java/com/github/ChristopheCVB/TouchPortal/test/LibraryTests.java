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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        public void onReceive(JsonObject jsonMessage) {
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
    public void testReceiveAction() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_ACTION);
        PrintWriter out = new PrintWriter(this.serverSocketClient.getOutputStream(), true);
        out.println(jsonMessage.toString());
        Thread.sleep(10);
        assertTrue(this.touchPortalPluginTest.isConnected());
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
    }

    @Test
    public void testReceiveInfo() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_INFO);
        long sdkVersion = 2;
        long tpVersionCode = 202000;
        String tpVersionString = "2.2.000";
        jsonMessage.addProperty(TPInfo.SDK_VERSION, sdkVersion);
        jsonMessage.addProperty(TPInfo.TP_VERSION_CODE, tpVersionCode);
        jsonMessage.addProperty(TPInfo.TP_VERSION_STRING, tpVersionString);
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
    public void testReceiveClose() throws IOException, InterruptedException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty(ReceivedMessageHelper.PLUGIN_ID, TouchPortalPluginTestConstants.ID);
        jsonMessage.addProperty(ReceivedMessageHelper.TYPE, ReceivedMessageHelper.TYPE_CLOSE_PLUGIN);
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
    public void testEntryTP() throws IOException {
        File testGeneratedResourcesDirectory = new File("build/generated/sources/annotationProcessor/java/test/resources");

        JsonObject entry = new JsonParser().parse(new String(Files.readAllBytes(Paths.get(new File(testGeneratedResourcesDirectory.getAbsolutePath() + "/entry.tp").getAbsolutePath())))).getAsJsonObject();

        // Plugin
        assertEquals(TouchPortalPluginTestConstants.ID, entry.get(PluginHelper.ID).getAsString());

        // Categories
        JsonArray jsonCategories = entry.getAsJsonArray(PluginHelper.CATEGORIES);
        assertEquals(1, jsonCategories.size());

        // Base Category
        JsonObject baseCategory = jsonCategories.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.ID, baseCategory.get(CategoryHelper.ID).getAsString());

        // Base Category Actions
        JsonArray baseCategoryActions = baseCategory.getAsJsonArray(CategoryHelper.ACTIONS);
        assertEquals(2, baseCategoryActions.size());

        // Base Category Action DummyWithData
        JsonObject baseCategoryActionDummyWithData = baseCategoryActions.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithData.ID, baseCategoryActionDummyWithData.get(ActionHelper.ID).getAsString());

        // Base Category Action DummyWithData Data items
        JsonArray baseCategoryActionDummyWithDataItems = baseCategoryActionDummyWithData.getAsJsonArray(ActionHelper.DATA);
        assertEquals(1, baseCategoryActionDummyWithDataItems.size());

        // Base Category Action DummyWithData Data Text item
        JsonObject baseCategoryActionDummyWithDataItemText = baseCategoryActionDummyWithDataItems.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithData.Text.ID, baseCategoryActionDummyWithDataItemText.get(DataHelper.ID).getAsString());

        // Base Category Action DummyWithoutData
        JsonObject baseCategoryActionDummyWithoutData = baseCategoryActions.get(1).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Actions.DummyWithoutData.ID, baseCategoryActionDummyWithoutData.get(ActionHelper.ID).getAsString());

        // Base Category Action DummyWithoutData Data items
        JsonArray baseCategoryActionDummyWithoutDataItems = baseCategoryActionDummyWithoutData.getAsJsonArray(ActionHelper.DATA);
        assertEquals(0, baseCategoryActionDummyWithoutDataItems.size());

        // Base Category Events
        JsonArray baseCategoryEvents = baseCategory.getAsJsonArray(CategoryHelper.EVENTS);
        assertEquals(1, baseCategoryEvents.size());

        // Base Category Event Custom Event
        JsonObject baseCategoryEventCustomState = baseCategoryEvents.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.Events.CustomState.ID, baseCategoryEventCustomState.get(EventHelper.ID).getAsString());

        // Base Category States
        JsonArray baseCategoryStates = baseCategory.getAsJsonArray(CategoryHelper.STATES);
        assertEquals(1, baseCategoryStates.size());

        // Base Category State Custom State
        JsonObject baseCategoryStateCustomState = baseCategoryStates.get(0).getAsJsonObject();
        assertEquals(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, baseCategoryStateCustomState.get(StateHelper.ID).getAsString());
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
