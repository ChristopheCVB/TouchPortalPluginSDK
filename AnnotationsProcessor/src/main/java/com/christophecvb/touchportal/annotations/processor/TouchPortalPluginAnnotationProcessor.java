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
import com.christophecvb.touchportal.helpers.*;
import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.lang.annotation.AnnotationFormatError;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Touch Portal Plugin Annotation Processor
 */
@AutoService(Processor.class)
public class TouchPortalPluginAnnotationProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
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
                throw new Exception("You need 1(one) @Plugin Annotation, you have " + plugins.size());
            }
            for (Element pluginElement : plugins) {
                Pair<JsonObject, TypeSpec.Builder> pluginPair = this.processPlugin(roundEnv, pluginElement);

                String actionFileName = "resources/" + PluginHelper.ENTRY_TP;
                FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", actionFileName, pluginElement);
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
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Plugin}
     *
     * @param roundEnv      RoundEnvironment
     * @param pluginElement Element
     * @return Pair<JsonObject, TypeSpec.Builder> pluginPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    private Pair<JsonObject, TypeSpec.Builder> processPlugin(RoundEnvironment roundEnv, Element pluginElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);

        TypeSpec.Builder pluginTypeSpecBuilder = this.createPluginTypeSpecBuilder(pluginElement, plugin);

        JsonObject jsonPlugin = new JsonObject();
        jsonPlugin.addProperty(PluginHelper.SDK, PluginHelper.TOUCH_PORTAL_PLUGIN_VERSION);
        jsonPlugin.addProperty(PluginHelper.VERSION, plugin.version());
        jsonPlugin.addProperty(PluginHelper.NAME, PluginHelper.getPluginName(pluginElement, plugin));
        jsonPlugin.addProperty(PluginHelper.ID, PluginHelper.getPluginId(pluginElement));
        JsonObject jsonConfiguration = new JsonObject();
        jsonConfiguration.addProperty(PluginHelper.CONFIGURATION_COLOR_DARK, plugin.colorDark());
        jsonConfiguration.addProperty(PluginHelper.CONFIGURATION_COLOR_LIGHT, plugin.colorLight());
        jsonPlugin.add(PluginHelper.CONFIGURATION, jsonConfiguration);
        jsonPlugin.addProperty(PluginHelper.PLUGIN_START_COMMAND, "java -Dapple.awt.UIElement=true -jar ./" + pluginElement.getSimpleName() + ".jar " + PluginHelper.COMMAND_START);

        TypeSpec.Builder settingsTypeSpecBuilder = TypeSpec.classBuilder("Settings").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        JsonArray jsonSettings = new JsonArray();
        Set<? extends Element> settingElements = roundEnv.getElementsAnnotatedWith(Setting.class);
        for (Element settingElement : settingElements) {
            Pair<JsonObject, TypeSpec.Builder> settingResult = this.processSetting(settingElement);
            jsonSettings.add(settingResult.first);
            settingsTypeSpecBuilder.addType(settingResult.second.build());
        }
        if (jsonSettings.size() > 0) {
            jsonPlugin.add(PluginHelper.SETTINGS, jsonSettings);
        }
        pluginTypeSpecBuilder.addType(settingsTypeSpecBuilder.build());

        JsonArray jsonCategories = new JsonArray();
        Set<? extends Element> categoryElements = roundEnv.getElementsAnnotatedWith(Category.class);
        for (Element categoryElement : categoryElements) {
            Pair<JsonObject, TypeSpec.Builder> categoryResult = this.processCategory(roundEnv, pluginElement, plugin, categoryElement);
            jsonCategories.add(categoryResult.first);
            pluginTypeSpecBuilder.addType(categoryResult.second.build());
        }
        if (jsonCategories.size() > 0) {
            jsonPlugin.add(PluginHelper.CATEGORIES, jsonCategories);
        }
        else {
            throw new Exception("Category Annotation missing");
        }

        return Pair.create(jsonPlugin, pluginTypeSpecBuilder);
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Setting}
     *
     * @param settingElement Element
     * @return Pair<JsonObject, TypeSpec.Builder> statePair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    private Pair<JsonObject, TypeSpec.Builder> processSetting(Element settingElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Setting: " + settingElement.getSimpleName());
        Setting setting = settingElement.getAnnotation(Setting.class);

        TypeSpec.Builder settingTypeSpecBuilder = this.createSettingTypeSpecBuilder(settingElement, setting);

        String className = settingElement.getEnclosingElement().getSimpleName() + "." + settingElement.getSimpleName();

        JsonObject jsonSetting = new JsonObject();
        jsonSetting.addProperty(SettingHelper.NAME, SettingHelper.getSettingName(settingElement, setting));
        String desiredTPType = GenericHelper.getTouchPortalType(className, settingElement);
        jsonSetting.addProperty(SettingHelper.TYPE, desiredTPType);
        jsonSetting.addProperty(SettingHelper.DEFAULT, setting.defaultValue());
        jsonSetting.addProperty(SettingHelper.IS_READ_ONLY, setting.isReadOnly());
        switch (desiredTPType) {
            case SettingHelper.TYPE_TEXT:
                if (setting.maxLength() > 0) {
                    jsonSetting.addProperty(SettingHelper.MAX_LENGTH, setting.maxLength());
                }
                if (setting.isPassword()) {
                    jsonSetting.addProperty(SettingHelper.IS_PASSWORD, true);
                }
                break;

            case SettingHelper.TYPE_NUMBER:
                try {
                    double defaultValue = Double.parseDouble(setting.defaultValue());
                    if (defaultValue < setting.minValue() || defaultValue > setting.maxValue()) {
                        throw new GenericHelper.TPTypeException.Builder(className).defaultNotInRange().build();
                    }
                }
                catch (NumberFormatException numberFormatException) {
                    throw new GenericHelper.TPTypeException.Builder(className).defaultInvalid(setting.defaultValue()).build();
                }
                if (setting.minValue() > Double.NEGATIVE_INFINITY) {
                    jsonSetting.addProperty(SettingHelper.MIN_VALUE, setting.minValue());
                }
                if (setting.maxValue() < Double.POSITIVE_INFINITY) {
                    jsonSetting.addProperty(SettingHelper.MAX_VALUE, setting.maxValue());
                }
                break;

            default:
                throw new GenericHelper.TPTypeException.Builder(className).typeUnsupported(desiredTPType).forAnnotation(GenericHelper.TPTypeException.ForAnnotation.SETTING).build();
        }

        return Pair.create(jsonSetting, settingTypeSpecBuilder);
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Category}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @return Pair<JsonObject, TypeSpec.Builder> categoryPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    private Pair<JsonObject, TypeSpec.Builder> processCategory(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Category: " + categoryElement.getSimpleName());
        Category category = categoryElement.getAnnotation(Category.class);

        TypeSpec.Builder categoryTypeSpecBuilder = this.createCategoryTypeSpecBuilder(pluginElement, categoryElement, category);

        JsonObject jsonCategory = new JsonObject();
        jsonCategory.addProperty(CategoryHelper.ID, CategoryHelper.getCategoryId(pluginElement, categoryElement, category));
        jsonCategory.addProperty(CategoryHelper.NAME, CategoryHelper.getCategoryName(categoryElement, category));
        jsonCategory.addProperty(CategoryHelper.IMAGE_PATH, PluginHelper.TP_PLUGIN_FOLDER + pluginElement.getSimpleName() + "/" + category.imagePath());

        TypeSpec.Builder actionsTypeSpecBuilder = TypeSpec.classBuilder("Actions").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        JsonArray jsonActions = new JsonArray();
        Set<? extends Element> actionElements = roundEnv.getElementsAnnotatedWith(Action.class);
        for (Element actionElement : actionElements) {
            Action action = actionElement.getAnnotation(Action.class);
            String categoryId = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();
            if (categoryId.equals(action.categoryId())) {
                Pair<JsonObject, TypeSpec.Builder> actionResult = this.processAction(roundEnv, pluginElement, plugin, categoryElement, category, actionElement);
                jsonActions.add(actionResult.first);
                actionsTypeSpecBuilder.addType(actionResult.second.build());
            }
        }
        categoryTypeSpecBuilder.addType(actionsTypeSpecBuilder.build());
        jsonCategory.add(CategoryHelper.ACTIONS, jsonActions);

        TypeSpec.Builder connectorsTypeSpecBuilder = TypeSpec.classBuilder("Connectors").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        JsonArray jsonConnectors = new JsonArray();
        Set<? extends Element> connectorElements = roundEnv.getElementsAnnotatedWith(Connector.class);
        for (Element connectorElement : connectorElements) {
            Connector connector = connectorElement.getAnnotation(Connector.class);
            String categoryId = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();
            if (categoryId.equals(connector.categoryId())) {
                Pair<JsonObject, TypeSpec.Builder> connectorResult = this.processConnector(roundEnv, pluginElement, plugin, categoryElement, category, connectorElement);
                jsonConnectors.add(connectorResult.first);
                connectorsTypeSpecBuilder.addType(connectorResult.second.build());
            }
        }
        categoryTypeSpecBuilder.addType(connectorsTypeSpecBuilder.build());
        jsonCategory.add(CategoryHelper.CONNECTORS, jsonConnectors);

        TypeSpec.Builder statesTypeSpecBuilder = TypeSpec.classBuilder("States").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        JsonArray jsonStates = new JsonArray();
        Set<? extends Element> stateElements = roundEnv.getElementsAnnotatedWith(State.class);
        for (Element stateElement : stateElements) {
            State state = stateElement.getAnnotation(State.class);
            String categoryId = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();
            if (categoryId.equals(state.categoryId())) {
                Pair<JsonObject, TypeSpec.Builder> stateResult = this.processState(roundEnv, pluginElement, plugin, categoryElement, category, stateElement);
                jsonStates.add(stateResult.first);
                statesTypeSpecBuilder.addType(stateResult.second.build());
            }
        }
        categoryTypeSpecBuilder.addType(statesTypeSpecBuilder.build());
        jsonCategory.add(CategoryHelper.STATES, jsonStates);

        TypeSpec.Builder eventsTypeSpecBuilder = TypeSpec.classBuilder("Events").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        JsonArray jsonEvents = new JsonArray();
        Set<? extends Element> eventElements = roundEnv.getElementsAnnotatedWith(Event.class);
        for (Element eventElement : eventElements) {
            State state = eventElement.getAnnotation(State.class);
            String categoryId = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();
            if (state != null) {
                if (categoryId.equals(state.categoryId())) {
                    Pair<JsonObject, TypeSpec.Builder> eventResult = this.processEvent(roundEnv, pluginElement, plugin, categoryElement, category, eventElement);
                    jsonEvents.add(eventResult.first);
                    eventsTypeSpecBuilder.addType(eventResult.second.build());
                }
            }
            else {
                throw new AnnotationFormatError("The State Annotation is missing for element " + eventElement.getSimpleName());
            }
        }
        categoryTypeSpecBuilder.addType(eventsTypeSpecBuilder.build());
        jsonCategory.add(CategoryHelper.EVENTS, jsonEvents);

        return Pair.create(jsonCategory, categoryTypeSpecBuilder);
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
     * @return Pair<JsonObject, TypeSpec.Builder> actionPair
     */
    private Pair<JsonObject, TypeSpec.Builder> processAction(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());
        Action action = actionElement.getAnnotation(Action.class);

        TypeSpec.Builder actionTypeSpecBuilder = this.createActionTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action);

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
     * @return Pair<JsonObject, TypeSpec.Builder> actionPair
     */
    private Pair<JsonObject, TypeSpec.Builder> processConnector(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element connectorElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Connector: " + connectorElement.getSimpleName());
        Connector connector = connectorElement.getAnnotation(Connector.class);

        TypeSpec.Builder connectorTypeSpecBuilder = this.createConnectorTypeSpecBuilder(pluginElement, categoryElement, category, connectorElement, connector);

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
            throw new IllegalArgumentException("Connector " + classMethod + " has no declared ConnectorValue");
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
     * @return Pair<JsonObject, TypeSpec.Builder> statePair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    private Pair<JsonObject, TypeSpec.Builder> processState(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element stateElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());
        State state = stateElement.getAnnotation(State.class);

        TypeSpec.Builder stateTypeSpecBuilder = this.createStateTypeSpecBuilder(pluginElement, categoryElement, category, stateElement, state);

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
            throw new Exception("The type of the State Annotation for " + className + " cannot be " + desiredTPType + " because the field is also Annotated with Event. Only the type " + StateHelper.TYPE_TEXT + " is supported for a State that has an Event Annotation.");
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
     * @return Pair<JsonObject, TypeSpec.Builder> eventPair
     * @throws GenericHelper.TPTypeException If any used type is not Supported
     */
    private Pair<JsonObject, TypeSpec.Builder> processEvent(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element eventElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());
        State state = eventElement.getAnnotation(State.class);
        Event event = eventElement.getAnnotation(Event.class);

        String reference = eventElement.getEnclosingElement().getSimpleName() + "." + eventElement.getSimpleName();

        if (state == null) {
            throw new Exception("The Event Annotation on " + reference + " must be used with the State Annotation");
        }

        TypeSpec.Builder eventTypeSpecBuilder = this.createEventTypeSpecBuilder(pluginElement, categoryElement, category, eventElement, event);

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
     * @return Pair<JsonObject, TypeSpec.Builder> dataPair
     */
    private Pair<JsonObject, TypeSpec.Builder> processActionData(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement, Action action, JsonObject jsonAction, Element dataElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        TypeSpec.Builder actionDataTypeSpecBuilder = this.createActionDataTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action, dataElement, data);

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
     * @return Pair<JsonObject, TypeSpec.Builder> dataPair
     */
    private Pair<JsonObject, TypeSpec.Builder> processConnectorData(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element connectorElement, Connector connector, JsonObject jsonConnector, Element dataElement) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Connector Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        TypeSpec.Builder connectorDataTypeSpecBuilder = this.createConnectorDataTypeSpecBuilder(pluginElement, categoryElement, category, connectorElement, connector, dataElement, data);

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

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Plugin}
     *
     * @param pluginElement Element
     * @param plugin        {@link Plugin}
     * @return TypeSpec.Builder pluginTypeSpecBuilder
     */
    private TypeSpec.Builder createPluginTypeSpecBuilder(Element pluginElement, Plugin plugin) {
        String simpleClassName = pluginElement.getSimpleName().toString() + "Constants";

        TypeSpec.Builder pluginTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName));
        pluginTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        pluginTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", PluginHelper.getPluginId(pluginElement)));
        pluginTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("name", plugin.name()));
        pluginTypeSpecBuilder.addField(this.getStaticFinalLongFieldSpec("version", plugin.version()));

        return pluginTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Category}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @return TypeSpec.Builder pluginTypeSpecBuilder
     */
    private TypeSpec.Builder createCategoryTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category) {
        String simpleClassName = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();

        TypeSpec.Builder categoryTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        categoryTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        categoryTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", CategoryHelper.getCategoryId(pluginElement, categoryElement, category)));
        categoryTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("name", category.name()));
        categoryTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("image_path", category.imagePath()));

        return categoryTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Action}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @return TypeSpec.Builder actionTypeSpecBuilder
     */
    private TypeSpec.Builder createActionTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action) {
        String simpleClassName = action.id().isEmpty() ? actionElement.getSimpleName().toString() : action.id();

        TypeSpec.Builder actionTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        actionTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action)));
        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("name", ActionHelper.getActionName(actionElement, action)));
        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("prefix", action.prefix()));
        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("description", action.description()));
        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("type", action.type()));
        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("format", action.format()));
        actionTypeSpecBuilder.addField(this.getStaticFinalBooleanFieldSpec("has_hold_functionality", action.hasHoldFunctionality()));

        return actionTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Action}
     *
     * @param pluginElement     Element
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @param connector         {@link Action}
     * @return TypeSpec.Builder actionTypeSpecBuilder
     */
    private TypeSpec.Builder createConnectorTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element connectorElement, Connector connector) {
        String simpleClassName = connector.id().isEmpty() ? connectorElement.getSimpleName().toString() : connector.id();

        TypeSpec.Builder actionTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        actionTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", ConnectorHelper.getConnectorId(pluginElement, categoryElement, category, connectorElement, connector)));
        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("name", ConnectorHelper.getConnectorName(connectorElement, connector)));
        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("format", connector.format()));

        return actionTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Data} for an {@link Action}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @param dataElement     Element
     * @param data            {@link Data}
     * @return TypeSpec.Builder dataTypeSpecBuilder
     */
    private TypeSpec.Builder createActionDataTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action, Element dataElement, Data data) {
        String simpleClassName = data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id();

        TypeSpec.Builder actionDataTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        actionDataTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", DataHelper.getActionDataId(pluginElement, categoryElement, category, actionElement, action, dataElement, data)));
        actionDataTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("label", DataHelper.getDataLabel(dataElement, data)));
        actionDataTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("default_value", data.defaultValue()));
        actionDataTypeSpecBuilder.addField(this.getStaticFinalStringArrayFieldSpec("value_choices", data.valueChoices()));
        actionDataTypeSpecBuilder.addField(this.getStaticFinalStringArrayFieldSpec("extensions", data.extensions()));
        actionDataTypeSpecBuilder.addField(this.getStaticFinalBooleanFieldSpec("is_directory", data.isDirectory()));
        actionDataTypeSpecBuilder.addField(this.getStaticFinalBooleanFieldSpec("is_color", data.isColor()));
        if (data.minValue() > Double.NEGATIVE_INFINITY) {
            actionDataTypeSpecBuilder.addField(this.getStaticFinalDoubleFieldSpec("min_value", data.minValue()));
        }
        if (data.maxValue() < Double.POSITIVE_INFINITY) {
            actionDataTypeSpecBuilder.addField(this.getStaticFinalDoubleFieldSpec("max_value", data.maxValue()));
        }

        return actionDataTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Data} for a {@link Connector}
     *
     * @param pluginElement     Element
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @param connector         {@link Connector}
     * @param dataElement       Element
     * @param data              {@link Data}
     * @return TypeSpec.Builder dataTypeSpecBuilder
     */
    private TypeSpec.Builder createConnectorDataTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element connectorElement, Connector connector, Element dataElement, Data data) {
        String simpleClassName = data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id();

        TypeSpec.Builder connectorDataTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        connectorDataTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", DataHelper.getConnectorDataId(pluginElement, categoryElement, category, connectorElement, connector, dataElement, data)));
        connectorDataTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("label", DataHelper.getDataLabel(dataElement, data)));
        connectorDataTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("default_value", data.defaultValue()));
        connectorDataTypeSpecBuilder.addField(this.getStaticFinalStringArrayFieldSpec("value_choices", data.valueChoices()));
        connectorDataTypeSpecBuilder.addField(this.getStaticFinalStringArrayFieldSpec("extensions", data.extensions()));
        connectorDataTypeSpecBuilder.addField(this.getStaticFinalBooleanFieldSpec("is_directory", data.isDirectory()));
        connectorDataTypeSpecBuilder.addField(this.getStaticFinalBooleanFieldSpec("is_color", data.isColor()));
        if (data.minValue() > Double.NEGATIVE_INFINITY) {
            connectorDataTypeSpecBuilder.addField(this.getStaticFinalDoubleFieldSpec("min_value", data.minValue()));
        }
        if (data.maxValue() < Double.POSITIVE_INFINITY) {
            connectorDataTypeSpecBuilder.addField(this.getStaticFinalDoubleFieldSpec("max_value", data.maxValue()));
        }

        return connectorDataTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Setting}
     *
     * @param settingElement  Element
     * @param setting         {@link Setting}
     * @return TypeSpec.Builder stateTypeSpecBuilder
     */
    private TypeSpec.Builder createSettingTypeSpecBuilder(Element settingElement, Setting setting) {
        String simpleClassName = settingElement.getSimpleName().toString();

        TypeSpec.Builder stateTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        stateTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("name", SettingHelper.getSettingName(settingElement, setting)));
        stateTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("default", setting.defaultValue()));
        stateTypeSpecBuilder.addField(this.getStaticFinalDoubleFieldSpec("max_length", setting.maxLength()));
        stateTypeSpecBuilder.addField(this.getStaticFinalBooleanFieldSpec("is_password", setting.isPassword()));
        if (setting.minValue() > Double.NEGATIVE_INFINITY) {
            stateTypeSpecBuilder.addField(this.getStaticFinalDoubleFieldSpec("min_value", setting.minValue()));
        }
        if (setting.maxValue() < Double.POSITIVE_INFINITY) {
            stateTypeSpecBuilder.addField(this.getStaticFinalDoubleFieldSpec("max_value", setting.maxValue()));
        }

        return stateTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link State}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @param state           {@link State}
     * @return TypeSpec.Builder stateTypeSpecBuilder
     */
    private TypeSpec.Builder createStateTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element stateElement, State state) {
        String simpleClassName = state.id().isEmpty() ? stateElement.getSimpleName().toString() : state.id();

        TypeSpec.Builder stateTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        stateTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state)));
        stateTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("desc", StateHelper.getStateDesc(stateElement, state)));
        stateTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("default_value", state.defaultValue()));
        stateTypeSpecBuilder.addField(this.getStaticFinalStringArrayFieldSpec("value_choices", state.valueChoices()));

        return stateTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Event}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @param event           {@link Event}
     * @return TypeSpec.Builder eventTypeSpecBuilder
     */
    private TypeSpec.Builder createEventTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element eventElement, Event event) {
        String simpleClassName = event.id().isEmpty() ? eventElement.getSimpleName().toString() : event.id();

        TypeSpec.Builder eventTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        eventTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event)));
        eventTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("name", EventHelper.getEventName(eventElement, event)));
        eventTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("format", event.format()));
        eventTypeSpecBuilder.addField(this.getStaticFinalStringArrayFieldSpec("value_choices", event.valueChoices()));

        return eventTypeSpecBuilder;
    }

    /**
     * Internal Get a Static Final String Field initialised with value
     *
     * @param fieldName String
     * @param value     String
     * @return FieldSpec fieldSpec
     */
    private FieldSpec getStaticFinalStringFieldSpec(String fieldName, String value) {
        return FieldSpec.builder(String.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$S", value).build();
    }

    /**
     * Internal Get a Static Final long Field initialised with value
     *
     * @param fieldName String
     * @param value     long
     * @return FieldSpec fieldSpec
     */
    private FieldSpec getStaticFinalDoubleFieldSpec(String fieldName, double value) {
        return FieldSpec.builder(double.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$L", value).build();
    }

    /**
     * Internal Get a Static Final long Field initialised with value
     *
     * @param fieldName String
     * @param value     long
     * @return FieldSpec fieldSpec
     */
    private FieldSpec getStaticFinalLongFieldSpec(String fieldName, long value) {
        return FieldSpec.builder(long.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$L", value).build();
    }

    /**
     * Internal Get a Static Final boolean Field initialised with value
     *
     * @param fieldName String
     * @param value     boolean
     * @return FieldSpec fieldSpec
     */
    private FieldSpec getStaticFinalBooleanFieldSpec(String fieldName, boolean value) {
        return FieldSpec.builder(boolean.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$L", value).build();
    }

    /**
     * Internal Get a Static Final boolean Field initialised with value
     *
     * @param fieldName String
     * @param values    String[]
     * @return FieldSpec fieldSpec
     */
    private FieldSpec getStaticFinalStringArrayFieldSpec(String fieldName, String[] values) {
        ArrayTypeName stringArray = ArrayTypeName.of(String.class);
        String literal = "{\"" + String.join("\",\"", values) + "\"}";
        return FieldSpec.builder(String[].class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("new $1T $2L", stringArray, literal).build();
    }
}
