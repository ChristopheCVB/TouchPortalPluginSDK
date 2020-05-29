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
            jsonCategory.put("id", this.getCategoryId(selectedPluginElement));
            jsonCategory.put("name", this.getPluginName(selectedPluginElement, plugin));
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

    private JSONObject processPlugin(Element pluginElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);
        JSONObject jsonPlugin = new JSONObject();
        jsonPlugin.put("sdk", 2);
        jsonPlugin.put("version", plugin.version());
        jsonPlugin.put("name", getPluginName(pluginElement, plugin));
        jsonPlugin.put("id", this.getPluginId(pluginElement));
        JSONObject jsonConfiguration = new JSONObject();
        jsonConfiguration.put("colorDark", plugin.colorDark());
        jsonConfiguration.put("colorLight", plugin.colorLight());
        jsonPlugin.put("configuration", jsonConfiguration);
        jsonPlugin.put("plugin_start_cmd", "java -jar %TP_PLUGIN_FOLDER%" + pluginElement.getSimpleName() + "\\" + pluginElement.getSimpleName() + ".jar start");

        return jsonPlugin;
    }

    private JSONObject processAction(Element actionElement, RoundEnvironment env) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());

        Action action = actionElement.getAnnotation(Action.class);
        JSONObject jsonAction = new JSONObject();
        jsonAction.put("id", this.getActionId(actionElement, action));
        jsonAction.put("name", this.getActionName(actionElement, action));
        jsonAction.put("prefix", action.prefix());
        jsonAction.put("type", "communicate");
        jsonAction.put("description", action.description());
        jsonAction.put("tryInline", action.tryInline());
        jsonAction.put("format", action.format());

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

    private JSONObject processState(Element stateElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());

        State state = stateElement.getAnnotation(State.class);
        JSONObject jsonState = new JSONObject();
        jsonState.put("id", this.getStateId(stateElement, state));
        String tpType = this.getTouchPortalType(stateElement);
        jsonState.put("type", tpType);
        jsonState.put("desc", this.getStateDesc(stateElement, state));
        jsonState.put("default", state.defaultValue());
        if (tpType.equals("choice")) {
            jsonState.put("valueChoices", state.valueChoices());
        }

        return jsonState;
    }

    private JSONObject processEvent(Element eventElement, RoundEnvironment env) throws Exception {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());

        Event event = eventElement.getAnnotation(Event.class);
        JSONObject jsonEvent = new JSONObject();
        jsonEvent.put("id", this.getEventId(eventElement, event));
        jsonEvent.put("type", "communicate");
        jsonEvent.put("name", this.getEventName(eventElement, event));
        jsonEvent.put("format", event.format());
        String tpType = this.getTouchPortalType(eventElement);
        jsonEvent.put("valueType", tpType);
        if (tpType.equals("choice")) {
            Set<? extends Element> stateElements = env.getElementsAnnotatedWith(State.class);
            for (Element stateElement : stateElements) {
                if (event.stateName().equals(stateElement.getSimpleName().toString())) {
                    State state = stateElement.getAnnotation(State.class);
                    jsonEvent.put("valueChoices", state.valueChoices());
                    jsonEvent.put("valueStateId", this.getStateId(stateElement, state));
                }
            }
        }
        else {
            throw new Exception("The type " + tpType + " is not supported for events");
        }

        return jsonEvent;
    }

    private JSONObject processActionData(Element dataElement, Action action) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());

        Data data = dataElement.getAnnotation(Data.class);
        JSONObject jsonData = new JSONObject();
        jsonData.put("id", this.getActionDataId(dataElement, data, action));
        String tpType = this.getTouchPortalType(dataElement);
        jsonData.put("type", tpType);
        jsonData.put("label", this.getActionDataLabel(dataElement, data));
        jsonData.put("default", data.defaultValue());
        if (tpType.equals("choice")) {
            jsonData.put("valueChoices", data.valueChoices());
        }

        return jsonData;
    }

    private String getTouchPortalType(Element stateElement) {
        String tpType;
        switch (stateElement.asType().toString()) {
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
                if (stateElement.asType().toString().endsWith("[]")) {
                    tpType = "choice";
                }
                else {
                    tpType = "text";
                }
                break;
        }
        return tpType;
    }

    private String getPluginId(Element element) {
        return ((PackageElement) element.getEnclosingElement()).getQualifiedName() + "." + element.getSimpleName();
    }

    private String getPluginName(Element selectedPluginElement, Plugin plugin) {
        return plugin.name().isEmpty() ? selectedPluginElement.getSimpleName().toString() : plugin.name();
    }

    private String getCategoryId(Element element) {
        return this.getPluginId(element) + ".basecategory";
    }

    private String getActionId(Element element, Action action) {
        return this.getCategoryId(element.getEnclosingElement()) + ".action." + (action.id().isEmpty() ? element.getSimpleName() : action.id());
    }

    private String getActionName(Element element, Action action) {
        return action.name().isEmpty() ? element.getSimpleName().toString() : action.name();
    }

    private String getStateId(Element element, State state) {
        return this.getCategoryId(element.getEnclosingElement()) + ".state." + (state.id().isEmpty() ? element.getSimpleName() : state.id());
    }

    private String getStateDesc(Element element, State state) {
        return state.desc().isEmpty() ? element.getSimpleName().toString() : state.desc();
    }

    private String getEventId(Element element, Event event) {
        return this.getCategoryId(element.getEnclosingElement()) + ".event." + (event.id().isEmpty() ? element.getSimpleName() : event.id());
    }

    private String getEventName(Element element, Event event) {
        return event.name().isEmpty() ? element.getSimpleName().toString() : event.name();
    }

    private String getActionDataId(Element element, Data data, Action action) {
        return this.getActionId(element.getEnclosingElement(), action) + ".data." + (data.id().isEmpty() ? element.getSimpleName() : data.id());
    }

    private String getActionDataLabel(Element dataElement, Data data) {
        return data.label().isEmpty() ? dataElement.getSimpleName().toString() : data.label();
    }
}
