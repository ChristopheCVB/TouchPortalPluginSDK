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

package com.github.ChristopheCVB.TouchPortal.sample;

import com.github.ChristopheCVB.TouchPortal.Annotations.*;
import com.github.ChristopheCVB.TouchPortal.Helpers.PluginHelper;
import com.github.ChristopheCVB.TouchPortal.Helpers.ReceivedMessageHelper;
import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin;
import com.github.ChristopheCVB.TouchPortal.model.TPInfo;
import com.google.gson.JsonObject;

import java.io.IOException;

@SuppressWarnings("unused")
@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "Touch Portal Plugin Example")
public class TouchPortalPluginExample extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    private enum Categories {
        /**
         * Category definition example
         */
        @Category(name = "Touch Portal Plugin Example", imagePath = "images/icon-24.png")
        BaseCategory
    }

    /**
     * State and Event definition example
     */
    @State(valueChoices = {"1", "2"}, defaultValue = "1")
    @Event(format = "When customState becomes $val")
    private String[] customState;

    /**
     * Constructor calling super
     *
     * @param args String[]
     */
    public TouchPortalPluginExample(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        if (args != null && args.length == 2) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                TouchPortalPluginExample touchPortalPluginExample = new TouchPortalPluginExample(args);
                try {
                    // Load a properties File
                    touchPortalPluginExample.loadProperties("plugin.config");
                    // Get a property
                    System.out.println(touchPortalPluginExample.getProperty("samplekey"));
                    // Set a property
                    touchPortalPluginExample.setProperty("samplekey", "Value set from Plugin");
                    // Store the properties
                    touchPortalPluginExample.storeProperties();
                }
                catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                // Initiate the connection with the Touch Portal Plugin System
                boolean connectedPairedAndListening = touchPortalPluginExample.connectThenPairAndListen(touchPortalPluginExample);

                if (connectedPairedAndListening) {
                    // Update a State with the ID from the Generated Constants Class
                    boolean stateUpdated = touchPortalPluginExample.sendStateUpdate(TouchPortalPluginExampleConstants.BaseCategory.States.CustomState.ID, "2");
                }
            }
        }
    }

    /**
     * Action example that contains a dynamic data text
     *
     * @param text String
     */
    @Action(description = "Long Description of Dummy Action with Data Text", format = "Set text to {$text$}", categoryId = "BaseCategory")
    private void dummyWithDataText(@Data String text) {
        System.out.println("Action dummyWithDataText received: " + text);
    }

    /**
     * Action example that contains a dynamic data text
     *
     * @param action String[]
     */
    @Action(description = "Long Description of Dummy Action with Data Text", format = "Do action {$action$}", categoryId = "BaseCategory")
    private void dummyWithDataChoice(@Data(valueChoices = {"Enable", "Disable", "Toggle"}, defaultValue = "Toggle") String[] action) {
        // The selected value is passed at index 0
        System.out.println("Action dummyWithDataChoice received: " + action[0]);
    }

    /**
     * Simple Action example
     *
     * @param jsonAction JSONObject
     */
    @Action(description = "Long Description of Dummy Action without Data", categoryId = "BaseCategory")
    private void dummyWithoutData(JsonObject jsonAction) {
        System.out.println("Action dummyWithoutData received [" + jsonAction + "]");
    }

    @Action(description = "Long Description of Dummy Switch Action", format = "Switch to {$isOn$}", categoryId = "BaseCategory")
    private void dummySwitchAction(@Data(defaultValue = "false") boolean isOn) {
        System.out.println("Action dummySwitchAction received: " + isOn);
    }

    @Action(description = "Long Description of Dummy Action", format = "Do a dummy action", categoryId = "BaseCategory")
    private void dummyAction() {
        System.out.println("Action dummyAction received");
    }

    @Override
    public void onDisconnect(Exception exception) {
        // Socket connection is lost or plugin has received close message
    }

    @Override
    public void onReceive(JsonObject jsonMessage) {
        // Check if ReceiveMessage is an Action
        if (ReceivedMessageHelper.isTypeAction(jsonMessage)) {
            // Get the Action ID
            String receivedActionId = ReceivedMessageHelper.getActionId(jsonMessage);
            if (receivedActionId != null) {
                //noinspection SwitchStatementWithTooFewBranches
                switch (receivedActionId) {
                    case TouchPortalPluginExampleConstants.BaseCategory.Actions.DummyWithoutData.ID:
                        // Manually call the action method because the parameter jsonMessage is not annotated with @Data
                        this.dummyWithoutData(jsonMessage);
                        break;
                }
            }
        }
        // dummyWithDataText, dummyWithDataChoice, dummySwitchAction and dummyAction are automatically called by the SDK
    }

    @Override
    public void onInfo(TPInfo tpInfo) {
    }
}
