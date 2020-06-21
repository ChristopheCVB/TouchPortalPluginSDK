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
 * <p>
 * Target is a Method
 * </p>
 * <p>
 * <b>If your method only has @Data annotated parameters, it will be called automatically by the SDK.</b>
 * <i>If it is not the case, the SDK will call the TouchPortalPluginListener.onReceive(JsonObject jsonMessage) instead.</i>
 * </p>
 *
 * @see <a href="https://www.touch-portal.com/sdk/index.php?section=dynamic-actions">TP Documentation: Dynamic Actions</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
    /**
     * Action id
     * <p>
     * Default is Method Name
     * </p>
     *
     * @return String id
     */
    String id() default "";

    /**
     * Action name
     * <p>
     * Default is Method Name
     * </p>
     *
     * @return String name
     */
    String name() default "";

    /**
     * Action prefix
     * <p>
     * Default is "Plugin"
     * </p>
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
     * <p>
     * Default is "communicate"
     * </p>
     *
     * @return String type
     */
    String type() default "communicate";

    /**
     * Action format
     * <p>
     * Default is ""
     * </p>
     *
     * @return String format
     */
    String format() default "";

    /**
     * Action categoryId
     *
     * @return String categoryId
     */
    String categoryId();
}
