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

package com.christophecvb.touchportal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Connector Annotation
 * <p>
 * Target is a Method or a Class extending TPConnector
 * </p>
 * <ul>
 *     <li>
 *         If used on a Method
 *         <ul>
 *             <li>If the method only has @Data annotated parameters, it will be called automatically by the SDK.</li>
 *             <li>If it is not the case, the SDK will call the TouchPortalPluginListener.onReceive(JsonObject jsonMessage) instead.</li>
 *         </ul>
 *     </li>
 *     <li>
 *         If used on a Class, the SDK will
 *         <ul>
 *             <li>Create a new instance of the Class</li>
 *             <li>Set the instance's {@link Data} annotated fields</li>
 *             <li>Call the onInvoke method</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @see <a href="https://www.touch-portal.com/api/index.php?section=connectors">TP Documentation: Connectors</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Connector {
    /**
     * Connector id
     * <p>
     * Default is Method Name
     * </p>
     *
     * @return String id
     */
    String id() default "";

    /**
     * Connector name
     * <p>
     * Default is Method Name
     * </p>
     *
     * @return String name
     */
    String name() default "";

    /**
     * Connector format
     *
     * @return String format
     */
    String format();

    /**
     * Connector categoryId
     *
     * @return String categoryId
     */
    String categoryId();
}
