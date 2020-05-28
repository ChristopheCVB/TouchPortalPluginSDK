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

    @Action(name = "Dummy Action with Data")
    private void dummyWithData(@Data String msg) {
        System.out.println("dummyWithData");
    }

    @Action()
    private void dummyWithoutData(String msg) {
        System.out.println("dummyWithoutData");
    }
}
