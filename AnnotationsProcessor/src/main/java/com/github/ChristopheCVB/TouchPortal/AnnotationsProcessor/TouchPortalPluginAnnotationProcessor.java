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
import com.github.ChristopheCVB.TouchPortal.Helpers.*;
import com.google.auto.service.AutoService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
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
        // TODO: Add Category Annotation
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) {
            return false;
        }
        // TODO: Generate Constants Classes for Annotations
        this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".process");

        try {
            Element selectedPluginElement = null;
            JSONObject jsonPlugin = null;

            Set<? extends Element> plugins = env.getElementsAnnotatedWith(Plugin.class);
            if (plugins.size() != 1) {
                throw new Exception("You need 1(one) @Plugin Annotation, you have " + plugins.size());
            }
            for (Element pluginElement : plugins) {
                selectedPluginElement = pluginElement;
                jsonPlugin = this.processPlugin(env, pluginElement);
            }

            String actionFileName = "resources/" + PluginHelper.ENTRY_TP;
            FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", actionFileName, selectedPluginElement);
            Writer writer = actionFileObject.openWriter();
            writer.write(jsonPlugin.toString());
            writer.flush();
            writer.close();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }

        return true;
    }

    /**
     * Generates a JSONObject representing the {@link Plugin}
     *
     * @param pluginElement Element
     * @return JSONObject jsonPlugin
     * @throws JSONException jsonException
     */
    private JSONObject processPlugin(RoundEnvironment env, Element pluginElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);
        JSONObject jsonPlugin = new JSONObject();
        jsonPlugin.put(PluginHelper.SDK, 2);
        jsonPlugin.put(PluginHelper.VERSION, plugin.version());
        jsonPlugin.put(PluginHelper.NAME, PluginHelper.getPluginName(pluginElement, plugin));
        jsonPlugin.put(PluginHelper.ID, PluginHelper.getPluginId(pluginElement));
        JSONObject jsonConfiguration = new JSONObject();
        jsonConfiguration.put(PluginHelper.CONFIGURATION_COLOR_DARK, plugin.colorDark());
        jsonConfiguration.put(PluginHelper.CONFIGURATION_COLOR_LIGHT, plugin.colorLight());
        jsonPlugin.put(PluginHelper.CONFIGURATION, jsonConfiguration);
        jsonPlugin.put(PluginHelper.PLUGIN_START_COMMAND, "java -jar %TP_PLUGIN_FOLDER%" + pluginElement.getSimpleName() + "\\" + pluginElement.getSimpleName() + ".jar " + PluginHelper.COMMAND_START);

        JSONArray jsonCategories = new JSONArray();
        Set<? extends Element> categoryElements = env.getElementsAnnotatedWith(Category.class);
        for (Element categoryElement : categoryElements) {
            jsonCategories.put(this.processCategory(env, pluginElement, plugin, categoryElement));
        }
        jsonPlugin.put(PluginHelper.CATEGORIES, jsonCategories);

        return jsonPlugin;
    }

    /**
     * Generates a JSONObject representing the {@link Category}
     *
     * @param env             RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @return JSONObject jsonCategory
     * @throws JSONException   If JSONObject is malformed
     * @throws TPTypeException If a used type is not Supported
     */
    private JSONObject processCategory(RoundEnvironment env, Element pluginElement, Plugin plugin, Element categoryElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Category: " + categoryElement.getSimpleName());
        Category category = categoryElement.getAnnotation(Category.class);
        JSONObject jsonCategory = new JSONObject();

        jsonCategory.put(CategoryHelper.ID, CategoryHelper.getCategoryId(pluginElement, categoryElement, category));
        jsonCategory.put(CategoryHelper.NAME, CategoryHelper.getCategoryName(categoryElement, category));
        jsonCategory.put(CategoryHelper.IMAGE_PATH, "%TP_PLUGIN_FOLDER%" + pluginElement.getSimpleName() + "/" + category.imagePath());

        JSONArray jsonActions = new JSONArray();
        Set<? extends Element> actionElements = env.getElementsAnnotatedWith(Action.class);
        for (Element actionElement : actionElements) {
            Action action = actionElement.getAnnotation(Action.class);
            String categoryId = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();
            if (categoryId.equals(action.categoryId())) {
                jsonActions.put(this.processAction(env, pluginElement, plugin, categoryElement, category, actionElement));
            }
        }
        jsonCategory.put(CategoryHelper.ACTIONS, jsonActions);

        JSONArray jsonStates = new JSONArray();
        Set<? extends Element> stateElements = env.getElementsAnnotatedWith(State.class);
        for (Element stateElement : stateElements) {
            jsonStates.put(this.processState(env, pluginElement, plugin, categoryElement, category, stateElement));
        }
        jsonCategory.put(CategoryHelper.STATES, jsonStates);

        JSONArray jsonEvents = new JSONArray();
        Set<? extends Element> eventElements = env.getElementsAnnotatedWith(Event.class);
        for (Element eventElement : eventElements) {
            jsonEvents.put(this.processEvent(env, pluginElement, plugin, categoryElement, category, eventElement));
        }
        jsonCategory.put(CategoryHelper.EVENTS, jsonEvents);

        return jsonCategory;
    }

    /**
     * Generates a JSONObject representing the {@link Action}
     *
     * @param env             RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @return JSONObject jsonAction
     * @throws JSONException If JSONObject is malformed
     */
    private JSONObject processAction(RoundEnvironment env, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());

        Action action = actionElement.getAnnotation(Action.class);
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

        Set<? extends Element> dataElements = env.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            JSONArray jsonActionData = new JSONArray();
            if (actionElement.equals(enclosingElement)) {
                JSONObject jsonActionDataItem = this.processActionData(env, pluginElement, plugin, categoryElement, category, actionElement, action, jsonAction, dataElement);
                jsonActionData.put(jsonActionDataItem);
            }
            jsonAction.put(ActionHelper.DATA, jsonActionData);
        }

        return jsonAction;
    }

    /**
     * Generates a JSONObject representing the {@link State}
     *
     * @param env             RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @return JSONObject jsonState
     * @throws JSONException If JSONObject is malformed
     */
    private JSONObject processState(RoundEnvironment env, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element stateElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());

        State state = stateElement.getAnnotation(State.class);
        JSONObject jsonState = new JSONObject();
        jsonState.put(StateHelper.ID, StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state));
        String tpType = GenericHelper.getTouchPortalType(stateElement);
        jsonState.put(StateHelper.TYPE, tpType);
        jsonState.put(StateHelper.DESC, StateHelper.getStateDesc(stateElement, state));
        jsonState.put(StateHelper.DEFAULT, state.defaultValue());
        if (tpType.equals(StateHelper.TYPE_CHOICE)) {
            jsonState.put(StateHelper.VALUE_CHOICES, state.valueChoices());
        }

        return jsonState;
    }

    /**
     * Generates a JSONObject representing the {@link Event}
     *
     * @param env             RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @return JSONObject jsonEvent
     * @throws JSONException   If JSONObject is malformed
     * @throws TPTypeException If any used type is not Supported
     */
    private JSONObject processEvent(RoundEnvironment env, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element eventElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());

        Event event = eventElement.getAnnotation(Event.class);
        State state = eventElement.getAnnotation(State.class);
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

        return jsonEvent;
    }

    /**
     * Generates a JSONObject representing the {@link Data}
     *
     * @param env             RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @param jsonAction      JSONObject
     * @param dataElement     Element
     * @return JSONObject jsonData
     * @throws JSONException jsonException
     */
    private JSONObject processActionData(RoundEnvironment env, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement, Action action, JSONObject jsonAction, Element dataElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());

        Data data = dataElement.getAnnotation(Data.class);
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

        return jsonData;
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
