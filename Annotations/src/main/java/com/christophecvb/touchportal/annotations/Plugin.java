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
 * Plugin Annotation
 * <p>
 * Target is a Class
 * </p>
 *
 * @see <a href="https://www.touch-portal.com/sdk/index.php?section=structure">TP Documentation: Plugin Structure</a>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Plugin {
    /**
     * Plugin name
     * <p>
     * Default is Class SimpleName
     * </p>
     *
     * @return String name
     */
    String name() default "";

    /**
     * Plugin version
     *
     * @return long version
     */
    long version();

    /**
     * Plugin colorDark
     * <p>
     * Format is HTML like `#RRGGBB`
     * </p>
     *
     * @return String colorDark
     */
    String colorDark();

    /**
     * Plugin colorLight
     * <p>
     * Format is HTML like `#RRGGBB`
     * </p>
     *
     * @return String colorLight
     */
    String colorLight();

    /**
     * Plugin Parent Category
     * <p>
     * Value from enum {@link ParentCategory}
     * </p>
     *
     * @return ParentCategory parentCategory
     */
    ParentCategory parentCategory() default ParentCategory.MISC;
}
