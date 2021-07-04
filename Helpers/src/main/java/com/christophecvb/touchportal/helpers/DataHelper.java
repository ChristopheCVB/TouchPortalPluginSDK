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
import com.christophecvb.touchportal.annotations.Data;

import javax.lang.model.element.Element;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Touch Portal Plugin Data Helper
 */
public class DataHelper {
    public static final String ID = GenericHelper.ID;
    public static final String TYPE = GenericHelper.TYPE;
    public static final String LABEL = "label";
    public static final String DEFAULT = GenericHelper.DEFAULT;
    public static final String VALUE_CHOICES = GenericHelper.VALUE_CHOICES;
    public static final String EXTENSIONS = "extensions";
    public static final String EXTENSION_FORMAT = "\\*\\.[a-zA-Z0-9]{1,}";
    public static final String COLOR_FORMAT = "#[a-fA-F0-9]{8}";
    public static final String ALLOW_DECIMALS = "allowDecimals";
    public static final String MIN_VALUE = "minValue";
    public static final String MAX_VALUE = "maxValue";

    protected static final String KEY_DATA = "data";

    /**
     * Get the generated Data Id
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @param dataElement     Element
     * @param data            {@link Data}
     * @return String dataId
     */
    public static String getActionDataId(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action, Element dataElement, Data data) {
        return DataHelper._getDataId(ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action), data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id());
    }

    /**
     * Get the generated Data Id
     *
     * @param pluginElement     Element
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @param connector         {@link Action}
     * @param dataElement       Element
     * @param data              {@link Data}
     * @return String dataId
     */
    public static String getConnectorDataId(Element pluginElement, Element categoryElement, Category category, Element connectorElement, Connector connector, Element dataElement, Data data) {
        return DataHelper._getDataId(ConnectorHelper.getConnectorId(pluginElement, categoryElement, category, connectorElement, connector), data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id());
    }

    /**
     * Get the generated Data Label
     *
     * @param dataElement Element
     * @param data        {@link Data}
     * @return String dataLabel
     */
    public static String getDataLabel(Element dataElement, Data data) {
        return data.label().isEmpty() ? dataElement.getSimpleName().toString() : data.label();
    }

    /**
     * Get the generated Data Id
     *
     * @param pluginClass         Class
     * @param actionMethodName    String
     * @param actionParameterName String
     * @return String actionDataId
     */
    public static String getActionDataId(Class<?> pluginClass, String actionMethodName, String actionParameterName) {
        String actionDataId = "";
        for (Method declaredMethod : pluginClass.getDeclaredMethods()) {
            if (declaredMethod.getName().equals(actionMethodName)) {
                for (Parameter parameter : declaredMethod.getParameters()) {
                    Data data = parameter.getAnnotation(Data.class);
                    if (data != null && parameter.getName().equals(actionParameterName)) {
                        actionDataId = DataHelper._getDataId(ActionHelper.getActionId(pluginClass, actionMethodName), data.id().isEmpty() ? actionParameterName : data.id());
                    }
                }
            }
        }
        return actionDataId;
    }

    /**
     * Get the generated Data Id
     *
     * @param pluginClass     Class
     * @param method          Method
     * @param methodParameter Parameter
     * @return String actionId
     */
    public static String getDataId(Class<?> pluginClass, Method method, Parameter methodParameter) {
        String dataId = "";

        if (methodParameter.isAnnotationPresent(Data.class)) {
            Data data = methodParameter.getAnnotation(Data.class);
            dataId = DataHelper._getDataId(ActionHelper.getActionId(pluginClass, method), data.id().isEmpty() ? methodParameter.getName() : data.id());
        }

        return dataId;
    }

    /**
     * Internal - Get the formatted Data Id
     *
     * @param parentId String
     * @param dataId   String
     * @return String dataId
     */
    private static String _getDataId(String parentId, String dataId) {
        return parentId + "." + DataHelper.KEY_DATA + "." + dataId;
    }
}
