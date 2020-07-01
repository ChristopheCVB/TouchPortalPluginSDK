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
import com.google.gson.JsonObject;

import java.io.File;

@SuppressWarnings("unused")
@Plugin(version = 4000, colorDark = "#203060", colorLight = "#4070F0")
public class TouchPortalPluginTest extends TouchPortalPlugin {
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
    public TouchPortalPluginTest(String[] args) {
        super(args[1], true);
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

    /**
     * Action example that contains a dynamic data text and number
     *
     * @param text String
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
    @Action(description = "Long Description of Dummy Action with Data File", categoryId = "BaseCategory")
    private void dummyWithDataFileAndDirectory(@Data(extensions = {"*.mp3", "*.7z"}) File file, @Data(isDirectory = true) File directory) {
        System.out.println("Action dummyWithDataFileAndDirectory received: file [" + file.getAbsolutePath() + "]");
    }

    /**
     * Action example that contains a dynamic data color
     *
     * @param color Color
     */
    @Action(description = "Long Description of Dummy Action with Data File", categoryId = "BaseCategory")
    private void dummyWithDataColor(@Data(isColor = true, defaultValue = "#000000FF") String color) {
        System.out.println("Action dummyWithDataColor received: color [" + color + "]");
    }

    private enum Categories {
        /**
         * Category definition example
         */
        @Category()
        BaseCategory
    }
}
