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

import java.io.File;

@SuppressWarnings("unused")
@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "Touch Portal Plugin Example")
public class TouchPortalPluginExample extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    private enum Categories {
        /**
         * Category definition example
         */
        @Category(name = "Touch Portal Plugin Example Base Category", imagePath = "images/icon-24.png")
        BaseCategory,
        @Category(name = "Touch Portal Plugin Example Second Category", imagePath = "images/icon-24.png")
        SecondCategory
    }

    /**
     * State and Event definition example
     */
    @State(valueChoices = {"1", "2"}, defaultValue = "1", categoryId = "BaseCategory")
    @Event(format = "When customState becomes $val")
    private String[] customState;

    /**
     * State and Event definition example
     */
    @State(valueChoices = {"1", "2"}, defaultValue = "1", categoryId = "SecondCategory")
    @Event(format = "When customState2 becomes $val")
    private String[] customState2;

    /**
     * Constructor calling super
     */
    public TouchPortalPluginExample() {
        super(true);
    }

    public static void main(String[] args) {
        if (args != null && args.length == 1) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                TouchPortalPluginExample touchPortalPluginExample = new TouchPortalPluginExample();
                // Load a properties File
                touchPortalPluginExample.loadProperties("plugin.config");
                // Get a property
                System.out.println(touchPortalPluginExample.getProperty("samplekey"));
                // Set a property
                touchPortalPluginExample.setProperty("samplekey", "Value set from Plugin");
                // Store the properties
                touchPortalPluginExample.storeProperties();
                // Initiate the connection with the Touch Portal Plugin System
                boolean connectedPairedAndListening = touchPortalPluginExample.connectThenPairAndListen(touchPortalPluginExample);

                if (connectedPairedAndListening) {
                    // Update a State with the ID from the Generated Constants Class
                    boolean stateUpdated = touchPortalPluginExample.sendStateUpdate(TouchPortalPluginExampleConstants.BaseCategory.States.CustomState.ID, "2");

                    // Create a new State
                    touchPortalPluginExample.createState("BaseCategory", "createdState1", "Created State 01", System.currentTimeMillis() + "1");
                    touchPortalPluginExample.createState("BaseCategory", "createdState1", "Created State 01", System.currentTimeMillis() + "2");
                    touchPortalPluginExample.createState("BaseCategory", "createdState2", "Created State 02", System.currentTimeMillis() + "");
                    touchPortalPluginExample.removeState("BaseCategory", "createdState2");
                    touchPortalPluginExample.removeState("BaseCategory", "createdState1");
                    touchPortalPluginExample.removeState("BaseCategory", "customState");
                }
            }
        }
    }

    /**
     * Action example with no parameter
     */
    @Action(description = "Long Description of Action Simple", format = "Do a simple action", categoryId = "BaseCategory")
    private void actionSimple() {
        System.out.println("Action actionSimple received");
    }

    /**
     * Action example with a Data Text parameter
     *
     * @param text String
     */
    @Action(description = "Long Description of Dummy Action with Data Text", format = "Set text to {$text$}", categoryId = "BaseCategory")
    private void actionWithText(@Data String text) {
        System.out.println("Action actionWithText received: " + text);
    }

    /**
     * Action example without Data but 1 parameter
     * <p>This action will not be called automatically by the SDK</p>
     *
     * @param jsonAction JSONObject
     * @see TouchPortalPluginExample .onReceive
     */
    @Action(description = "Long Description of Action without Data", categoryId = "BaseCategory")
    private void actionWithoutData(JsonObject jsonAction) {
        System.out.println("Action actionWithoutData received [" + jsonAction + "]");
    }

    /**
     * Action example with a Data Choice
     *
     * @param doActions String[]
     */
    @Action(description = "Long Description of Action with Choice", format = "Do action {$doActions$}", categoryId = "BaseCategory")
    private void actionWithChoice(@Data(valueChoices = {"Enable", "Disable", "Toggle"}, defaultValue = "Toggle") String[] doActions) {
        // The user selected value is passed at index 0
        System.out.println("Action actionWithChoice received: " + doActions[0]);
    }

    /**
     * Action example with a Data Switch
     *
     * @param isOn boolean
     */
    @Action(description = "Long Description of Action with Switch", format = "Switch to {$isOn$}", categoryId = "BaseCategory")
    private void actionWithSwitch(@Data(defaultValue = "false") boolean isOn) {
        System.out.println("Action actionWithSwitch received: " + isOn);
    }

    /**
     * Action example with a Data File
     *
     * @param file File
     */
    @Action(description = "Long Description of Action with File", format = "Do an action with {$file$}", categoryId = "BaseCategory")
    private void actionWithFile(@Data File file) {
        System.out.println("Action actionWithFile received: " + file.getAbsolutePath());
    }

    /**
     * Action example with a Data File that is a directory
     *
     * @param directory File
     */
    @Action(description = "Long Description of Action with Directory", format = "Do an action with {$directory$}", categoryId = "BaseCategory")
    private void actionWithDirectory(@Data(isDirectory = true) File directory) {
        System.out.println("Action actionWithDirectory received: " + directory.getAbsolutePath());
    }

    /**
     * Action example with a Data Integer and Double
     *
     * @param integerValue Integer
     * @param doubleValue  Double
     */
    @Action(description = "Long Description of Action with Numbers", format = "Do an action with {$integerValue$} and {$doubleValue$}", categoryId = "BaseCategory")
    private void actionWithNumbers(@Data Integer integerValue, @Data Double doubleValue) {
        System.out.println("Action actionWithNumber received: " + integerValue + " and " + doubleValue);
    }

    /**
     * Action example with a Data Color
     *
     * @param color String
     */
    @Action(description = "Long Description of Action with Color", format = "Do an action with {$color$}", categoryId = "BaseCategory")
    private void actionWithColor(@Data(defaultValue = "#00000000", isColor = true) String color) {
        System.out.println("Action actionWithColor received: " + color);
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
                    case TouchPortalPluginExampleConstants.BaseCategory.Actions.ActionWithoutData.ID:
                        // Manually call the action method because the parameter jsonMessage is not annotated with @Data
                        this.actionWithoutData(jsonMessage);
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
