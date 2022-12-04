package com.christophecvb.touchportal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ActionTranslations {
    ActionTranslation[] value();
}
