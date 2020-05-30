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

package com.github.ChristopheCVB.TouchPortal.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Action Annotation
 *
 * Target is a Method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
    /**
     * Action id
     *
     * Default is Method Name
     *
     * @return String id
     */
    String id() default "";

    /**
     * Action name
     *
     * Default is Method Name
     *
     * @return String name
     */
    String name() default "";

    /**
     * Action prefix
     *
     * Default is "Plugin"
     *
     * @return String prefix
     */
    String prefix() default "Plugin";

    /**
     * Action description
     *
     * @return String description
     */
    String description();

    /**
     * Action type
     *
     * Default is "communicate"
     *
     * @return String type
     */
    String type() default "communicate";

    /**
     * Action format
     *
     * This is only added if tryInline is true
     *
     * Default is ""
     *
     * @return String format
     */
    String format() default "";
}
