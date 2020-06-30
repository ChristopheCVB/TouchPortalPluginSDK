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
 * Data Annotation
 * <p>
 * Target is a Parameter
 * </p>
 *
 * @see <a href="https://www.touch-portal.com/sdk/index.php?section=action-data">TP Documentation: Action Data</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Data {
    /**
     * Data id
     * <p>
     * Default is Parameter Name
     * </p>
     *
     * @return String id
     */
    String id() default "";

    /**
     * Data label
     * <p>
     * Default is Parameter Name
     * </p>
     *
     * @return String label
     */
    String label() default "";

    /**
     * Data defaultValue
     * <p>
     * 0 for number
     * false for switch
     * "" (empty String) otherwise
     * </p>
     *
     * @return String defaultValue
     */
    String defaultValue() default "";

    /**
     * Data valueChoices
     * <p>
     * Default is empty Array
     * </p>
     *
     * @return String[] valueChoices
     */
    String[] valueChoices() default {};

    /**
     * Data extensions
     * <p>
     * Used if the parameter type is File
     * </p>
     * <p>
     * Supported extensions (i.e. `{"*.txt"}`)
     * </p>
     * <p>
     * Default is an empty Array, hence supporting all extensions
     * </p>
     *
     * @return String[] extensions
     */
    String[] extensions() default {};

    /**
     * Data isDirectory
     * <p>
     * Used if the parameter type is File
     * </p>
     *
     * @return boolean isDirectory
     */
    boolean isDirectory() default false;
}
