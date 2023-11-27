package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.EventHelper;
import com.christophecvb.touchportal.helpers.GenericHelper;
import com.christophecvb.touchportal.helpers.StateHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class EventProcessor {
    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Event}
     *
     * @param processor       {@link TouchPortalPluginAnnotationsProcessor}
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; eventPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     * @throws TPAnnotationException If an Annotation is misused
     */
    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element eventElement) throws GenericHelper.TPTypeException, TPAnnotationException {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());
        State state = eventElement.getAnnotation(State.class);
        Event event = eventElement.getAnnotation(Event.class);

        if (state == null) {
            throw new TPAnnotationException.Builder(State.class).isMissing(true).forElement(eventElement).build();
        }

        String reference = eventElement.getEnclosingElement().getSimpleName() + "." + eventElement.getSimpleName();

        TypeSpec.Builder eventTypeSpecBuilder = SpecUtils.createEventTypeSpecBuilder(pluginElement, categoryElement, category, eventElement, event);

        JsonObject jsonEvent = new JsonObject();
        jsonEvent.addProperty(EventHelper.ID, EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event));
        jsonEvent.addProperty(EventHelper.TYPE, EventHelper.TYPE_COMMUNICATE);
        jsonEvent.addProperty(EventHelper.NAME, EventHelper.getEventName(eventElement, event));
        jsonEvent.addProperty(EventHelper.FORMAT, event.format());
        jsonEvent.addProperty(EventHelper.VALUE_STATE_ID, StateHelper.getStateId(pluginElement, categoryElement, category, eventElement, state));

        String desiredTPType = GenericHelper.getTouchPortalType(reference, eventElement);

        if (desiredTPType.equals(EventHelper.VALUE_TYPE)) {
            if (event.valueChoices().length > 0) {
                jsonEvent.addProperty(EventHelper.VALUE_TYPE, StateHelper.TYPE_CHOICE);
                JsonArray eventValueChoices = new JsonArray();
                for (String valueChoice : event.valueChoices()) {
                    eventValueChoices.add(valueChoice);
                }
                jsonEvent.add(EventHelper.VALUE_CHOICES, eventValueChoices);
            } else {
                jsonEvent.addProperty(EventHelper.VALUE_TYPE, StateHelper.TYPE_TEXT);
            }
        }

        return Pair.create(jsonEvent, eventTypeSpecBuilder);
    }
}
