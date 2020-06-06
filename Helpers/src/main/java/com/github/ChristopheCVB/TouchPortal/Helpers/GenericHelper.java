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

import javax.lang.model.element.Element;

/**
 * Touch Portal Plugin Generic Helper
 */
public class GenericHelper {
    public static final String TP_TYPE_TEXT = "text";
    public static final String TP_TYPE_NUMBER = "number";
    public static final String TP_TYPE_SWITCH = "switch";
    public static final String TP_TYPE_CHOICE = "choice";

    protected static final String ID = "id";
    protected static final String NAME = "name";
    protected static final String TYPE = "type";
    protected static final String TYPE_CHOICE = "choice";
    protected static final String DESCRIPTION = "description";
    protected static final String VALUE = "value";
    protected static final String DEFAULT = "default";

    /**
     * Retrieve the internal Touch Portal type according to the Java's element type
     *
     * @param element Element
     * @return String tpType
     */
    public static String getTouchPortalType(Element element) {
        String tpType;
        String elementType = element.asType().toString();
        switch (elementType) {
            case "byte":
            case "char":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "java.lang.Byte":
            case "java.lang.Char":
            case "java.lang.Short":
            case "java.lang.Integer":
            case "java.lang.Long":
            case "java.lang.Float":
            case "java.lang.Double":
                tpType = GenericHelper.TP_TYPE_NUMBER;
                break;

            case "boolean":
            case "java.lang.Boolean":
                tpType = GenericHelper.TP_TYPE_SWITCH;
                break;

            default:
                if (elementType.endsWith("[]")) {
                    tpType = GenericHelper.TP_TYPE_CHOICE;
                }
                else {
                    tpType = GenericHelper.TP_TYPE_TEXT;
                }
                break;
        }
        return tpType;
    }
}
