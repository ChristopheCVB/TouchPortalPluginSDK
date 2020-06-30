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

package com.github.ChristopheCVB.TouchPortal.AnnotationsProcessor;

import com.github.ChristopheCVB.TouchPortal.Annotations.*;
import com.github.ChristopheCVB.TouchPortal.AnnotationsProcessor.utils.Pair;
import com.github.ChristopheCVB.TouchPortal.Helpers.*;
import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import java.util.LinkedHashSet;
import java.util.Set;

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
        annotations.add(Action.class.getCanonicalName());
        annotations.add(Data.class.getCanonicalName());
        annotations.add(State.class.getCanonicalName());
        annotations.add(Event.class.getCanonicalName());
        annotations.add(Category.class.getCanonicalName());
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
    private Pair<JsonObject, TypeSpec.Builder> processPlugin(RoundEnvironment roundEnv, Element pluginElement) throws GenericHelper.TPTypeException {
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
        jsonPlugin.addProperty(PluginHelper.PLUGIN_START_COMMAND, "java -jar \"" + PluginHelper.TP_PLUGIN_FOLDER + pluginElement.getSimpleName() + "\\" + pluginElement.getSimpleName() + ".jar\" " + PluginHelper.COMMAND_START + " \" " + PluginHelper.TP_PLUGIN_FOLDER + " \"");

        JsonArray jsonCategories = new JsonArray();
        Set<? extends Element> categoryElements = roundEnv.getElementsAnnotatedWith(Category.class);
        for (Element categoryElement : categoryElements) {
            Pair<JsonObject, TypeSpec.Builder> categoryResult = this.processCategory(roundEnv, pluginElement, plugin, categoryElement);
            jsonCategories.add(categoryResult.first);
            pluginTypeSpecBuilder.addType(categoryResult.second.build());
        }
        jsonPlugin.add(PluginHelper.CATEGORIES, jsonCategories);

        return Pair.create(jsonPlugin, pluginTypeSpecBuilder);
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
    private Pair<JsonObject, TypeSpec.Builder> processCategory(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement) throws GenericHelper.TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Category: " + categoryElement.getSimpleName());
        Category category = categoryElement.getAnnotation(Category.class);

        TypeSpec.Builder categoryTypeSpecBuilder = this.createCategoryTypeSpecBuilder(pluginElement, categoryElement, category);

        JsonObject jsonCategory = new JsonObject();
        jsonCategory.addProperty(CategoryHelper.ID, CategoryHelper.getCategoryId(pluginElement, categoryElement, category));
        jsonCategory.addProperty(CategoryHelper.NAME, CategoryHelper.getCategoryName(categoryElement, category));
        jsonCategory.addProperty(CategoryHelper.IMAGE_PATH, PluginHelper.TP_PLUGIN_FOLDER + pluginElement.getSimpleName() + "/" + category.imagePath());

        TypeSpec.Builder actionsTypeSpecBuilder = TypeSpec.classBuilder("Actions");
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

        TypeSpec.Builder statesTypeSpecBuilder = TypeSpec.classBuilder("States");
        JsonArray jsonStates = new JsonArray();
        Set<? extends Element> stateElements = roundEnv.getElementsAnnotatedWith(State.class);
        for (Element stateElement : stateElements) {
            Pair<JsonObject, TypeSpec.Builder> stateResult = this.processState(roundEnv, pluginElement, plugin, categoryElement, category, stateElement);
            jsonStates.add(stateResult.first);
            statesTypeSpecBuilder.addType(stateResult.second.build());
        }
        categoryTypeSpecBuilder.addType(statesTypeSpecBuilder.build());
        jsonCategory.add(CategoryHelper.STATES, jsonStates);

        TypeSpec.Builder eventsTypeSpecBuilder = TypeSpec.classBuilder("Events");
        JsonArray jsonEvents = new JsonArray();
        Set<? extends Element> eventElements = roundEnv.getElementsAnnotatedWith(Event.class);
        for (Element eventElement : eventElements) {
            Pair<JsonObject, TypeSpec.Builder> eventResult = this.processEvent(roundEnv, pluginElement, plugin, categoryElement, category, eventElement);
            jsonEvents.add(eventResult.first);
            eventsTypeSpecBuilder.addType(eventResult.second.build());
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
    private Pair<JsonObject, TypeSpec.Builder> processAction(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement) throws GenericHelper.TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());
        Action action = actionElement.getAnnotation(Action.class);

        TypeSpec.Builder actionTypeSpecBuilder = this.createActionTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action);

        JsonObject jsonAction = new JsonObject();
        jsonAction.addProperty(ActionHelper.ID, ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action));
        jsonAction.addProperty(ActionHelper.NAME, ActionHelper.getActionName(actionElement, action));
        jsonAction.addProperty(ActionHelper.PREFIX, action.prefix());
        jsonAction.addProperty(ActionHelper.TYPE, action.type());
        jsonAction.addProperty(ActionHelper.DESCRIPTION, action.description());
        if (!action.format().isEmpty()) {
            jsonAction.addProperty(ActionHelper.TRY_INLINE, true);
            jsonAction.addProperty(ActionHelper.FORMAT, action.format());
        }

        JsonArray jsonActionData = new JsonArray();
        Set<? extends Element> dataElements = roundEnv.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            if (actionElement.equals(enclosingElement)) {
                Pair<JsonObject, TypeSpec.Builder> actionDataResult = this.processActionData(roundEnv, pluginElement, plugin, categoryElement, category, actionElement, action, jsonAction, dataElement);
                jsonActionData.add(actionDataResult.first);
                actionTypeSpecBuilder.addType(actionDataResult.second.build());
            }
        }
        jsonAction.add(ActionHelper.DATA, jsonActionData);

        return Pair.create(jsonAction, actionTypeSpecBuilder);
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
    private Pair<JsonObject, TypeSpec.Builder> processState(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element stateElement) throws GenericHelper.TPTypeException {
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
                stateValueChoices.add(new JsonPrimitive(valueChoice));
            }
            jsonState.add(StateHelper.VALUE_CHOICES, stateValueChoices);
        }
        else if (!desiredTPType.equals(StateHelper.TYPE_TEXT)) {
            throw new GenericHelper.TPTypeException.Builder(className, GenericHelper.TPTypeException.ForAnnotation.STATE, desiredTPType).build();
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
    private Pair<JsonObject, TypeSpec.Builder> processEvent(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element eventElement) throws GenericHelper.TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());
        State state = eventElement.getAnnotation(State.class);
        Event event = eventElement.getAnnotation(Event.class);

        TypeSpec.Builder eventTypeSpecBuilder = this.createEventTypeSpecBuilder(pluginElement, categoryElement, category, eventElement, event);

        String reference = eventElement.getEnclosingElement().getSimpleName() + "." + eventElement.getSimpleName();

        JsonObject jsonEvent = new JsonObject();
        jsonEvent.addProperty(EventHelper.ID, EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event));
        jsonEvent.addProperty(EventHelper.TYPE, EventHelper.TYPE_COMMUNICATE);
        jsonEvent.addProperty(EventHelper.NAME, EventHelper.getEventName(eventElement, event));
        jsonEvent.addProperty(EventHelper.FORMAT, event.format());
        String desiredTPType = GenericHelper.getTouchPortalType(reference, eventElement);
        jsonEvent.addProperty(EventHelper.VALUE_TYPE, desiredTPType);
        if (desiredTPType.equals(EventHelper.VALUE_TYPE_CHOICE)) {
            JsonArray stateValueChoices = new JsonArray();
            for (String valueChoice : state.valueChoices()) {
                stateValueChoices.add(new JsonPrimitive(valueChoice));
            }
            jsonEvent.add(EventHelper.VALUE_CHOICES, stateValueChoices);
            jsonEvent.addProperty(EventHelper.VALUE_STATE_ID, StateHelper.getStateId(pluginElement, categoryElement, category, eventElement, state));
        }
        else {
            throw new GenericHelper.TPTypeException.Builder(reference, GenericHelper.TPTypeException.ForAnnotation.EVENT, desiredTPType).build();
        }

        return Pair.create(jsonEvent, eventTypeSpecBuilder);
    }

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Data}
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
    private Pair<JsonObject, TypeSpec.Builder> processActionData(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement, Action action, JsonObject jsonAction, Element dataElement) throws GenericHelper.TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        TypeSpec.Builder actionDataTypeSpecBuilder = this.createActionDataTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action, dataElement, data);

        Element method = dataElement.getEnclosingElement();
        String className = method.getEnclosingElement().getSimpleName() + "." + method.getSimpleName() + "(" + dataElement.getSimpleName() + ")";

        JsonObject jsonData = new JsonObject();
        String dataId = DataHelper.getActionDataId(pluginElement, categoryElement, category, actionElement, action, dataElement, data);
        jsonData.addProperty(DataHelper.ID, dataId);
        String desiredTPType = GenericHelper.getTouchPortalType(className, dataElement);
        jsonData.addProperty(DataHelper.TYPE, desiredTPType);
        jsonData.addProperty(DataHelper.LABEL, DataHelper.getActionDataLabel(dataElement, data));
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
        // Specific properties
        switch (desiredTPType) {
            case GenericHelper.TP_TYPE_CHOICE:
                JsonArray dataValueChoices = new JsonArray();
                for (String valueChoice : data.valueChoices()) {
                    dataValueChoices.add(valueChoice);
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
        }
        if (!action.format().isEmpty()) {
            // Replace wildcards
            String rawFormat = jsonAction.get(ActionHelper.FORMAT).getAsString();
            jsonAction.addProperty(ActionHelper.FORMAT, rawFormat.replace("{$" + (data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id()) + "$}", "{$" + dataId + "$}"));
        }

        return Pair.create(jsonData, actionDataTypeSpecBuilder);
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

        pluginTypeSpecBuilder.addField(FieldSpec.builder(String.class, "ID").addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$S", PluginHelper.getPluginId(pluginElement)).build());

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

        TypeSpec.Builder categoryTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName));
        categoryTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        categoryTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", CategoryHelper.getCategoryId(pluginElement, categoryElement, category)));

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

        TypeSpec.Builder actionTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName));
        actionTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        actionTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action)));

        return actionTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Data}
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

        TypeSpec.Builder actionDataTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName));
        actionDataTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", DataHelper.getActionDataId(pluginElement, categoryElement, category, actionElement, action, dataElement, data)));

        return actionDataTypeSpecBuilder;
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

        TypeSpec.Builder stateTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName));
        stateTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state)));

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

        TypeSpec.Builder eventTypeSpecBuilder = TypeSpec.classBuilder(TouchPortalPluginAnnotationProcessor.capitalize(simpleClassName));
        eventTypeSpecBuilder.addField(this.getStaticFinalStringFieldSpec("id", EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event)));

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
}
