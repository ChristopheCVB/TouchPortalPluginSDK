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

@Plugin(version = 1000, colorDark = "#203060", colorLight = "#4070F0")
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
        super(args);
    }

    /**
     * Action example that contains a dynamic data text
     *
     * @param text String
     */
    @Action(description = "Long Description of Dummy Action with Data", categoryId = "BaseCategory")
    private void dummyWithData(@Data String text, @Data Integer number) {
        System.out.println("Action dummyWithData received: text [" + text + "] number [" + number + "]");
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

    private enum Categories {
        /**
         * Category definition example
         */
        @Category()
        BaseCategory
    }
}
