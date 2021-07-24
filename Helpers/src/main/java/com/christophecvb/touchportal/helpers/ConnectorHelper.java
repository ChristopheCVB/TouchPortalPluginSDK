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

import com.christophecvb.touchportal.annotations.Action;
import com.christophecvb.touchportal.annotations.Category;
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

    protected static final String KEY_CONNECTOR = "connector";

    /**
     * Get the generated Connector ID
     *
     * @param pluginElement     Element
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @param connector         {@link Connector}
     * @return String connectorId
     */
    public static String getConnectorId(Element pluginElement, Element categoryElement, Category category, Element connectorElement, Connector connector) {
        return ConnectorHelper._getConnectorId(CategoryHelper.getCategoryId(pluginElement, categoryElement, category), connector.id().isEmpty() ? connectorElement.getSimpleName().toString() : connector.id());
    }

    /**
     * Get the generated Connector Name
     *
     * @param actionElement Element
     * @param connector     {@link Action}
     * @return String connectorName
     */
    public static String getConnectorName(Element actionElement, Connector connector) {
        return connector.name().isEmpty() ? actionElement.getSimpleName().toString() : connector.name();
    }

    /**
     * Get the generated Connector ID
     *
     * @param pluginClass      Class
     * @param actionMethodName String
     * @return String connectorId
     */
    public static String getConnectorId(Class<?> pluginClass, String actionMethodName) {
        String connectorId = "";

        for (Method method : pluginClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Action.class) && method.getName().equals(actionMethodName)) {
                Action action = method.getDeclaredAnnotation(Action.class);
                connectorId = ConnectorHelper._getConnectorId(CategoryHelper.getCategoryId(pluginClass, action.categoryId()), (!action.id().isEmpty() ? action.id() : actionMethodName));
            }
        }

        return connectorId;
    }

    /**
     * Get the generated Connector ID
     *
     * @param pluginClass       Class
     * @param connectorMethod   Method
     * @return String connectorId
     */
    public static String getConnectorId(Class<?> pluginClass, Method connectorMethod) {
        String connectorId = "";

        if (connectorMethod.isAnnotationPresent(Connector.class)) {
            Connector connector = connectorMethod.getDeclaredAnnotation(Connector.class);
            connectorId = ConnectorHelper._getConnectorId(CategoryHelper.getCategoryId(pluginClass, connector.categoryId()), (!connector.id().isEmpty() ? connector.id() : connectorMethod.getName()));
        }

        return connectorId;
    }

    /**
     * Internal - Get the formatted Connector ID
     *
     * @param categoryId  String
     * @param rawActionId String
     * @return String actionId
     */
    private static String _getConnectorId(String categoryId, String rawActionId) {
        return categoryId + "." + ConnectorHelper.KEY_CONNECTOR + "." + rawActionId;
    }
}
