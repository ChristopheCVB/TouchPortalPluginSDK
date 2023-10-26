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

package com.christophecvb.touchportal.helpers;

import com.christophecvb.touchportal.annotations.Setting;

import javax.lang.model.element.Element;
import java.lang.reflect.Field;

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
    public static final String IS_READ_ONLY = "readOnly";
    public static final String TOOLTIP = "tooltip";

    public static class Tooltip {
        public static final String TITLE = "title";
        public static final String BODY = "body";
        public static final String DOC_URL = "docUrl";
    }

    /**
     * Get the generated Setting Name
     *
     * @param settingElement Element
     * @param setting        {@link Setting}
     * @return String settingName
     */
    public static String getSettingName(Element settingElement, Setting setting) {
        return SettingHelper._getSettingName(setting.name().isEmpty() ? settingElement.getSimpleName().toString() : setting.name());
    }

    /**
     * Get the generated Setting Name
     *
     * @param settingField Field
     * @param setting      {@link Setting}
     * @return String settingName
     */
    public static String getSettingName(Field settingField, Setting setting) {
        return SettingHelper._getSettingName(setting.name().isEmpty() ? settingField.getName() : setting.name());
    }

    /**
     * Internal - Get the generated Setting Name
     *
     * @param rawName String
     * @return String settingName
     */
    private static String _getSettingName(String rawName) {
        return rawName;
    }
}
