package com.github.ChristopheCVB.TouchPortal.sample;

import com.github.ChristopheCVB.TouchPortal.Annotations.Data;
import com.github.ChristopheCVB.TouchPortal.Annotations.Plugin;
import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin;

@Plugin(version = 1000, colorDark = "#203060", colorLight = "#4070F0")
public class TouchPortalPluginExample extends TouchPortalPlugin {
    @com.github.ChristopheCVB.TouchPortal.Annotations.Action(name = "Dummy Action")
    private void dummyWithData(@Data String msg) {
        System.out.println("dummyWithData");
    }

    @com.github.ChristopheCVB.TouchPortal.Annotations.Action()
    private void dummyWithoutData(String msg) {
        System.out.println("dummyWithoutData");
    }
}
