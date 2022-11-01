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

import java.lang.annotation.*;

/**
 * ActionTranslation Annotation
 * <p>
 * Target is a Method
 * </p>
 */
@Repeatable(ActionTranslations.class)
public @interface ActionTranslation {
    /**
     * ActionTranslation Language
     *
     * @return Language language
     */
    Language language();

    /**
     * ActionTranslation name
     * <p>
     * Default is ""
     * </p>
     *
     * @return String name
     */
    String name() default "";

    /**
     * ActionTranslation prefix
     * <p>
     * Default is ""
     * </p>
     *
     * @return String prefix
     */
    String prefix() default "";

    /**
     * ActionTranslation description
     * <p>
     * Default is ""
     * </p>
     *
     * @return String description
     */
    String description() default "";

    /**
     * ActionTranslation format
     * <p>
     * Default is ""
     * </p>
     *
     * @return String format
     */
    String format() default "";
}
