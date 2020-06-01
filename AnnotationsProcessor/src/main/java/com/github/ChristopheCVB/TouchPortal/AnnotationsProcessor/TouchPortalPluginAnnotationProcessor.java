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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
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
        annotations.add(Category.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".process");

        try {
            Element selectedPluginElement = null;
            JSONObject jsonPlugin = null;

            Set<? extends Element> plugins = roundEnv.getElementsAnnotatedWith(Plugin.class);
            if (plugins.size() != 1) {
                throw new Exception("You need 1(one) @Plugin Annotation, you have " + plugins.size());
            }
            for (Element pluginElement : plugins) {
                selectedPluginElement = pluginElement;
                jsonPlugin = this.processPlugin(roundEnv, pluginElement);
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
    private JSONObject processPlugin(RoundEnvironment roundEnv, Element pluginElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);

        try {
            this.writePluginConstantsFile(pluginElement, plugin);
        }
        catch (IOException ignored) {
        }

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
        Set<? extends Element> categoryElements = roundEnv.getElementsAnnotatedWith(Category.class);
        for (Element categoryElement : categoryElements) {
            jsonCategories.put(this.processCategory(roundEnv, pluginElement, plugin, categoryElement));
        }
        jsonPlugin.put(PluginHelper.CATEGORIES, jsonCategories);

        return jsonPlugin;
    }

    /**
     * Generates a JSONObject representing the {@link Category}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @return JSONObject jsonCategory
     * @throws JSONException   If JSONObject is malformed
     * @throws TPTypeException If a used type is not Supported
     */
    private JSONObject processCategory(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Category: " + categoryElement.getSimpleName());
        Category category = categoryElement.getAnnotation(Category.class);

        try {
            this.writeCategoryConstantsFile(pluginElement, categoryElement, category);
        }
        catch (IOException ignored) {
        }

        JSONObject jsonCategory = new JSONObject();
        jsonCategory.put(CategoryHelper.ID, CategoryHelper.getCategoryId(pluginElement, categoryElement, category));
        jsonCategory.put(CategoryHelper.NAME, CategoryHelper.getCategoryName(categoryElement, category));
        jsonCategory.put(CategoryHelper.IMAGE_PATH, "%TP_PLUGIN_FOLDER%" + pluginElement.getSimpleName() + "/" + category.imagePath());

        JSONArray jsonActions = new JSONArray();
        Set<? extends Element> actionElements = roundEnv.getElementsAnnotatedWith(Action.class);
        for (Element actionElement : actionElements) {
            Action action = actionElement.getAnnotation(Action.class);
            String categoryId = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();
            if (categoryId.equals(action.categoryId())) {
                jsonActions.put(this.processAction(roundEnv, pluginElement, plugin, categoryElement, category, actionElement));
            }
        }
        jsonCategory.put(CategoryHelper.ACTIONS, jsonActions);

        JSONArray jsonStates = new JSONArray();
        Set<? extends Element> stateElements = roundEnv.getElementsAnnotatedWith(State.class);
        for (Element stateElement : stateElements) {
            jsonStates.put(this.processState(roundEnv, pluginElement, plugin, categoryElement, category, stateElement));
        }
        jsonCategory.put(CategoryHelper.STATES, jsonStates);

        JSONArray jsonEvents = new JSONArray();
        Set<? extends Element> eventElements = roundEnv.getElementsAnnotatedWith(Event.class);
        for (Element eventElement : eventElements) {
            jsonEvents.put(this.processEvent(roundEnv, pluginElement, plugin, categoryElement, category, eventElement));
        }
        jsonCategory.put(CategoryHelper.EVENTS, jsonEvents);

        return jsonCategory;
    }

    /**
     * Generates a JSONObject representing the {@link Action}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @return JSONObject jsonAction
     * @throws JSONException If JSONObject is malformed
     */
    private JSONObject processAction(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());
        Action action = actionElement.getAnnotation(Action.class);

        try {
            this.writeActionConstantsFile(pluginElement, categoryElement, category, actionElement, action);
        }
        catch (IOException ignored) {
        }

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

        Set<? extends Element> dataElements = roundEnv.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            JSONArray jsonActionData = new JSONArray();
            if (actionElement.equals(enclosingElement)) {
                JSONObject jsonActionDataItem = this.processActionData(roundEnv, pluginElement, plugin, categoryElement, category, actionElement, action, jsonAction, dataElement);
                jsonActionData.put(jsonActionDataItem);
            }
            jsonAction.put(ActionHelper.DATA, jsonActionData);
        }

        return jsonAction;
    }

    /**
     * Generates a JSONObject representing the {@link State}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @return JSONObject jsonState
     * @throws JSONException If JSONObject is malformed
     */
    private JSONObject processState(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element stateElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process State: " + stateElement.getSimpleName());
        State state = stateElement.getAnnotation(State.class);

        try {
            this.writeStateConstantsFile(pluginElement, categoryElement, category, stateElement, state);
        }
        catch (IOException ignored) {
        }

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
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @return JSONObject jsonEvent
     * @throws JSONException   If JSONObject is malformed
     * @throws TPTypeException If any used type is not Supported
     */
    private JSONObject processEvent(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element eventElement) throws JSONException, TPTypeException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Event: " + eventElement.getSimpleName());
        State state = eventElement.getAnnotation(State.class);
        Event event = eventElement.getAnnotation(Event.class);

        try {
            this.writeEventConstantsFile(pluginElement, categoryElement, category, eventElement, event);
        }
        catch (IOException ignored) {
        }

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
     * @param roundEnv        RoundEnvironment
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
    private JSONObject processActionData(RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement, Action action, JSONObject jsonAction, Element dataElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action Data: " + dataElement.getSimpleName());
        Data data = dataElement.getAnnotation(Data.class);

        try {
            this.writeActionDataConstantsFile(pluginElement, categoryElement, category, actionElement, action, dataElement, data);
        }
        catch (IOException ignored) {
        }

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
     * Generates a Java Class with Constants for the {@link Plugin}
     *
     * @param pluginElement Element
     * @param plugin        {@link Plugin}
     * @throws IOException ioException
     */
    private void writePluginConstantsFile(Element pluginElement, Plugin plugin) throws IOException {
        String simpleClassName = pluginElement.getSimpleName().toString() + "Constants";
        String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(simpleClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            writePackage(packageName, out);

            this.writeJavaClassHeader(simpleClassName, out);

            this.writeConstant(out, "id", PluginHelper.getPluginId(pluginElement));

            out.println("}");
        }
    }

    /**
     * Generates a Java Class with Constants for the {@link Category}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @throws IOException ioException
     */
    private void writeCategoryConstantsFile(Element pluginElement, Element categoryElement, Category category) throws IOException {
        String simpleClassName = pluginElement.getSimpleName().toString() + categoryElement.getSimpleName().toString() + "Constants";
        String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(simpleClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            writePackage(packageName, out);

            this.writeJavaClassHeader(simpleClassName, out);

            this.writeConstant(out, "id", CategoryHelper.getCategoryId(pluginElement, categoryElement, category));

            out.println("}");
        }
    }

    /**
     * Generates a Java Class with Constants for the {@link Action}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @throws IOException ioException
     */
    private void writeActionConstantsFile(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action) throws IOException {
        String simpleClassName = pluginElement.getSimpleName().toString() + categoryElement.getSimpleName().toString() + actionElement.getSimpleName().toString() + "Constants";
        String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(simpleClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            writePackage(packageName, out);

            this.writeJavaClassHeader(simpleClassName, out);

            this.writeConstant(out, "id", ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action));

            out.println("}");
        }
    }

    /**
     * Generates a Java Class with Constants for the {@link Data}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @param action          {@link Action}
     * @param dataElement     Element
     * @param data            {@link Data}
     * @throws IOException ioException
     */
    private void writeActionDataConstantsFile(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action, Element dataElement, Data data) throws IOException {
        String simpleClassName = pluginElement.getSimpleName().toString() + categoryElement.getSimpleName().toString() + actionElement.getSimpleName().toString() + dataElement.getSimpleName().toString() + "Constants";
        String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(simpleClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            writePackage(packageName, out);

            this.writeJavaClassHeader(simpleClassName, out);

            this.writeConstant(out, "id", DataHelper.getActionDataId(pluginElement, categoryElement, category, actionElement, action, dataElement, data));

            out.println("}");
        }
    }

    /**
     * Generates a Java Class with Constants for the {@link State}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param stateElement    Element
     * @param state           {@link State}
     * @throws IOException ioException
     */
    private void writeStateConstantsFile(Element pluginElement, Element categoryElement, Category category, Element stateElement, State state) throws IOException {
        String simpleClassName = pluginElement.getSimpleName().toString() + categoryElement.getSimpleName().toString() + stateElement.getSimpleName() + "Constants";
        String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(simpleClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            writePackage(packageName, out);

            this.writeJavaClassHeader(simpleClassName, out);

            this.writeConstant(out, "id", StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state));

            out.println("}");
        }
    }

    /**
     * Generates a Java Class with Constants for the {@link Event}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param eventElement    Element
     * @param event           {@link Event}
     * @throws IOException ioException
     */
    private void writeEventConstantsFile(Element pluginElement, Element categoryElement, Category category, Element eventElement, Event event) throws IOException {
        String simpleClassName = pluginElement.getSimpleName().toString() + categoryElement.getSimpleName().toString() + eventElement.getSimpleName() + "EventConstants";
        String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(simpleClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            writePackage(packageName, out);

            this.writeJavaClassHeader(simpleClassName, out);

            this.writeConstant(out, "id", EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event));

            out.println("}");
        }
    }

    /**
     * Internal Write the Java Class Header
     *
     * @param simpleClassName String
     * @param out             PrintWriter
     */
    private void writeJavaClassHeader(String simpleClassName, PrintWriter out) {
        out.print("public class ");
        out.print(simpleClassName);
        out.println(" {");
    }

    /**
     * Internal Write the Java Class Package
     *
     * @param packageName String
     * @param out         PrintWriter
     */
    private void writePackage(String packageName, PrintWriter out) {
        if (packageName != null) {
            out.print("package ");
            out.print(packageName);
            out.println(";");
            out.println();
        }
    }

    /**
     * Internal Write a Static Final Constant
     *
     * @param out       PrintWriter
     * @param fieldName String
     * @param value     String
     */
    private void writeConstant(PrintWriter out, String fieldName, String value) {
        out.print("  public static final String ");
        out.print(fieldName.toUpperCase());
        out.print(" = \"");
        out.print(value);
        out.print("\";");
        out.println();
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
