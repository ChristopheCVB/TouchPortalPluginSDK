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
    public static final String TP_TYPE_FILE = "file";
    public static final String TP_TYPE_DIRECTORY = "folder";
    public static final String TP_TYPE_COLOR = "color";

    protected static final String ID = "id";
    protected static final String NAME = "name";
    protected static final String TYPE = "type";
    protected static final String TYPE_CHOICE = "choice";
    protected static final String DESCRIPTION = "description";
    protected static final String VALUE = "value";
    protected static final String DEFAULT = "default";

    /**
     * Retrieve the Touch Portal type according to the Java's element type
     *
     * @param reference String
     * @param element   Element
     * @return String tpType
     * @throws GenericHelper.TPTypeException If the Type is not supported
     */
    public static String getTouchPortalType(String reference, Element element) throws GenericHelper.TPTypeException {
        return GenericHelper.getTouchPortalType(element.asType().toString(), reference);
    }

    /**
     * Retrieve the Touch Portal type according to the Java's type
     *
     * @param rawType   String
     * @param reference String
     * @return String tpType
     * @throws GenericHelper.TPTypeException If the Type is not supported
     */
    public static String getTouchPortalType(String rawType, String reference) throws GenericHelper.TPTypeException {
        String tpType;
        switch (rawType) {
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
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

            case "java.lang.String":
                tpType = GenericHelper.TP_TYPE_TEXT;
                break;

            case "java.lang.String[]":
                tpType = GenericHelper.TP_TYPE_CHOICE;
                break;

            case "java.io.File":
                tpType = GenericHelper.TP_TYPE_FILE;
                break;

            default:
                throw new TPTypeException.Builder(reference, rawType).build();
        }
        return tpType;
    }

    /**
     * Touch Portal Type Exception
     */
    public static class TPTypeException extends Exception {

        /**
         * Constructor
         *
         * @param message String
         */
        private TPTypeException(String message) {
            super(message);
        }

        /**
         * Enum representing the non All TPTypes supported
         */
        public enum ForAnnotation {
            STATE,
            EVENT
        }

        /**
         * Builder
         */
        public static class Builder {
            /**
             * Exception message
             */
            private String message;

            /**
             * Constructor
             *
             * @param reference     String - Name of the element being processed
             * @param forAnnotation {@link ForAnnotation}
             * @param tpType        String - Desired TPType
             */
            public Builder(String reference, ForAnnotation forAnnotation, String tpType) {
                this.message = reference + ": The type '" + tpType + "' is not supported";
                if (forAnnotation != null) {
                    switch (forAnnotation) {
                        case STATE:
                            this.message += " for states, only '" + StateHelper.TYPE_CHOICE + "' and '" + StateHelper.TYPE_TEXT + "' are.";
                            break;

                        case EVENT:
                            this.message += " for events, only '" + EventHelper.VALUE_TYPE_CHOICE + "' is.";
                            break;
                    }
                }
            }

            /**
             * Constructor
             *
             * @param reference String - The element being processed
             * @param rawType   String - The raw Java Type
             */
            public Builder(String reference, String rawType) {
                this(reference, null, rawType);
            }

            /**
             * Build the {@link TPTypeException}
             *
             * @return TPTypeException tpTypeException
             */
            public TPTypeException build() {
                return new TPTypeException(this.message);
            }
        }
    }
}
