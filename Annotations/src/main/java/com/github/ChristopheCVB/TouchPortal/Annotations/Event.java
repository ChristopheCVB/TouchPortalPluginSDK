package com.github.ChristopheCVB.TouchPortal.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Event {
    String id() default "";
    String name() default "";
    String format();
    String valueStateId();
    String[] valueChoices();
}
