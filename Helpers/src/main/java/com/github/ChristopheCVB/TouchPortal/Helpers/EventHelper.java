package com.github.ChristopheCVB.TouchPortal.Helpers;

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
     * @param eventElement Element
     * @param event {@link Event}
     * @return String eventId
     */
    public static String getEventId(Element eventElement, Event event) {
        return EventHelper._getEventId(CategoryHelper.getCategoryId(eventElement.getEnclosingElement()), event.id().isEmpty() ? eventElement.getSimpleName().toString() : event.id());
    }

    /**
     * Get the formatted Event Name
     *
     * @param element Element
     * @param event {@link Event}
     * @return String eventName
     */
    public static String getEventName(Element element, Event event) {
        return event.name().isEmpty() ? element.getSimpleName().toString() : event.name();
    }

    private static String _getEventId(String categoryId, String rawEventId) {
        return categoryId + "." + EventHelper.KEY_EVENT + "." + rawEventId;
    }
}
