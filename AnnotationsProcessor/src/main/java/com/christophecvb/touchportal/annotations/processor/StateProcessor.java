package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.Category;
import com.christophecvb.touchportal.annotations.Event;
import com.christophecvb.touchportal.annotations.Plugin;
import com.christophecvb.touchportal.annotations.State;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.GenericHelper;
import com.christophecvb.touchportal.helpers.StateHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class StateProcessor {
    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link State}
     *
     * @param processor       {@link TouchPortalPluginAnnotationsProcessor}
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; statePair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     * @throws TPAnnotationException If an Annotation is misused
     */
    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element stateElement) throws GenericHelper.TPTypeException, TPAnnotationException {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());
        State state = stateElement.getAnnotation(State.class);

        TypeSpec.Builder stateTypeSpecBuilder = SpecUtils.createStateTypeSpecBuilder(pluginElement, categoryElement, category, stateElement, state);

        String className = stateElement.getEnclosingElement().getSimpleName() + "." + stateElement.getSimpleName();

        JsonObject jsonState = new JsonObject();
        jsonState.addProperty(StateHelper.ID, StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state));
        String desiredTPType = GenericHelper.getTouchPortalType(className, stateElement);
        jsonState.addProperty(StateHelper.TYPE, desiredTPType);
        jsonState.addProperty(StateHelper.DESC, StateHelper.getStateDesc(stateElement, state));
        jsonState.addProperty(StateHelper.DEFAULT, state.defaultValue());
        if (desiredTPType.equals(StateHelper.TYPE_CHOICE)) {
            JsonArray stateValueChoices = new JsonArray();
            for (String valueChoice : state.valueChoices()) {
                stateValueChoices.add(valueChoice);
            }
            jsonState.add(StateHelper.VALUE_CHOICES, stateValueChoices);
        }
        else if (!desiredTPType.equals(StateHelper.TYPE_TEXT)) {
            throw new GenericHelper.TPTypeException.Builder(className).typeUnsupported(desiredTPType).forAnnotation(GenericHelper.TPTypeException.ForAnnotation.STATE).build();
        }

        Event event = stateElement.getAnnotation(Event.class);
        if (event != null && !desiredTPType.equals(StateHelper.TYPE_TEXT)) {
            throw new TPAnnotationException.Builder(State.class).typeFor(desiredTPType, className, "the field is also Annotated with Event. Only the type " + StateHelper.TYPE_TEXT + " is supported for a State that has an Event Annotation.").build();
        }

        return Pair.create(jsonState, stateTypeSpecBuilder);
    }
}
