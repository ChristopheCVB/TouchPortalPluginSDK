package com.github.ChristopheCVB.TouchPortal.test;

import com.github.ChristopheCVB.TouchPortal.Helpers.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LibraryTests {
    private ServerSocket serverSocket;
    private TouchPortalPluginTest touchPortalPluginTest;

    @Before
    public void initializeTPPlugin() throws IOException {
        this.serverSocket = new ServerSocket(12136, 50, InetAddress.getByName("127.0.0.1"));

        File testResourcesDirectory = new File("src/test/resources/");
        this.touchPortalPluginTest = new TouchPortalPluginTest(new String[]{"start", testResourcesDirectory.getAbsolutePath() + "/"});

        this.touchPortalPluginTest.connectThenPairAndListen(null);
        this.serverSocket.accept();
    }

    @After
    public void close() throws IOException {
        this.serverSocket.close();
    }

    @Test
    public void testConnection() {
        assertTrue(this.touchPortalPluginTest.isConnected());
    }

    @Test
    public void testSend() {
        assertTrue(this.touchPortalPluginTest.sendStateUpdate(TouchPortalPluginTestConstants.BaseCategory.States.CustomState.ID, "New Value"));
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
        File testGeneratedResourcesDirectory = new File("build\\generated\\sources\\annotationProcessor\\java\\test\\resources");

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
        this.touchPortalPluginTest.loadProperties("plugin.config");

        assertEquals("Sample Value", this.touchPortalPluginTest.getProperty("samplekey"));

        String key = "key";
        String value = "value:" + System.currentTimeMillis();
        this.touchPortalPluginTest.setProperty(key, value);

        assertEquals(value, this.touchPortalPluginTest.getProperty(key));

        this.touchPortalPluginTest.storeProperties();
        this.touchPortalPluginTest.reloadProperties();

        assertEquals(value, this.touchPortalPluginTest.getProperty(key));
    }

    @Test(expected = FileNotFoundException.class)
    public void testPropertiesFail() throws IOException {
        this.touchPortalPluginTest.loadProperties("doesNot.exists");
    }
}
