package com.github.ChristopheCVB.TouchPortal.sample;

import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin;
import com.github.ChristopheCVB.TouchPortal.Annotations.TouchPortalPluginAnnotations;

public class TouchPortalPluginExample extends TouchPortalPlugin {
    @TouchPortalPluginAnnotations.Action(name = "dummyName", id = "dummyId")
    private void dummy(String msg) {
        System.out.println("dummy");
    }

    @TouchPortalPluginAnnotations.Action(name = "dummyName2", id = "dummyId2")
    private void dummy2(String msg) {
        System.out.println("dummy2");
    }
}
