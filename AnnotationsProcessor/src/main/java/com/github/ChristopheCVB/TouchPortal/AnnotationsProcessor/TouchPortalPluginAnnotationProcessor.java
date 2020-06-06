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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                Pair<JSONObject, TypeSpec.Builder> pluginPair = this.processPlugin(roundEnv, pluginElement);

                String actionFileName = "resources/" + PluginHelper.ENTRY_TP;
                FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", actionFileName, pluginElement);
                Writer writer = actionFileObject.openWriter();
                writer.write(pluginPair.first.toString(2));
                writer.flush();
                writer.close();

                TypeSpec pluginTypeSpec = pluginPair.second.build();
                String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();
                JavaFile javaConstantsFile = JavaFile.builder(packageName, pluginTypeSpec).build();
                javaConstantsFile.writeTo(this.filer);
            }

        }
        catch (Exception exception) {
            exception.printStackTrace();
        }

        return true;
    }

    /**
     * Generates a JSONObject and a TypeSpec.Builder representing the {@link Plugin}
     *
     * @param roundEnv      RoundEnvironment
     * @param pluginElement Element
     * @return Pair<JSONObject, TypeSpec.Builder> pluginPair
     * @throws JSONException jsonException
     */
    private Pair<JSONObject, TypeSpec.Builder> processPlugin(RoundEnvironment roundEnv, Element pluginElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);

        TypeSpec.Builder pluginTypeSpecBuilder = this.createPluginTypeSpecBuilder(pluginElement, plugin);

        JSONObject jsonPlugin = new JSONObject();
        jsonPlugin.put(PluginHelper.SDK, PluginHelper.TOUCH_PORTAL_PLUGIN_VERSION);
        jsonPlugin.put(PluginHelper.VERSION, plugin.version());
        jsonPlugin.put(PluginHelper.NAME, PluginHelper.getPluginName(pluginElement, plugin));
        jsonPlugin.put(PluginHelper.ID, PluginHelper.getPluginId(pluginElement));
        JSONObject jsonConfiguration = new JSONObject();
        jsonConfiguration.put(PluginHelper.CONFIGURATION_COLOR_DARK, plugin.colorDark());
        jsonConfiguration.put(PluginHelper.CONFIGURATION_COLOR_LIGHT, plugin.colorLight());
        jsonPlugin.put(PluginHelper.CONFIGURATION, jsonConfiguration);
        jsonPlugin.put(PluginHelper.PLUGIN_START_COMMAND, "java -jar " + PluginHelper.TP_PLUGIN_FOLDER + pluginElement.getSimpleName() + "\\" + pluginElement.getSimpleName() + ".jar " + PluginHelper.COMMAND_START + " \" " + PluginHelper.TP_PLUGIN_FOLDER + " \"");

        JSONArray jsonCategories = new JSONArray();
        Set<? extends Element> categoryElements = roundEnv.getElementsAnnotatedWith(Category.class);
        for (Element categoryElement : categoryElements) {
            Pair<JSONObject, TypeSpec.Builder> categoryResult = this.processCategory(roundEnv, pluginElement, plugin, categoryElement);
            jsonCategories.put(categoryResult.first);
            pluginTypeSpecBuilder.addType(categoryResult.second.build());
        }
        jsonPlugin.put(PluginHelper.CATEGORIES, jsonCategories);

        return Pair.create(jsonPlugin, pluginTypeSpecBuilder);
    }

    /**
     * Generates a JSONObject and a TypeSpec.Builder representing the {@link Category}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @return Pair<JSONObject, TypeSpec.Builder> categoryPair
     * @throws JSONException   If JSONObject is malformed
     * @throws TPTypeException If a used type is not Supported
     */
    private Pair<JSONObject, TypeSpec.Builder> processCategory(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Category: " + categoryElement.getSimpleName());
        Category category = categoryElement.getAnnotation(Category.class);

        TypeSpec.Builder categoryTypeSpecBuilder = this.createCategoryTypeSpecBuilder(pluginElement, categoryElement, category);

        JSONObject jsonCategory = new JSONObject();
        jsonCategory.put(CategoryHelper.ID, CategoryHelper.getCategoryId(pluginElement, categoryElement, category));
        jsonCategory.put(CategoryHelper.NAME, CategoryHelper.getCategoryName(categoryElement, category));
        jsonCategory.put(CategoryHelper.IMAGE_PATH, PluginHelper.TP_PLUGIN_FOLDER + pluginElement.getSimpleName() + "/" + category.imagePath());

        TypeSpec.Builder actionsTypeSpecBuilder = TypeSpec.classBuilder("Actions");
        JSONArray jsonActions = new JSONArray();
        Set<? extends Element> actionElements = roundEnv.getElementsAnnotatedWith(Action.class);
        for (Element actionElement : actionElements) {
            Action action = actionElement.getAnnotation(Action.class);
            String categoryId = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();
            if (categoryId.equals(action.categoryId())) {
                Pair<JSONObject, TypeSpec.Builder> actionResult = this.processAction(roundEnv, pluginElement, plugin, categoryElement, category, actionElement);
                jsonActions.put(actionResult.first);
                actionsTypeSpecBuilder.addType(actionResult.second.build());
            }
        }
        categoryTypeSpecBuilder.addType(actionsTypeSpecBuilder.build());
        jsonCategory.put(CategoryHelper.ACTIONS, jsonActions);

        TypeSpec.Builder statesTypeSpecBuilder = TypeSpec.classBuilder("States");
        JSONArray jsonStates = new JSONArray();
        Set<? extends Element> stateElements = roundEnv.getElementsAnnotatedWith(State.class);
        for (Element stateElement : stateElements) {
            Pair<JSONObject, TypeSpec.Builder> stateResult = this.processState(roundEnv, pluginElement, plugin, categoryElement, category, stateElement);
            jsonStates.put(stateResult.first);
            statesTypeSpecBuilder.addType(stateResult.second.build());
        }
        categoryTypeSpecBuilder.addType(statesTypeSpecBuilder.build());
        jsonCategory.put(CategoryHelper.STATES, jsonStates);

        TypeSpec.Builder eventsTypeSpecBuilder = TypeSpec.classBuilder("Events");
        JSONArray jsonEvents = new JSONArray();
        Set<? extends Element> eventElements = roundEnv.getElementsAnnotatedWith(Event.class);
        for (Element eventElement : eventElements) {
            Pair<JSONObject, TypeSpec.Builder> eventResult = this.processEvent(roundEnv, pluginElement, plugin, categoryElement, category, eventElement);
            jsonEvents.put(eventResult.first);
            eventsTypeSpecBuilder.addType(eventResult.second.build());
        }
        categoryTypeSpecBuilder.addType(eventsTypeSpecBuilder.build());
        jsonCategory.put(CategoryHelper.EVENTS, jsonEvents);

        return Pair.create(jsonCategory, categoryTypeSpecBuilder);
    }

    /**
     * Generates a JSONObject and a TypeSpec.Builder representing the {@link Action}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @return Pair<JSONObject, TypeSpec.Builder> actionPair
     * @throws JSONException If JSONObject is malformed
     */
    private Pair<JSONObject, TypeSpec.Builder> processAction(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());
        Action action = actionElement.getAnnotation(Action.class);

        TypeSpec.Builder actionTypeSpecBuilder = this.createActionTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action);

        JSONObject jsonAction = new JSONObject();
        jsonAction.put(ActionHelper.ID, ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action));
        jsonAction.put(ActionHelper.NAME, ActionHelper.getActionName(actionElement, action));
        jsonAction.put(ActionHelper.PREFIX, action.prefix());
        jsonAction.put(ActionHelper.TYPE, action.type());
        jsonAction.put(ActionHelper.DESCRIPTION, action.description());
        if (!action.format().isEmpty()) {
            jsonAction.put(ActionHelper.TRY_INLINE, true);
            jsonAction.put(ActionHelper.FORMAT, action.format());
        }

        JSONArray jsonActionData = new JSONArray();
        Set<? extends Element> dataElements = roundEnv.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            if (actionElement.equals(enclosingElement)) {
                Pair<JSONObject, TypeSpec.Builder> actionDataResult = this.processActionData(roundEnv, pluginElement, plugin, categoryElement, category, actionElement, action, jsonAction, dataElement);
                jsonActionData.put(actionDataResult.first);
                actionTypeSpecBuilder.addType(actionDataResult.second.build());
            }
        }
        jsonAction.put(ActionHelper.DATA, jsonActionData);

        return Pair.create(jsonAction, actionTypeSpecBuilder);
    }

    /**
     * Generates a JSONObject and a TypeSpec.Builder representing the {@link State}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @return Pair<JSONObject, TypeSpec.Builder> statePair
     * @throws JSONException If JSONObject is malformed
     */
    private Pair<JSONObject, TypeSpec.Builder> processState(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element stateElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());
        State state = stateElement.getAnnotation(State.class);

        TypeSpec.Builder stateTypeSpecBuilder = this.createStateTypeSpecBuilder(pluginElement, categoryElement, category, stateElement, state);

        JSONObject jsonState = new JSONObject();
        jsonState.put(StateHelper.ID, StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state));
        String tpType = GenericHelper.getTouchPortalType(stateElement);
        jsonState.put(StateHelper.TYPE, tpType);
        jsonState.put(StateHelper.DESC, StateHelper.getStateDesc(stateElement, state));
        jsonState.put(StateHelper.DEFAULT, state.defaultValue());
        if (tpType.equals(StateHelper.TYPE_CHOICE)) {
            jsonState.put(StateHelper.VALUE_CHOICES, state.valueChoices());
        }
        else if (!tpType.equals(StateHelper.TYPE_TEXT)) {
            throw new TPTypeException("The type '" + tpType + "' is not supported for states, only '" + StateHelper.TYPE_CHOICE + "' and '" + StateHelper.TYPE_TEXT + "' are.");
        }

        return Pair.create(jsonState, stateTypeSpecBuilder);
    }

    /**
     * Generates a JSONObject and a TypeSpec.Builder representing the {@link Event}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @return Pair<JSONObject, TypeSpec.Builder> eventPair
     * @throws JSONException   If JSONObject is malformed
     * @throws TPTypeException If any used type is not Supported
     */
    private Pair<JSONObject, TypeSpec.Builder> processEvent(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element eventElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());
        State state = eventElement.getAnnotation(State.class);
        Event event = eventElement.getAnnotation(Event.class);

        TypeSpec.Builder eventTypeSpecBuilder = this.createEventTypeSpecBuilder(pluginElement, categoryElement, category, eventElement, event);

        JSONObject jsonEvent = new JSONObject();
        jsonEvent.put(EventHelper.ID, EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event));
        jsonEvent.put(EventHelper.TYPE, EventHelper.TYPE_COMMUNICATE);
        jsonEvent.put(EventHelper.NAME, EventHelper.getEventName(eventElement, event));
        jsonEvent.put(EventHelper.FORMAT, event.format());
        String tpType = GenericHelper.getTouchPortalType(eventElement);
        jsonEvent.put(EventHelper.VALUE_TYPE, tpType);
        if (tpType.equals(EventHelper.VALUE_TYPE_CHOICE)) {
            jsonEvent.put(EventHelper.VALUE_CHOICES, state.valueChoices());
            jsonEvent.put(EventHelper.VALUE_STATE_ID, StateHelper.getStateId(pluginElement, categoryElement, category, eventElement, state));
        }
        else {
            throw new TPTypeException("The type '" + tpType + "' is not supported for events, only '" + EventHelper.VALUE_TYPE_CHOICE + "' is.");
        }

        return Pair.create(jsonEvent, eventTypeSpecBuilder);
    }

    /**
     * Generates a JSONObject and a TypeSpec.Builder representing the {@link Data}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @param jsonAction      JSONObject
     * @param dataElement     Element
     * @return Pair<JSONObject, TypeSpec.Builder> dataPair
     * @throws JSONException jsonException
     */
    private Pair<JSONObject, TypeSpec.Builder> processActionData(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement, Action action, JSONObject jsonAction, Element dataElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        TypeSpec.Builder actionDataTypeSpecBuilder = this.createActionDataTypeSpecBuilder(pluginElement, categoryElement, category, actionElement, action, dataElement, data);

        JSONObject jsonData = new JSONObject();
        String dataId = DataHelper.getActionDataId(pluginElement, categoryElement, category, actionElement, action, dataElement, data);
        jsonData.put(DataHelper.ID, dataId);
        String tpType = GenericHelper.getTouchPortalType(dataElement);
        jsonData.put(DataHelper.TYPE, tpType);
        jsonData.put(DataHelper.LABEL, DataHelper.getActionDataLabel(dataElement, data));
        jsonData.put(DataHelper.DEFAULT, data.defaultValue());
        if (tpType.equals(DataHelper.TYPE_CHOICE)) {
            jsonData.put(DataHelper.VALUE_CHOICES, data.valueChoices());
        }
        if (!action.format().isEmpty()) {
            String rawFormat = jsonAction.getString(ActionHelper.FORMAT);
            jsonAction.put(ActionHelper.FORMAT, rawFormat.replace("{$" + (data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id()) + "$}", "{$" + dataId + "$}"));
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

    /**
     * Touch Portal Type Exception
     */
    private static class TPTypeException extends Exception {
        /**
         * Constructor
         *
         * @param message String
         */
        public TPTypeException(String message) {
            super(message);
        }
    }
}
