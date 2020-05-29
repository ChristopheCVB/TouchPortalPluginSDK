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
import com.google.auto.service.AutoService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".process");

        try {
            JSONObject jsonPlugin = null;
            Element selectedPluginElement = null;

            Set<? extends Element> plugins = env.getElementsAnnotatedWith(Plugin.class);
            if (plugins.size() != 1) {
                throw new Exception("You need 1(one) @Plugin Annotation, you have " + plugins.size());
            }
            for (Element pluginElement : plugins) {
                selectedPluginElement = pluginElement;
                jsonPlugin = this.processPlugin(pluginElement);
            }

            JSONObject jsonCategory = new JSONObject();
            Plugin plugin = selectedPluginElement.getAnnotation(Plugin.class);
            jsonCategory.put("id", TouchPortalPluginAnnotationProcessor.getCategoryId(selectedPluginElement));
            jsonCategory.put("name", TouchPortalPluginAnnotationProcessor.getPluginName(selectedPluginElement, plugin));
            jsonCategory.put("imagepath", "%TP_PLUGIN_FOLDER%" + selectedPluginElement.getSimpleName() + "/images/icon-24.png");

            JSONArray jsonCategories = new JSONArray();
            jsonCategories.put(jsonCategory);
            jsonPlugin.put("categories", jsonCategories);

            JSONArray jsonActions = new JSONArray();
            Set<? extends Element> actionElements = env.getElementsAnnotatedWith(Action.class);
            for (Element actionElement : actionElements) {
                jsonActions.put(this.processAction(actionElement, env));
            }
            jsonCategory.put("actions", jsonActions);

            JSONArray jsonStates = new JSONArray();
            Set<? extends Element> stateElements = env.getElementsAnnotatedWith(State.class);
            for (Element stateElement : stateElements) {
                jsonStates.put(this.processState(stateElement));
            }
            jsonCategory.put("states", jsonStates);

            JSONArray jsonEvents = new JSONArray();
            Set<? extends Element> eventElements = env.getElementsAnnotatedWith(Event.class);
            for (Element eventElement : eventElements) {
                jsonEvents.put(this.processEvent(eventElement, env));
            }
            jsonCategory.put("events", jsonEvents);

            String actionFileName = "resources/entry.tp";
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
    private JSONObject processPlugin(Element pluginElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);
        JSONObject jsonPlugin = new JSONObject();
        jsonPlugin.put("sdk", 2);
        jsonPlugin.put("version", plugin.version());
        jsonPlugin.put("name", getPluginName(pluginElement, plugin));
        jsonPlugin.put("id", TouchPortalPluginAnnotationProcessor.getPluginId(pluginElement));
        JSONObject jsonConfiguration = new JSONObject();
        jsonConfiguration.put("colorDark", plugin.colorDark());
        jsonConfiguration.put("colorLight", plugin.colorLight());
        jsonPlugin.put("configuration", jsonConfiguration);
        jsonPlugin.put("plugin_start_cmd", "java -jar %TP_PLUGIN_FOLDER%" + pluginElement.getSimpleName() + "\\" + pluginElement.getSimpleName() + ".jar start");

        return jsonPlugin;
    }

    /**
     * Generates a JSONObject representing the {@link Action}
     *
     * @param actionElement Element
     * @param env RoundEnvironment
     * @return JSONObject jsonAction
     * @throws JSONException jsonException
     */
    private JSONObject processAction(Element actionElement, RoundEnvironment env) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());

        Action action = actionElement.getAnnotation(Action.class);
        JSONObject jsonAction = new JSONObject();
        jsonAction.put("id", TouchPortalPluginAnnotationProcessor.getActionId(actionElement, action));
        jsonAction.put("name", TouchPortalPluginAnnotationProcessor.getActionName(actionElement, action));
        jsonAction.put("prefix", action.prefix());
        jsonAction.put("type", "communicate");
        jsonAction.put("description", action.description());
        if (action.tryInline()) {
            jsonAction.put("tryInline", action.tryInline());
            jsonAction.put("format", action.format());
        }

        Set<? extends Element> dataElements = env.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            JSONArray jsonActionDatas = new JSONArray();
            if (actionElement.equals(enclosingElement)) {
                JSONObject jsonActionData = this.processActionData(dataElement, action);
                jsonActionDatas.put(jsonActionData);
            }
            jsonAction.put("data", jsonActionDatas);
        }

        return jsonAction;
    }

    /**
     * Generates a JSONObject representing the {@link State}
     *
     * @param stateElement Element
     * @return JSONObject jsonState
     * @throws JSONException jsonException
     */
    private JSONObject processState(Element stateElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());

        State state = stateElement.getAnnotation(State.class);
        JSONObject jsonState = new JSONObject();
        jsonState.put("id", TouchPortalPluginAnnotationProcessor.getStateId(stateElement, state));
        String tpType = TouchPortalPluginAnnotationProcessor.getTouchPortalType(stateElement);
        jsonState.put("type", tpType);
        jsonState.put("desc", TouchPortalPluginAnnotationProcessor.getStateDesc(stateElement, state));
        jsonState.put("default", state.defaultValue());
        if (tpType.equals("choice")) {
            jsonState.put("valueChoices", state.valueChoices());
        }

        return jsonState;
    }

    /**
     * Generates a JSONObject representing the {@link Event}
     *
     * @param eventElement Element
     * @param env RoundEnvironment
     * @return JSONObject jsonEvent
     * @throws JSONException jsonException
     */
    private JSONObject processEvent(Element eventElement, RoundEnvironment env) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());

        Event event = eventElement.getAnnotation(Event.class);
        JSONObject jsonEvent = new JSONObject();
        jsonEvent.put("id", TouchPortalPluginAnnotationProcessor.getEventId(eventElement, event));
        jsonEvent.put("type", "communicate");
        jsonEvent.put("name", TouchPortalPluginAnnotationProcessor.getEventName(eventElement, event));
        jsonEvent.put("format", event.format());
        String tpType = TouchPortalPluginAnnotationProcessor.getTouchPortalType(eventElement);
        jsonEvent.put("valueType", tpType);
        if (tpType.equals("choice")) {
            Set<? extends Element> stateElements = env.getElementsAnnotatedWith(State.class);
            for (Element stateElement : stateElements) {
                if (event.stateFieldName().equals(stateElement.getSimpleName().toString())) {
                    State state = stateElement.getAnnotation(State.class);
                    jsonEvent.put("valueChoices", state.valueChoices());
                    jsonEvent.put("valueStateId", TouchPortalPluginAnnotationProcessor.getStateId(stateElement, state));
                }
            }
        }
        else {
            throw new Exception("The type " + tpType + " is not supported for events");
        }

        return jsonEvent;
    }

    /**
     * Generates a JSONObject representing the {@link Data}
     *
     * @param dataElement Element
     * @param action Action
     * @return JSONObject jsonData
     * @throws JSONException jsonException
     */
    private JSONObject processActionData(Element dataElement, Action action) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());

        Data data = dataElement.getAnnotation(Data.class);
        JSONObject jsonData = new JSONObject();
        jsonData.put("id", TouchPortalPluginAnnotationProcessor.getActionDataId(dataElement, data, action));
        String tpType = TouchPortalPluginAnnotationProcessor.getTouchPortalType(dataElement);
        jsonData.put("type", tpType);
        jsonData.put("label", TouchPortalPluginAnnotationProcessor.getActionDataLabel(dataElement, data));
        jsonData.put("default", data.defaultValue());
        if (tpType.equals("choice")) {
            jsonData.put("valueChoices", data.valueChoices());
        }

        return jsonData;
    }

    /**
     * Retrieve the internal Touch Portal type according to the Java's element type
     * @param element Element
     * @return String tpType
     */
    private static String getTouchPortalType(Element element) {
        String tpType;
        switch (element.asType().toString()) {
            case "byte":
            case "char":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "java.lang.Byte":
            case "java.lang.Char":
            case "java.lang.Short":
            case "java.lang.Integer":
            case "java.lang.Long":
            case "java.lang.Float":
            case "java.lang.Double":
                tpType = "Number";
                break;

            case "boolean":
            case "java.lang.Boolean":
                tpType = "switch";
                break;

            default:
                if (element.asType().toString().endsWith("[]")) {
                    tpType = "choice";
                }
                else {
                    tpType = "text";
                }
                break;
        }
        return tpType;
    }

    /**
     * Get the formatted Plugin ID
     *
     * @param element Element
     * @return String pluginId
     */
    private static String getPluginId(Element element) {
        return ((PackageElement) element.getEnclosingElement()).getQualifiedName() + "." + element.getSimpleName();
    }

    /**
     * Get the formatted Plugin Name
     *
     * @param element Element
     * @param plugin {@link Plugin}
     * @return String pluginName
     */
    private static String getPluginName(Element element, Plugin plugin) {
        return plugin.name().isEmpty() ? element.getSimpleName().toString() : plugin.name();
    }

    /**
     * Get the formatted Category ID
     *
     * @param element Element
     * @return String categoryId
     */
    private static String getCategoryId(Element element) {
        return TouchPortalPluginAnnotationProcessor.getPluginId(element) + ".basecategory";
    }

    /**
     * Get the formatted Action ID
     *
     * @param element Element
     * @param action {@link Action}
     * @return String actionId
     */
    private static String getActionId(Element element, Action action) {
        return TouchPortalPluginAnnotationProcessor.getCategoryId(element.getEnclosingElement()) + ".action." + (action.id().isEmpty() ? element.getSimpleName() : action.id());
    }

    /**
     * Get the formatted Action Name
     *
     * @param element Element
     * @param action {@link Action}
     * @return String actionName
     */
    private static String getActionName(Element element, Action action) {
        return action.name().isEmpty() ? element.getSimpleName().toString() : action.name();
    }

    /**
     * Get the formatted State ID
     *
     * @param element Element
     * @param state {@link State}
     * @return String stateId
     */
    private static String getStateId(Element element, State state) {
        return TouchPortalPluginAnnotationProcessor.getCategoryId(element.getEnclosingElement()) + ".state." + (state.id().isEmpty() ? element.getSimpleName() : state.id());
    }

    /**
     * Get the formatted State Desc
     *
     * @param element Element
     * @param state {@link State}
     * @return String stateDesc
     */
    private static String getStateDesc(Element element, State state) {
        return state.desc().isEmpty() ? element.getSimpleName().toString() : state.desc();
    }

    /**
     * Get the formatted Event ID
     *
     * @param element Element
     * @param event {@link Event}
     * @return String eventId
     */
    private static String getEventId(Element element, Event event) {
        return TouchPortalPluginAnnotationProcessor.getCategoryId(element.getEnclosingElement()) + ".event." + (event.id().isEmpty() ? element.getSimpleName() : event.id());
    }

    /**
     * Get the formatted Event Name
     *
     * @param element Element
     * @param event {@link Event}
     * @return String eventName
     */
    private static String getEventName(Element element, Event event) {
        return event.name().isEmpty() ? element.getSimpleName().toString() : event.name();
    }

    /**
     * Get the formatted Data Id
     *
     * @param element Element
     * @param data {@link Data}
     * @param action {@link Action}
     * @return String dataId
     */
    private static String getActionDataId(Element element, Data data, Action action) {
        return TouchPortalPluginAnnotationProcessor.getActionId(element.getEnclosingElement(), action) + ".data." + (data.id().isEmpty() ? element.getSimpleName() : data.id());
    }

    /**
     * Get the formatted Data Label
     *
     * @param dataElement Element
     * @param data {@link Data}
     * @return String dataLabel
     */
    private static String getActionDataLabel(Element dataElement, Data data) {
        return data.label().isEmpty() ? dataElement.getSimpleName().toString() : data.label();
    }
}
