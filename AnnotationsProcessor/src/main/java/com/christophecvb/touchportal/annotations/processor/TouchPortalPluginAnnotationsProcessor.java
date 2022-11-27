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

package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.*;
import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Touch Portal Plugin Annotations Processor
 */
@AutoService(Processor.class)
public class TouchPortalPluginAnnotationsProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;

    public Messager getMessager() {
        return this.messager;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Plugin.class.getCanonicalName());
        annotations.add(Setting.class.getCanonicalName());
        annotations.add(Category.class.getCanonicalName());
        annotations.add(Action.class.getCanonicalName());
        annotations.add(Data.class.getCanonicalName());
        annotations.add(State.class.getCanonicalName());
        annotations.add(Event.class.getCanonicalName());
        annotations.add(Connector.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || annotations.size() == 0) {
            return false;
        }
        this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".process");

        try {
            Set<? extends Element> plugins = roundEnv.getElementsAnnotatedWith(Plugin.class);
            if (plugins.size() != 1) {
                throw new TPAnnotationException.Builder(Plugin.class).count(plugins.size()).build();
            }
            for (Element pluginElement : plugins) {
                Pair<JsonObject, TypeSpec.Builder> pluginPair = PluginProcessor.process(this, roundEnv, pluginElement);

                String entryFileName = "resources/" + PluginHelper.ENTRY_TP;
                FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", entryFileName, pluginElement);
                Writer writer = actionFileObject.openWriter();
                writer.write(pluginPair.first.toString());
                writer.flush();
                writer.close();

                TypeSpec pluginTypeSpec = pluginPair.second.build();
                String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();
                JavaFile javaConstantsFile = JavaFile.builder(packageName, pluginTypeSpec).build();
                javaConstantsFile.writeTo(this.filer);
            }
        }
        catch (Exception exception) {
            this.messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
        }

        return true;
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Action}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; actionPair
     */
    public Pair<JsonObject, TypeSpec.Builder> processAction(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());
        Action action = actionElement.getAnnotation(Action.class);

        TypeSpec.Builder actionTypeSpecBuilder = SpecUtils.createActionTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action);

        JsonObject jsonAction = new JsonObject();
        jsonAction.addProperty(ActionHelper.ID, ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action));
        jsonAction.addProperty(ActionHelper.NAME, ActionHelper.getActionName(actionElement, action));
        jsonAction.addProperty(ActionHelper.PREFIX, action.prefix());
        jsonAction.addProperty(ActionHelper.TYPE, action.type());
        if (!action.description().isEmpty()) {
            jsonAction.addProperty(ActionHelper.DESCRIPTION, action.description());
        }
        if (!action.format().isEmpty()) {
            jsonAction.addProperty(ActionHelper.FORMAT, action.format());
            jsonAction.addProperty(ActionHelper.TRY_INLINE, true);
        }
        jsonAction.addProperty(ActionHelper.HAS_HOLD_FUNCTIONALITY, action.hasHoldFunctionality());

        ActionTranslation[] actionTranslations = actionElement.getAnnotationsByType(ActionTranslation.class);
        if (actionTranslations.length > 0) {
            for (ActionTranslation actionTranslation : actionTranslations) {
                String languageCode = actionTranslation.language().getCode();
                if (!actionTranslation.name().isEmpty()) {
                    jsonAction.addProperty(ActionHelper.NAME + "_" + languageCode, actionTranslation.name());
                }
                if (!actionTranslation.prefix().isEmpty()) {
                    jsonAction.addProperty(ActionHelper.PREFIX + "_" + languageCode, actionTranslation.prefix());
                }
                if (!actionTranslation.description().isEmpty()) {
                    jsonAction.addProperty(ActionHelper.DESCRIPTION + "_" + languageCode, actionTranslation.description());
                }
                if (!actionTranslation.format().isEmpty()) {
                    jsonAction.addProperty(ActionHelper.FORMAT + "_" + languageCode, actionTranslation.format());
                }
            }
        }

        JsonArray jsonActionData = new JsonArray();
        Set<? extends Element> dataElements = roundEnv.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            if (actionElement.equals(enclosingElement)) {
                Pair<JsonObject, TypeSpec.Builder> actionDataResult = this.processActionData(roundEnv, pluginElement, plugin, categoryElement, category, actionElement, action, jsonAction, dataElement);
                jsonActionData.add(actionDataResult.first);
                if (actionDataResult.second != null) {
                    actionTypeSpecBuilder.addType(actionDataResult.second.build());
                }
            }
        }
        if (jsonActionData.size() > 0) {
            jsonAction.add(ActionHelper.DATA, jsonActionData);
        }

        return Pair.create(jsonAction, actionTypeSpecBuilder);
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Connector}
     *
     * @param roundEnv          RoundEnvironment
     * @param pluginElement     Element
     * @param plugin            {@link Plugin}
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; actionPair
     */
    public Pair<JsonObject, TypeSpec.Builder> processConnector(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element connectorElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Connector: " + connectorElement.getSimpleName());
        Connector connector = connectorElement.getAnnotation(Connector.class);

        TypeSpec.Builder connectorTypeSpecBuilder = SpecUtils.createConnectorTypeSpecBuilder(pluginElement, categoryElement, category, connectorElement, connector);

        JsonObject jsonConnector = new JsonObject();
        jsonConnector.addProperty(ConnectorHelper.ID, ConnectorHelper.getConnectorId(pluginElement, categoryElement, category, connectorElement, connector));
        jsonConnector.addProperty(ConnectorHelper.NAME, ConnectorHelper.getConnectorName(connectorElement, connector));
        jsonConnector.addProperty(ConnectorHelper.FORMAT, connector.format());

        String classMethod = pluginElement.getSimpleName() + "." + connectorElement.getSimpleName();
        boolean connectorValueFound = false;
        Set<? extends Element> connectorValueElements = roundEnv.getElementsAnnotatedWith(ConnectorValue.class);
        for (Element connectorValueElement : connectorValueElements) {
            Element enclosingElement = connectorValueElement.getEnclosingElement();
            if (connectorElement.equals(enclosingElement)) {
                String classMethodParameter = classMethod + "(" + connectorValueElement.getSimpleName() + ")";
                String desiredType = GenericHelper.getTouchPortalType(classMethodParameter, connectorValueElement);
                if (!desiredType.equals(GenericHelper.TP_TYPE_NUMBER)) {
                    throw new GenericHelper.TPTypeException.Builder(classMethodParameter).typeUnsupported(desiredType).forAnnotation(GenericHelper.TPTypeException.ForAnnotation.CONNECTOR_VALUE).build();
                }
                connectorValueFound = true;
                break;
            }
        }
        if (!connectorValueFound) {
            throw new TPAnnotationException.Builder(ConnectorValue.class).isMissing(true).forElement(connectorElement).build();
        }

        JsonArray jsonConnectorData = new JsonArray();
        Set<? extends Element> dataElements = roundEnv.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            if (connectorElement.equals(enclosingElement)) {
                Pair<JsonObject, TypeSpec.Builder> connectorDataResult = this.processConnectorData(roundEnv, pluginElement, plugin, categoryElement, category, connectorElement, connector, jsonConnector, dataElement);
                jsonConnectorData.add(connectorDataResult.first);
                if (connectorDataResult.second != null) {
                    connectorTypeSpecBuilder.addType(connectorDataResult.second.build());
                }
            }
        }
        if (jsonConnectorData.size() > 0) {
            jsonConnector.add(ConnectorHelper.DATA, jsonConnectorData);
        }

        return Pair.create(jsonConnector, connectorTypeSpecBuilder);
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link State}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; statePair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    public Pair<JsonObject, TypeSpec.Builder> processState(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element stateElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());
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

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Event}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; eventPair
     * @throws GenericHelper.TPTypeException If any used type is not Supported
     */
    public Pair<JsonObject, TypeSpec.Builder> processEvent(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element eventElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());
        State state = eventElement.getAnnotation(State.class);
        Event event = eventElement.getAnnotation(Event.class);

        String reference = eventElement.getEnclosingElement().getSimpleName() + "." + eventElement.getSimpleName();

        if (state == null) {
            throw new TPAnnotationException.Builder(State.class).isMissing(true).forElement(eventElement).build();
        }

        TypeSpec.Builder eventTypeSpecBuilder = SpecUtils.createEventTypeSpecBuilder(pluginElement, categoryElement, category, eventElement, event);

        JsonObject jsonEvent = new JsonObject();
        jsonEvent.addProperty(EventHelper.ID, EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event));
        jsonEvent.addProperty(EventHelper.TYPE, EventHelper.TYPE_COMMUNICATE);
        jsonEvent.addProperty(EventHelper.NAME, EventHelper.getEventName(eventElement, event));
        jsonEvent.addProperty(EventHelper.FORMAT, event.format());
        String desiredTPType = GenericHelper.getTouchPortalType(reference, eventElement);
        if (desiredTPType.equals(StateHelper.TYPE_TEXT)) {
            jsonEvent.addProperty(EventHelper.VALUE_TYPE, EventHelper.VALUE_TYPE_CHOICE);
            JsonArray eventValueChoices = new JsonArray();
            for (String valueChoice : event.valueChoices()) {
                eventValueChoices.add(valueChoice);
            }
            jsonEvent.add(EventHelper.VALUE_CHOICES, eventValueChoices);
            jsonEvent.addProperty(EventHelper.VALUE_STATE_ID, StateHelper.getStateId(pluginElement, categoryElement, category, eventElement, state));
        }
        else {
            throw new GenericHelper.TPTypeException.Builder(reference).typeUnsupported(desiredTPType).forAnnotation(GenericHelper.TPTypeException.ForAnnotation.EVENT).build();
        }

        return Pair.create(jsonEvent, eventTypeSpecBuilder);
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Data} for an {@link Action}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @param jsonAction      JsonObject
     * @param dataElement     Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; dataPair
     */
    private Pair<JsonObject, TypeSpec.Builder> processActionData(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement, Action action, JsonObject jsonAction, Element dataElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        TypeSpec.Builder actionDataTypeSpecBuilder = SpecUtils.createActionDataTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action, dataElement, data);

        Element method = dataElement.getEnclosingElement();
        String className = method.getEnclosingElement().getSimpleName() + "." + method.getSimpleName() + "(" + dataElement.getSimpleName() + ")";

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
        AtomicReference<String> dataId = new AtomicReference<>(DataHelper.getActionDataId(pluginElement, categoryElement, category, actionElement, action, dataElement, data));
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
                        actionDataTypeSpecBuilder = null;

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
                            this.messager.printMessage(Diagnostic.Kind.ERROR, "Action Data Extension: [" + extension + "] format is not valid");
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
                            this.messager.printMessage(Diagnostic.Kind.ERROR, "Action Data Color Default value: [" + data.defaultValue() + "] format is not valid");
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
        if (!action.format().isEmpty()) {
            // Replace wildcards
            String rawFormat = jsonAction.get(ActionHelper.FORMAT).getAsString();
            jsonAction.addProperty(ActionHelper.FORMAT, rawFormat.replace("{$" + (data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id()) + "$}", "{$" + dataId.get() + "$}"));
        }

        return Pair.create(jsonData, actionDataTypeSpecBuilder);
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Data} for a {@link Connector}
     *
     * @param roundEnv          RoundEnvironment
     * @param pluginElement     Element
     * @param plugin            {@link Plugin}
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @param connector         {@link Connector}
     * @param jsonConnector     JsonObject
     * @param dataElement       Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; dataPair
     */
    private Pair<JsonObject, TypeSpec.Builder> processConnectorData(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element connectorElement, Connector connector, JsonObject jsonConnector, Element dataElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Connector Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        TypeSpec.Builder connectorDataTypeSpecBuilder = SpecUtils.createConnectorDataTypeSpecBuilder(pluginElement, categoryElement, category, connectorElement, connector, dataElement, data);

        Element method = dataElement.getEnclosingElement();
        String className = method.getEnclosingElement().getSimpleName() + "." + method.getSimpleName() + "(" + dataElement.getSimpleName() + ")";

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
        AtomicReference<String> dataId = new AtomicReference<>(DataHelper.getConnectorDataId(pluginElement, categoryElement, category, connectorElement, connector, dataElement, data));
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
                        connectorDataTypeSpecBuilder = null;

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
                            this.messager.printMessage(Diagnostic.Kind.ERROR, "Action Data Extension: [" + extension + "] format is not valid");
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
                            this.messager.printMessage(Diagnostic.Kind.ERROR, "Action Data Color Default value: [" + data.defaultValue() + "] format is not valid");
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
        if (!connector.format().isEmpty()) {
            // Replace wildcards
            String rawFormat = jsonConnector.get(ConnectorHelper.FORMAT).getAsString();
            jsonConnector.addProperty(ConnectorHelper.FORMAT, rawFormat.replace("{$" + (data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id()) + "$}", "{$" + dataId.get() + "$}"));
        }

        return Pair.create(jsonData, connectorDataTypeSpecBuilder);
    }
}
