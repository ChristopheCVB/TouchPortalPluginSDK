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

import com.github.ChristopheCVB.TouchPortal.Annotations.*;
import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin;
import com.github.ChristopheCVB.TouchPortal.model.TPActionMessage;
import com.google.gson.JsonObject;

import java.io.File;

@SuppressWarnings("unused")
@Plugin(version = 6000, colorDark = "#203060", colorLight = "#4070F0")
public class TouchPortalPluginTest extends TouchPortalPlugin {
    /**
     * State and Event definition example
     */
    @State(defaultValue = "1", categoryId = "BaseCategory")
    @Event(valueChoices = {"1", "2"}, format = "When customState becomes $val")
    private String customState;

    /**
     * State definition example
     */
    @State(valueChoices = {"1", "2"}, defaultValue = "1", categoryId = "BaseCategory")
    private String[] customState2;

    /**
     * Setting definition example for text
     */
    @Setting(name = "IP Address", defaultValue = "localhost", maxLength = 15)
    String ipSetting = "localhost";

    /**
     * Setting definition example for number
     */
    @Setting(defaultValue = "0", minValue = 0, maxValue = 10)
    float otherSetting = 0;

    /**
     * Setting definition example for read only text
     */
    @Setting(defaultValue = "This is not editable by the user", isReadOnly = true)
    String readOnlySetting = "This is not editable by the user";

    /**
     * Constructor calling super
     */
    public TouchPortalPluginTest() {
        super(true);
    }

    /**
     * Simple Action example
     *
     * @param jsonAction JSONObject
     */
    @Action(description = "Long Description of Dummy Action with JsonObject", categoryId = "BaseCategory")
    private void dummyWithJsonObject(JsonObject jsonAction) {
        System.out.println("Action dummyWithoutData received [" + jsonAction + "]");
    }

    /**
     * Simple Action example
     *
     * @param tpActionMessage TPActionMessage
     */
    @Action(description = "Long Description of Dummy Action with TPActionMessage", categoryId = "BaseCategory")
    private void dummyWithTPActionMessage(TPActionMessage tpActionMessage) {
        System.out.println("Action dummyWithoutData received [" + tpActionMessage + "]");
    }

    /**
     * Action example that contains a dynamic data text and number
     *
     * @param text   String
     * @param number Integer
     */
    @Action(description = "Long Description of Dummy Action with Data Text and Number", categoryId = "BaseCategory")
    private void dummyWithDataTextAndNumber(@Data String text, @Data Integer number) {
        System.out.println("Action dummyWithDataTextAndNumber received: text [" + text + "] number [" + number + "]");
    }

    /**
     * Action example that contains a dynamic data file and directory
     *
     * @param file      File
     * @param directory File
     */
    @Action(description = "Long Description of Dummy Action with Data File and Directory", categoryId = "BaseCategory")
    private void dummyWithDataFileAndDirectory(@Data(extensions = {"*.mp3", "*.7z"}) File file, @Data(isDirectory = true) File directory) {
        System.out.println("Action dummyWithDataFileAndDirectory received: file [" + file.getAbsolutePath() + "]");
    }

    /**
     * Action example that contains a dynamic data color
     *
     * @param color String
     */
    @Action(description = "Long Description of Dummy Action with Data Color", categoryId = "BaseCategory")
    private void dummyWithDataColor(@Data(isColor = true, defaultValue = "#000000FF") String color) {
        System.out.println("Action dummyWithDataColor received: color [" + color + "]");
    }

    /**
     * Action example that contains a parameter
     *
     * @param value String
     */
    @Action(description = "Long Description of Dummy Action with Param", categoryId = "BaseCategory")
    private void dummyWithParam(String value) {
        // This is not automatically called
    }

    @Action(name = "Hold Me!", hasHoldFunctionality = true, categoryId = "BaseCategory")
    private void actionHoldable() {
        Boolean isHeld = this.isActionBeingHeld(TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID);
        if (isHeld != null) {
            // Action is triggered by a Hold
            while (isHeld != null && isHeld) {
                System.out.println("actionHoldable has been triggered by a HOLD");
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ignored) {}
                isHeld = this.isActionBeingHeld(TouchPortalPluginTestConstants.BaseCategory.Actions.ActionHoldable.ID);
            }
        }
        else {
            // Action is triggered by a Press
            System.out.println("actionHoldable has been triggered by a Press");
        }
    }

    private enum Categories {
        /**
         * Category definition example
         */
        @Category()
        BaseCategory
    }
}
