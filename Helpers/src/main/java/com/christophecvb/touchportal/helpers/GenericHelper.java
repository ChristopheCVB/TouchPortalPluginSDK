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
    protected static final String DESCRIPTION = "description";
    protected static final String VALUE = "value";
    protected static final String DEFAULT = "default";
    protected static final String VALUE_CHOICES = "valueChoices";

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
                throw new TPTypeException.Builder(reference).typeUnsupported(rawType).build();
        }
        return tpType;
    }

    /**
     * Retrieve the Touch Portal type Number allow decimals
     *
     * @param rawType String
     * @return boolean allowDecimals
     */
    public static boolean getTouchPortalTypeNumberAllowDecimals(String rawType) {
        boolean allowDecimals = true;
        switch (rawType) {
            case "short":
            case "int":
            case "long":
            case "java.lang.Short":
            case "java.lang.Integer":
            case "java.lang.Long":
                allowDecimals = false;
                break;
        }
        return allowDecimals;
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
            EVENT,
            SETTING,
            CONNECTOR_VALUE
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
             * @param reference String - The full element name being processed
             */
            public Builder(String reference) {
                this.message = reference;
            }

            /**
             * Specify the error being type related
             *
             * @param type String
             * @return Builder builder
             */
            public Builder typeUnsupported(String type) {
                this.message += ": The type '" + type + "' is not supported";

                return this;
            }

            /**
             * Specify the error being annotation related
             *
             * @param forAnnotation {@link ForAnnotation}
             * @return Builder builder
             */
            public Builder forAnnotation(ForAnnotation forAnnotation) {
                if (forAnnotation != null) {
                    switch (forAnnotation) {
                        case STATE:
                            this.message += " for States, only '" + StateHelper.TYPE_CHOICE + "' and '" + StateHelper.TYPE_TEXT + "' are.";
                            break;

                        case EVENT:
                            this.message += " for Events, only '" + EventHelper.VALUE_TYPE_CHOICE + "' is.";
                            break;

                        case SETTING:
                            this.message += " for Settings, only '" + SettingHelper.TYPE_NUMBER + "' and '" + SettingHelper.TYPE_TEXT + "' are.";
                            break;

                        case CONNECTOR_VALUE:
                            this.message += " for ConnectorValues, only '" + GenericHelper.TP_TYPE_NUMBER + "' is.";
                            break;
                    }
                }

                return this;
            }

            /**
             * Specify the error being default value range related
             *
             * @return Builder builder
             */
            public Builder defaultNotInRange() {
                this.message += ": The specified default value is not in range of min or max";

                return this;
            }

            /**
             * Specify the error being default value related
             *
             * @param defaultValue String
             * @return Builder builder
             */
            public Builder defaultInvalid(String defaultValue) {
                this.message += ": The specified default value is a not valid number: " + defaultValue;

                return this;
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
