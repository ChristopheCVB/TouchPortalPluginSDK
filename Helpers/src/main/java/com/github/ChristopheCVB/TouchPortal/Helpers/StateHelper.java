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

import com.github.ChristopheCVB.TouchPortal.Annotations.Category;
import com.github.ChristopheCVB.TouchPortal.Annotations.State;

import javax.lang.model.element.Element;
import java.lang.reflect.Field;

/**
 * Touch Portal Plugin State Helper
 */
public class StateHelper {
    public static final String ID = GenericHelper.ID;
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_CHOICE = GenericHelper.TP_TYPE_CHOICE;
    public static final String TYPE_TEXT = GenericHelper.TP_TYPE_TEXT;
    public static final String DESC = "desc";
    public static final String DEFAULT = GenericHelper.DEFAULT;
    public static final String VALUE_CHOICES = GenericHelper.VALUE_CHOICES;

    protected static final String KEY_STATE = "state";

    /**
     * Get the generated State ID
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @param state           {@link State}
     * @return String stateId
     */
    public static String getStateId(Element pluginElement, Element categoryElement, Category category, Element stateElement, State state) {
        return StateHelper._getStateId(CategoryHelper.getCategoryId(pluginElement, categoryElement, category), state.id().isEmpty() ? stateElement.getSimpleName().toString() : state.id());
    }

    /**
     * Get the generated State Desc
     *
     * @param element Element
     * @param state   {@link State}
     * @return String stateDesc
     */
    public static String getStateDesc(Element element, State state) {
        return state.desc().isEmpty() ? element.getSimpleName().toString() : state.desc();
    }

    /**
     * Get the generated State ID
     *
     * @param pluginClass    Class
     * @param categoryId     String
     * @param stateFieldName String
     * @return String stateId
     */
    public static String getStateId(Class<?> pluginClass, String categoryId, String stateFieldName) {
        String stateId = "";

        try {
            Field stateField = pluginClass.getDeclaredField(stateFieldName);
            if (stateField.isAnnotationPresent(State.class)) {
                State state = stateField.getAnnotation(State.class);
                stateId = StateHelper._getStateId(CategoryHelper.getCategoryId(pluginClass, categoryId), state.id().isEmpty() ? stateFieldName : state.id());
            }
        }
        catch (NoSuchFieldException e) {
            stateId = StateHelper._getStateId(CategoryHelper.getCategoryId(pluginClass, categoryId), stateFieldName);
        }

        return stateId;
    }

    /**
     * Internal - Get the formatted State ID
     *
     * @param categoryId String
     * @param rawStateId String
     * @return stateId
     */
    private static String _getStateId(String categoryId, String rawStateId) {
        return categoryId + "." + StateHelper.KEY_STATE + "." + rawStateId;
    }
}
