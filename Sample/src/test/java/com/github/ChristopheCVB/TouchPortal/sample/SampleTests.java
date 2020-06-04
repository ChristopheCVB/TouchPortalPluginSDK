package com.github.ChristopheCVB.TouchPortal.sample;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SampleTests {
    private ServerSocket serverSocket;
    private TouchPortalPluginExample touchPortalPluginExample;

    @Before
    public void initializeTPPlugin() throws IOException {
        this.serverSocket = new ServerSocket(12136, 50, InetAddress.getByName("127.0.0.1"));

        File testResourceDirectory = new File("src/test/resources/");
        this.touchPortalPluginExample = new TouchPortalPluginExample(new String[]{"start", testResourceDirectory.getAbsolutePath() + "/"});

        this.touchPortalPluginExample.connectThenPairAndListen(null);
        this.serverSocket.accept();
    }

    @After
    public void close() throws IOException {
        this.serverSocket.close();
    }

    @Test
    public void testConnection() throws IOException {
        assertTrue(this.touchPortalPluginExample.isConnected());
    }

    @Test
    public void testSend() {
        assertTrue(this.touchPortalPluginExample.sendStateUpdate(TouchPortalPluginExampleConstants.BaseCategory.States.CustomState.ID, "New Value"));
    }

    @Test
    public void testAnnotations() {
        assertEquals(TouchPortalPluginExampleConstants.ID, "com.github.ChristopheCVB.TouchPortal.sample.TouchPortalPluginExample");
        assertEquals(TouchPortalPluginExampleConstants.BaseCategory.ID, "com.github.ChristopheCVB.TouchPortal.sample.TouchPortalPluginExample.BaseCategory");
        assertEquals(TouchPortalPluginExampleConstants.BaseCategory.Actions.DummyWithData.ID, "com.github.ChristopheCVB.TouchPortal.sample.TouchPortalPluginExample.BaseCategory.action.dummyWithData");
        assertEquals(TouchPortalPluginExampleConstants.BaseCategory.Actions.DummyWithoutData.ID, "com.github.ChristopheCVB.TouchPortal.sample.TouchPortalPluginExample.BaseCategory.action.dummyWithoutData");
        assertEquals(TouchPortalPluginExampleConstants.BaseCategory.States.CustomState.ID, "com.github.ChristopheCVB.TouchPortal.sample.TouchPortalPluginExample.BaseCategory.state.customState");
        assertEquals(TouchPortalPluginExampleConstants.BaseCategory.Events.CustomState.ID, "com.github.ChristopheCVB.TouchPortal.sample.TouchPortalPluginExample.BaseCategory.event.customState");
    }

    @Test
    public void testProperties() throws IOException {
        this.touchPortalPluginExample.loadProperties("plugin.config");

        assertEquals("Sample Value", this.touchPortalPluginExample.getProperty("samplekey"));

        String key = "key";
        String value = "value:" + System.currentTimeMillis();
        this.touchPortalPluginExample.setProperty(key, value);

        assertEquals(value, this.touchPortalPluginExample.getProperty(key));

        this.touchPortalPluginExample.storeProperties();
        this.touchPortalPluginExample.reloadProperties();

        assertEquals(value, this.touchPortalPluginExample.getProperty(key));
    }

    @Test(expected = FileNotFoundException.class)
    public void testPropertiesFail() throws IOException {
        this.touchPortalPluginExample.loadProperties("doesNot.exists");
    }
}
