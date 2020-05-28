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
import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin;

@Plugin(version = 1000, colorDark = "#203060", colorLight = "#4070F0")
public class TouchPortalPluginExample extends TouchPortalPlugin {
    @State
    private String[] customState;

    @Event(format = "When $val", valueChoices = {"1", "2"}, valueStateId = "com.github.ChristopheCVB.TouchPortal.sample.TouchPortalPluginExample.basecategory.state.customState")
    private String[] customEvent;

    public TouchPortalPluginExample() {}

    @Action(description = "Long Description of Dummy Action with Data")
    private void dummyWithData(@Data String text, Object object) {
        System.out.println("dummyWithData");
    }

    @Action(description = "Long Description of Dummy Action without Data")
    private void dummyWithoutData(String msg) {
        System.out.println("dummyWithoutData");
    }
}
