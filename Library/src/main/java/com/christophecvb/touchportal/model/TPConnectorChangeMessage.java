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

package com.christophecvb.touchportal.model;

import com.christophecvb.touchportal.helpers.ConnectorHelper;
import com.christophecvb.touchportal.helpers.DataHelper;
import com.christophecvb.touchportal.helpers.ReceivedMessageHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class TPConnectorChangeMessage extends TPMessage {
    public String pluginId;
    public String connectorId;
    public Integer value;
    public ArrayList<Data> data;

    public static class Data {
        public String id;
        public String value;
    }

    public Object getTypedDataValue(Class<?> pluginClass, Method actionMethod, Parameter connectorMethodParameter) {
        return this.getTypedDataValue(connectorMethodParameter.getParameterizedType().getTypeName(), DataHelper.getDataId(pluginClass, actionMethod, connectorMethodParameter));
    }

    public Object getTypedDataValue(Class<?> pluginClass, Field actionField) {
        return this.getTypedDataValue(actionField.getType().getTypeName(), DataHelper.getDataId(pluginClass, actionField));
    }

    public Object getTypedDataValue(String connectorDataType, String connectorDataId) {
        Object value = null;

        Data data = this.data.stream().filter(datum -> datum.id.equals(connectorDataId)).findFirst().orElse(null);
        if (data != null) {
            value = ReceivedMessageHelper.getTypedValue(connectorDataType, data.value);
        }

        return value;
    }

    public String getConstructedId() {
        return ConnectorHelper.getConstructedId(
                this.pluginId,
                this.connectorId,
                this.value,
                this.data.stream().collect(Collectors.toMap(data -> data.id, data -> data.value))
        );
    }
}
