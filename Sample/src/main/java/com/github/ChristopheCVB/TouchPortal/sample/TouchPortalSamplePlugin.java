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
import com.github.ChristopheCVB.TouchPortal.model.TPBroadcastMessage;
import com.github.ChristopheCVB.TouchPortal.model.TPInfoMessage;
import com.github.ChristopheCVB.TouchPortal.model.TPListChangeMessage;
import com.github.ChristopheCVB.TouchPortal.model.TPSettingsMessage;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "Touch Portal Plugin Example")
public class TouchPortalSamplePlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    /**
     * Logger
     */
    private final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());

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
    @State(defaultValue = "1", categoryId = "BaseCategory")
    @Event(valueChoices = {"1", "2"}, format = "When customStateWithEvent becomes $val")
    private String customStateWithEvent;

    /**
     * State of type choice definition example
     */
    @State(valueChoices = {"1", "2"}, defaultValue = "1", categoryId = "SecondCategory")
    private String[] customStateChoice;

    /**
     * State of type text definition example
     */
    @State(defaultValue = "Default Value", categoryId = "SecondCategory")
    private String customStateText;

    /**
     * Setting of type text definition example
     */
    @Setting(name = "IP", defaultValue = "localhost", maxLength = 15)
    private String ipSetting;

    /**
     * Setting of type number definition example
     */
    @Setting(name = "Update Delay", defaultValue = "10", minValue = 10, maxValue = 30)
    private int updateDelaySetting;

    /**
     * Constructor calling super
     */
    public TouchPortalSamplePlugin() {
        super(true);
    }

    public static void main(String[] args) {
        if (args != null && args.length == 1) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                TouchPortalSamplePlugin touchPortalSamplePlugin = new TouchPortalSamplePlugin();
                // Load a properties File
                touchPortalSamplePlugin.loadProperties("plugin.config");
                // Get a property
                TouchPortalSamplePlugin.LOGGER.log(Level.INFO, touchPortalSamplePlugin.getProperty("samplekey"));
                // Set a property
                touchPortalSamplePlugin.setProperty("samplekey", "Value set from Plugin");
                // Store the properties
                touchPortalSamplePlugin.storeProperties();
                // Initiate the connection with the Touch Portal Plugin System
                boolean connectedPairedAndListening = touchPortalSamplePlugin.connectThenPairAndListen(touchPortalSamplePlugin);

                if (connectedPairedAndListening) {
                    // Update a State with the ID from the Generated Constants Class
                    boolean stateUpdated = touchPortalSamplePlugin.sendStateUpdate(TouchPortalSamplePluginConstants.BaseCategory.States.CustomStateWithEvent.ID, "2");

                    // Create a new State
                    touchPortalSamplePlugin.sendCreateState("BaseCategory", "createdState1", "Created State 01", System.currentTimeMillis() + "1");
                    touchPortalSamplePlugin.sendCreateState("BaseCategory", "createdState1", "Created State 01", System.currentTimeMillis() + "2");
                    touchPortalSamplePlugin.sendCreateState("BaseCategory", "createdState2", "Created State 02", System.currentTimeMillis() + "");
                    touchPortalSamplePlugin.sendRemoveState("BaseCategory", "createdState2");
                    touchPortalSamplePlugin.sendRemoveState("BaseCategory", "createdState1");
                    touchPortalSamplePlugin.sendRemoveState("BaseCategory", "customState");
                }
            }
        }
    }

    /**
     * Action example with no parameter
     */
    @Action(description = "Long Description of Action Simple", format = "Do a simple action", categoryId = "BaseCategory")
    private void actionSimple() {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionSimple received");
    }

    /**
     * Action example with a Data Text parameter
     *
     * @param text String
     */
    @Action(description = "Long Description of Dummy Action with Data Text", format = "Set text to {$text$}", categoryId = "BaseCategory")
    private void actionWithText(@Data String text) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithText received: " + text);
    }

    /**
     * Action example without Data but 1 parameter
     * <p>This action will not be called automatically by the SDK</p>
     *
     * @param jsonAction JSONObject
     * @see TouchPortalSamplePlugin .onReceive
     */
    @Action(description = "Long Description of Action without Data", categoryId = "BaseCategory")
    private void actionWithoutData(JsonObject jsonAction) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithoutData received [" + jsonAction + "]");
    }

    /**
     * Action example with a Data Choice
     *
     * @param doActions String[]
     */
    @Action(description = "Long Description of Action with Choice", format = "Do action {$doActions$}", categoryId = "BaseCategory")
    private void actionWithChoice(@Data(valueChoices = {"Enable", "Disable", "Toggle"}, defaultValue = "Toggle") String[] doActions) {
        // The user selected value is passed at index 0
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithChoice received: " + doActions[0]);
    }

    /**
     * Action example with a Data Switch
     *
     * @param isOn boolean
     */
    @Action(description = "Long Description of Action with Switch", format = "Switch to {$isOn$}", categoryId = "BaseCategory")
    private void actionWithSwitch(@Data(defaultValue = "false") boolean isOn) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithSwitch received: " + isOn);
    }

    /**
     * Action example with a Data File
     *
     * @param file File
     */
    @Action(description = "Long Description of Action with File", format = "Do an action with {$file$}", categoryId = "BaseCategory")
    private void actionWithFile(@Data File file) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithFile received: " + file.getAbsolutePath());
    }

    /**
     * Action example with a Data File that is a directory
     *
     * @param directory File
     */
    @Action(description = "Long Description of Action with Directory", format = "Do an action with {$directory$}", categoryId = "BaseCategory")
    private void actionWithDirectory(@Data(isDirectory = true) File directory) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithDirectory received: " + directory.getAbsolutePath());
    }

    /**
     * Action example with a Data Integer and Double
     *
     * @param integerValue Integer
     * @param doubleValue  Double
     */
    @Action(description = "Long Description of Action with Numbers", format = "Do an action with {$integerValue$} and {$doubleValue$}", categoryId = "BaseCategory")
    private void actionWithNumbers(@Data(minValue = -1, maxValue = 42, defaultValue = "0") Integer integerValue, @Data Double doubleValue) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithNumber received: " + integerValue + " and " + doubleValue);
    }

    /**
     * Action example with a Data Color
     *
     * @param color String
     */
    @Action(description = "Long Description of Action with Color", format = "Do an action with {$color$}", categoryId = "BaseCategory")
    private void actionWithColor(@Data(defaultValue = "#00000000", isColor = true) String color) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithColor received: " + color);
    }

    @Action(name = "Hold Me!", hasHoldFunctionality = true, categoryId = "BaseCategory")
    private void actionHoldable() {
        Boolean isHeld = this.isActionBeingHeld(TouchPortalSamplePluginConstants.BaseCategory.Actions.ActionHoldable.ID);
        if (isHeld != null) {
            // Action is triggered by a Hold
            while (isHeld != null && isHeld) {
                TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "actionHoldable has been triggered by a HOLD");
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ignored) {}
                isHeld = this.isActionBeingHeld(TouchPortalSamplePluginConstants.BaseCategory.Actions.ActionHoldable.ID);
            }
        }
        else {
            // Action is triggered by a Press
            TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "actionHoldable has been triggered by a Press");
        }
    }

    @Override
    public void onDisconnected(Exception exception) {
        // Socket connection is lost or plugin has received close message
        System.exit(0);
    }

    @Override
    public void onReceived(JsonObject jsonMessage) {
        // Check if ReceiveMessage is an Action
        if (ReceivedMessageHelper.isTypeAction(jsonMessage)) {
            // Get the Action ID
            String receivedActionId = ReceivedMessageHelper.getActionId(jsonMessage);
            if (receivedActionId != null) {
                //noinspection SwitchStatementWithTooFewBranches
                switch (receivedActionId) {
                    case TouchPortalSamplePluginConstants.BaseCategory.Actions.ActionWithoutData.ID:
                        // Manually call the action method because the parameter jsonMessage is not annotated with @Data
                        this.actionWithoutData(jsonMessage);
                        break;
                }
            }
        }
        // dummyWithDataText, dummyWithDataChoice, dummySwitchAction and dummyAction are automatically called by the SDK
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
}
