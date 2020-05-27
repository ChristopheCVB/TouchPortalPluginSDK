package com.github.ChristopheCVB.TouchPortal.sample;

import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin;
import com.github.ChristopheCVB.TouchPortal.annotation.TouchPortalPluginAnnotations;

public class TouchPortalPluginExample extends TouchPortalPlugin {
    @TouchPortalPluginAnnotations.Action(name = "dummyName", id = "dummyId")
    private void dummy(String msg) {
        System.out.println("dummy");
    }
}
