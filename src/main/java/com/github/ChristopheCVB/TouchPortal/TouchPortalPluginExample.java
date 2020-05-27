package com.github.ChristopheCVB.TouchPortal;

public class TouchPortalPluginExample extends TouchPortalPlugin {
    @TouchPortalPluginAnnotations.Action(name = "dummy")
    private void dummy() {
        System.out.println("dummy");
    }
}
