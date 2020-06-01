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

import com.github.ChristopheCVB.TouchPortal.Annotations.Action;
import com.github.ChristopheCVB.TouchPortal.Annotations.Category;

import javax.lang.model.element.Element;
import java.lang.reflect.Method;

/**
 * Touch Portal Plugin Action Helper
 */
public class ActionHelper {
    public static final String ID = GenericHelper.ID;
    public static final String NAME = GenericHelper.NAME;
    public static final String PREFIX = "prefix";
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_EXECUTE = "execute";
    public static final String TYPE_COMMUNICATE = "communicate";
    public static final String EXECUTION_TYPE = "executionType";
    public static final String EXECUTION_COMMAND = "execution_cmd";
    public static final String DESCRIPTION = GenericHelper.DESCRIPTION;
    public static final String DATA = "data";
    public static final String TRY_INLINE = "tryInline";
    public static final String FORMAT = "format";

    protected static final String KEY_ACTION = "action";

    /**
     * Get the formatted Action ID
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @return String actionId
     */
    public static String getActionId(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action) {
        return ActionHelper._getActionId(CategoryHelper.getCategoryId(pluginElement, categoryElement, category), action.id().isEmpty() ? actionElement.getSimpleName().toString() : action.id());
    }

    /**
     * Get the formatted Action Name
     *
     * @param actionElement Element
     * @param action        {@link Action}
     * @return String actionName
     */
    public static String getActionName(Element actionElement, Action action) {
        return action.name().isEmpty() ? actionElement.getSimpleName().toString() : action.name();
    }

    /**
     * Get the formatted Action ID
     *
     * @param pluginClass      Class
     * @param actionMethodName String
     * @return String actionId
     */
    public static String getActionId(Class pluginClass, String actionMethodName) {
        String actionId = "";

        for (Method method : pluginClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Action.class) && method.getName().equals(actionMethodName)) {
                Action action = method.getDeclaredAnnotation(Action.class);
                actionId = ActionHelper._getActionId(CategoryHelper.getCategoryId(pluginClass, action.categoryId()), (!action.id().isEmpty() ? action.id() : actionMethodName));
            }
        }

        return actionId;
    }

    /**
     * Internal Get the formatted Action ID
     *
     * @param categoryId  String
     * @param rawActionId String
     * @return String actionId
     */
    private static String _getActionId(String categoryId, String rawActionId) {
        return categoryId + "." + ActionHelper.KEY_ACTION + "." + rawActionId;
    }
}
