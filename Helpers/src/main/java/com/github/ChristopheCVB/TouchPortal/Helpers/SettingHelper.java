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

package com.github.ChristopheCVB.TouchPortal.Helpers;

import com.github.ChristopheCVB.TouchPortal.Annotations.Setting;

import javax.lang.model.element.Element;

/**
 * Touch Portal Plugin State Helper
 */
public class SettingHelper {
    public static final String NAME = GenericHelper.NAME;
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_NUMBER = GenericHelper.TP_TYPE_NUMBER;
    public static final String TYPE_TEXT = GenericHelper.TP_TYPE_TEXT;
    public static final String DEFAULT = GenericHelper.DEFAULT;
    public static final String MAX_LENGTH = "maxLength";
    public static final String IS_PASSWORD = "isPassword";
    public static final String MIN_VALUE = "minValue";
    public static final String MAX_VALUE = "maxValue";

    /**
     * Get the generated State Name
     *
     * @param settingElement Element
     * @param setting        {@link Setting}
     * @return String settingName
     */
    public static String getSettingName(Element settingElement, Setting setting) {
        return setting.name().isEmpty() ? settingElement.getSimpleName().toString() : setting.name();
    }
}
