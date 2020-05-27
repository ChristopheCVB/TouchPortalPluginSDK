package com.github.ChristopheCVB.TouchPortal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class TouchPortalPluginAnnotations {
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    public @interface Action {
        String name();
        String id();
    }
}
