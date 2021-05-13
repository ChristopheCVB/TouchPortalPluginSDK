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
 * Setting Annotation
 * <p>
 * Target is a Field
 * </p>
 *
 * @see <a href="https://www.touch-portal.com/sdk/index.php?section=settings">TP Documentation: Settings</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting {
    /**
     * Setting name
     * <p>
     * Default is Field name
     * </p>
     *
     * @return String Setting
     */
    String name() default "";

    /**
     * Setting defaultValue
     *
     * @return String defaultValue
     */
    String defaultValue();

    /**
     * Setting maxLength
     * <p>
     * Used if the parameter type is text
     * </p>
     *
     * @return double maxLength
     */
    double maxLength() default 0;

    /**
     * Setting isPassword
     * <p>
     * Used if the parameter type is text
     * </p>
     *
     * @return boolean isPassword
     */
    boolean isPassword() default false;

    /**
     * Setting minValue
     * <p>
     * Used if the parameter type is number
     * </p>
     *
     * @return double minValue
     */
    double minValue() default Double.NEGATIVE_INFINITY;

    /**
     * Setting maxValue
     * <p>
     * Used if the parameter type is number
     * </p>
     *
     * @return double maxValue
     */
    double maxValue() default Double.POSITIVE_INFINITY;

    /**
     * Setting readOnly
     *
     * @return boolean isReadOnly
     */
    boolean isReadOnly() default false;
}
