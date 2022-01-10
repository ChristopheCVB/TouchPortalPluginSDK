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

import com.christophecvb.touchportal.annotations.Connector;

import javax.lang.model.element.Element;
import java.lang.reflect.Method;

/**
 * Touch Portal Plugin Action Helper
 */
public class ConnectorHelper {
    public static final String ID = GenericHelper.ID;
    public static final String NAME = GenericHelper.NAME;
    public static final String TYPE = GenericHelper.TYPE;
    public static final String DATA = "data";
    public static final String FORMAT = "format";
    public static final String UPDATE_PREFIX = "pc";
    public static final String UPDATE_ID_SEPARATOR = "_";
    public static final String UPDATE_DATA_SEPARATOR = "|";

    protected static final String KEY_CONNECTOR = "connector";

    /**
     * Get the generated Connector ID
     *
     * @param connectorElement  Element
     * @param connector         {@link Connector}
     * @return String connectorId
     */
    public static String getConnectorId(Element connectorElement, Connector connector) {
        return ConnectorHelper._getConnectorId(connector.id().isEmpty() ? connectorElement.getSimpleName().toString() : connector.id());
    }

    /**
     * Get the generated Connector Name
     *
     * @param connectorElement  Element
     * @param connector         {@link Connector}
     * @return String connectorName
     */
    public static String getConnectorName(Element connectorElement, Connector connector) {
        return connector.name().isEmpty() ? connectorElement.getSimpleName().toString() : connector.name();
    }

    /**
     * Get the generated Connector ID
     *
     * @param pluginClass           Class
     * @param connectorMethodName   String
     * @return String connectorId
     */
    public static String getConnectorId(Class<?> pluginClass, String connectorMethodName) {
        String connectorId = "";

        for (Method method : pluginClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Connector.class) && method.getName().equals(connectorMethodName)) {
                Connector connector = method.getDeclaredAnnotation(Connector.class);
                connectorId = ConnectorHelper.getConnectorId(method, connector);
            }
        }

        return connectorId;
    }

    /**
     * Get the generated Connector ID
     *
     * @param connectorMethod  Method
     * @return String connectorId
     */
    public static String getConnectorId(Method connectorMethod) {
        String connectorId = "";

        if (connectorMethod.isAnnotationPresent(Connector.class)) {
            Connector connector = connectorMethod.getDeclaredAnnotation(Connector.class);
            connectorId = ConnectorHelper.getConnectorId(connectorMethod, connector);
        }

        return connectorId;
    }

    /**
     * Get the generated Connector ID
     *
     * @param connectorMethod   Method
     * @param connector         {@link Connector}
     * @return String connectorId
     */
    public static String getConnectorId(Method connectorMethod, Connector connector) {
        return ConnectorHelper._getConnectorId(!connector.id().isEmpty() ? connector.id() : connectorMethod.getName());
    }

    /**
     * Internal - Get the formatted Connector ID
     *
     * @param rawConnectorId    String
     * @return String connectorId
     */
    private static String _getConnectorId(String rawConnectorId) {
        return ConnectorHelper.KEY_CONNECTOR + "." + rawConnectorId;
    }

    /**
     * Compute a Linear Translate
     *
     * @param inputMin      float
     * @param inputMax      float
     * @param outputMin     float
     * @param outputMax     float
     * @param inputValue    float
     * @return float computedValue
     */
    public static Float linearTranslate(float inputMin, float inputMax, float outputMin, float outputMax, float inputValue) {
        float inputRange = inputMax - inputMin;
        float outputRange = outputMax - outputMin;
        float computedCross = outputRange / inputRange * (inputValue - inputMin);
        return computedCross + outputMin;
    }
}
