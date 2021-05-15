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

import com.christophecvb.touchportal.helpers.DataHelper;
import com.christophecvb.touchportal.helpers.ReceivedMessageHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

public class TPActionMessage extends TPMessage {
    public String pluginId;
    public String actionId;
    public ArrayList<Data> data;

    public static class Data {
        public String id;
        public String value;
    }

    public Object getTypedDataValue(Class<?> pluginClass, Method actionMethod, Parameter actionMethodParameter) {
        return this.getTypedDataValue(actionMethodParameter.getParameterizedType().getTypeName(), DataHelper.getActionDataId(pluginClass, actionMethod, actionMethodParameter));
    }

    public Object getTypedDataValue(String actionDataType, String actionDataId) {
        Object value = null;

        Data data = this.data.stream().filter(datum -> datum.id.equals(actionDataId)).findFirst().orElse(null);
        if (data != null) {
            value = ReceivedMessageHelper.getTypedValue(actionDataType, data.value);
        }

        return value;
    }
}
