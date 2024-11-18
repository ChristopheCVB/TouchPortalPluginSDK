package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DataProcessor {
    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Data} for an {@link Action} or a {@link Connector}
     *
     * @param <T>             Action or Connector Annotation
     * @param processor       {@link TouchPortalPluginAnnotationsProcessor}
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param targetElement   Element
     * @param annotation      {@link Action} or {@link Connector}
     * @param jsonElement     JsonObject
     * @param dataElement     Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; dataPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    public static <T extends Annotation> Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element targetElement, T annotation, JsonObject jsonElement, Element dataElement) throws GenericHelper.TPTypeException {
        String annotationName = annotation.annotationType().getSimpleName();
        boolean isAction = annotation instanceof Action;

        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process " + annotationName + " Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        TypeSpec.Builder dataTypeSpecBuilder = isAction ?
                SpecUtils.createActionDataTypeSpecBuilder(pluginElement, categoryElement, category, targetElement, (Action) annotation, dataElement, data) :
                SpecUtils.createConnectorDataTypeSpecBuilder(pluginElement, categoryElement, category, targetElement, (Connector) annotation, dataElement, data);

        Element method = dataElement.getEnclosingElement();
        String className = method.getEnclosingElement().getSimpleName() + "." + annotationName + "(" + dataElement.getSimpleName() + ")";

        JsonObject jsonData = new JsonObject();
        String desiredTPType = GenericHelper.getTouchPortalType(className, dataElement);
        jsonData.addProperty(DataHelper.TYPE, desiredTPType);
        jsonData.addProperty(DataHelper.LABEL, DataHelper.getDataLabel(dataElement, data));
        // Default Value
        switch (desiredTPType) {
            case GenericHelper.TP_TYPE_NUMBER:
                double defaultValue = 0;
                try {
                    defaultValue = Double.parseDouble(data.defaultValue());
                }
                catch (NumberFormatException ignored) {}
                jsonData.addProperty(DataHelper.DEFAULT, defaultValue);
                break;

            case GenericHelper.TP_TYPE_SWITCH:
                jsonData.addProperty(DataHelper.DEFAULT, data.defaultValue().equals("true"));
                break;

            default:
                jsonData.addProperty(DataHelper.DEFAULT, data.defaultValue());
                break;
        }
        AtomicReference<String> dataId = new AtomicReference<>( isAction ?
                DataHelper.getActionDataId(pluginElement, categoryElement, category, targetElement, (Action) annotation, dataElement, data) :
                DataHelper.getConnectorDataId(pluginElement, categoryElement, category, targetElement, (Connector) annotation, dataElement, data));
        // Specific properties
        switch (desiredTPType) {
            case GenericHelper.TP_TYPE_CHOICE:
                JsonArray dataValueChoices = new JsonArray();
                if (!data.stateId().isEmpty()) {
                    Optional<? extends Element> optionalStateElement = roundEnv.getElementsAnnotatedWith(State.class).stream().filter(element -> {
                        State state = element.getAnnotation(State.class);
                        String shortStateId = !state.id().isEmpty() ? state.id() : element.getSimpleName().toString();
                        return shortStateId.equals(data.stateId());
                    }).findFirst();
                    if (optionalStateElement.isPresent()) {
                        dataTypeSpecBuilder = null;

                        Element stateElement = optionalStateElement.get();
                        State state = stateElement.getAnnotation(State.class);
                        dataId.set(StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state));
                        for (String valueChoice : state.valueChoices()) {
                            dataValueChoices.add(valueChoice);
                        }
                        jsonData.addProperty(DataHelper.DEFAULT, data.defaultValue().isEmpty() ? state.defaultValue() : data.defaultValue());
                    }
                    else {
                        for (String valueChoice : data.valueChoices()) {
                            dataValueChoices.add(valueChoice);
                        }
                    }
                }
                else {
                    for (String valueChoice : data.valueChoices()) {
                        dataValueChoices.add(valueChoice);
                    }
                }
                jsonData.add(DataHelper.VALUE_CHOICES, dataValueChoices);
                break;

            case GenericHelper.TP_TYPE_FILE:
                if (data.isDirectory()) {
                    jsonData.addProperty(DataHelper.TYPE, GenericHelper.TP_TYPE_DIRECTORY);
                }
                else {
                    JsonArray jsonExtensions = new JsonArray();
                    for (String extension : data.extensions()) {
                        if (extension.matches(DataHelper.EXTENSION_FORMAT)) {
                            jsonExtensions.add(extension);
                        }
                        else {
                            processor.getMessager().printMessage(Diagnostic.Kind.ERROR, annotationName + " Data Extension: [" + extension + "] format is not valid");
                        }
                    }
                    jsonData.add(DataHelper.EXTENSIONS, jsonExtensions);
                }
                break;

            case GenericHelper.TP_TYPE_TEXT:
                if (data.isColor()) {
                    jsonData.addProperty(DataHelper.TYPE, GenericHelper.TP_TYPE_COLOR);
                    if (!data.defaultValue().isEmpty()) {
                        if (!data.defaultValue().matches(DataHelper.COLOR_FORMAT)) {
                            processor.getMessager().printMessage(Diagnostic.Kind.ERROR, annotationName + " Data Color Default value: [" + data.defaultValue() + "] format is not valid");
                        }
                    }
                }
                break;

            case GenericHelper.TP_TYPE_NUMBER:
                try {
                    double defaultValue = jsonData.get(DataHelper.DEFAULT).getAsDouble();
                    if (defaultValue < data.minValue() || defaultValue > data.maxValue()) {
                        throw new GenericHelper.TPTypeException.Builder(className).defaultNotInRange().build();
                    }
                }
                catch (NumberFormatException numberFormatException) {
                    throw new GenericHelper.TPTypeException.Builder(className).defaultInvalid(data.defaultValue()).build();
                }
                jsonData.addProperty(DataHelper.ALLOW_DECIMALS, GenericHelper.getTouchPortalTypeNumberAllowDecimals(dataElement.asType().toString()));
                if (data.minValue() > Double.NEGATIVE_INFINITY) {
                    jsonData.addProperty(DataHelper.MIN_VALUE, data.minValue());
                }
                if (data.maxValue() < Double.POSITIVE_INFINITY) {
                    jsonData.addProperty(DataHelper.MAX_VALUE, data.maxValue());
                }
                break;
        }
        jsonData.addProperty(DataHelper.ID, dataId.get());
        String format = isAction ? ((Action) annotation).format() : ((Connector) annotation).format();
        if (!format.isEmpty()) {
            // Replace wildcards
            String rawFormat = jsonElement.get(isAction ? ActionHelper.FORMAT : ConnectorHelper.FORMAT).getAsString();
            jsonElement.addProperty(isAction ? ActionHelper.FORMAT : ConnectorHelper.FORMAT, rawFormat.replace("{$" + (data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id()) + "$}", "{$" + dataId.get() + "$}"));
        }

        return Pair.create(jsonData, dataTypeSpecBuilder);
    }
}
