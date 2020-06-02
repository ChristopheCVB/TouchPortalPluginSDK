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
import com.github.ChristopheCVB.TouchPortal.Annotations.Event;

import javax.lang.model.element.Element;

/**
 * Touch Portal Plugin Event Helper
 */
public class EventHelper {
    public static final String ID = GenericHelper.ID;
    public static final String NAME = GenericHelper.NAME;
    public static final String FORMAT = "format";
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_COMMUNICATE = "communicate";
    public static final String VALUE_TYPE = "valueType";
    public static final String VALUE_TYPE_CHOICE = GenericHelper.TYPE_CHOICE;
    public static final String VALUE_CHOICES = "valueChoices";
    public static final String VALUE_STATE_ID = "valueStateId";

    private static final String KEY_EVENT = "event";

    /**
     * Get the formatted Event ID
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @param event           {@link Event}
     * @return String eventId
     */
    public static String getEventId(Element pluginElement, Element categoryElement, Category category, Element eventElement, Event event) {
        return EventHelper._getEventId(CategoryHelper.getCategoryId(pluginElement, categoryElement, category), event.id().isEmpty() ? eventElement.getSimpleName().toString() : event.id());
    }

    /**
     * Get the formatted Event Name
     *
     * @param element Element
     * @param event   {@link Event}
     * @return String eventName
     */
    public static String getEventName(Element element, Event event) {
        return event.name().isEmpty() ? element.getSimpleName().toString() : event.name();
    }

    /**
     * Get the formatted Event ID
     *
     * @param pluginClass Class
     * @param categoryId  String
     * @param eventId     String
     * @return String eventId
     */
    public static String getEventId(Class<?> pluginClass, String categoryId, String eventId) {
        return EventHelper._getEventId(CategoryHelper.getCategoryId(pluginClass, categoryId), eventId);
    }

    /**
     * Internal Get the formatted Event ID
     *
     * @param categoryId String
     * @param eventId    String
     * @return String eventId
     */
    private static String _getEventId(String categoryId, String eventId) {
        return categoryId + "." + EventHelper.KEY_EVENT + "." + eventId;
    }
}
