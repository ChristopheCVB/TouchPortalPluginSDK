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
 * Plugin Annotation
 *
 * Target is a Class
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Plugin {
    /**
     * Plugin name
     *
     * Default is Class SimpleName
     *
     * @return String name
     */
    String name() default "";

    /**
     * Plugin version
     *
     * @return String version
     */
    int version();

    /**
     * Plugin colorDark
     *
     * Format is HTML like `#RRGGBB`
     *
     * @return String colorDark
     */
    String colorDark();

    /**
     * Plugin colorLight
     *
     * Format is HTML like `#RRGGBB`
     *
     * @return String colorLight
     */
    String colorLight();
}
