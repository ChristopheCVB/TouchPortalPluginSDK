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

import javax.lang.model.element.Element;

/**
 * Touch Portal Plugin Category Helper
 */
public class CategoryHelper {
    public static final String ID = GenericHelper.ID;
    public static final String NAME = GenericHelper.NAME;
    public static final String IMAGE_PATH = "imagepath";
    public static final String ACTIONS = "actions";
    public static final String EVENTS = "events";
    public static final String STATES = "states";

    /**
     * Get the formatted Category ID
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @return String categoryId
     */
    public static String getCategoryId(Element pluginElement, Element categoryElement, Category category) {
        return CategoryHelper._getCategoryId(PluginHelper.getPluginId(pluginElement), category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id());
    }

    /**
     * Get the formatted Category Name
     *
     * @param categoryElement Element
     * @param category        {@link Category}
     * @return String categoryName
     */
    public static String getCategoryName(Element categoryElement, Category category) {
        return category.name().isEmpty() ? categoryElement.getSimpleName().toString() : category.name();
    }

    /**
     * Get the formatted Category ID
     *
     * @param pluginClass Class
     * @param categoryId  String
     * @return String categoryId
     */
    public static String getCategoryId(Class pluginClass, String categoryId) {
        return CategoryHelper._getCategoryId(PluginHelper.getPluginId(pluginClass), categoryId);
    }

    /**
     * Internal Get the formatted Category ID
     *
     * @param pluginId   String
     * @param categoryId String
     * @return String categoryId
     */
    private static String _getCategoryId(String pluginId, String categoryId) {
        return pluginId + "." + categoryId;
    }
}
