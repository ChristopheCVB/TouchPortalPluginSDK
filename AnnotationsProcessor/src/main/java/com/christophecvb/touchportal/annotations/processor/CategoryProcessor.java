package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.CategoryHelper;
import com.christophecvb.touchportal.helpers.GenericHelper;
import com.christophecvb.touchportal.helpers.PluginHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.util.Set;

public class CategoryProcessor {
    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Category}
     *
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; categoryPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     * @throws TPAnnotationException If an Annotation is misused
     */
    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement) throws GenericHelper.TPTypeException, TPAnnotationException {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process Category: " + categoryElement.getSimpleName());
        Category category = categoryElement.getAnnotation(Category.class);

        TypeSpec.Builder categoryTypeSpecBuilder = SpecUtils.createCategoryTypeSpecBuilder(pluginElement, categoryElement, category);

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
                Pair<JsonObject, TypeSpec.Builder> actionResult = processor.processAction(roundEnv, pluginElement, plugin, categoryElement, category, actionElement);
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
                Pair<JsonObject, TypeSpec.Builder> connectorResult = processor.processConnector(roundEnv, pluginElement, plugin, categoryElement, category, connectorElement);
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
                Pair<JsonObject, TypeSpec.Builder> stateResult = processor.processState(roundEnv, pluginElement, plugin, categoryElement, category, stateElement);
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
                    Pair<JsonObject, TypeSpec.Builder> eventResult = processor.processEvent(roundEnv, pluginElement, plugin, categoryElement, category, eventElement);
                    jsonEvents.add(eventResult.first);
                    eventsTypeSpecBuilder.addType(eventResult.second.build());
                }
            }
            else {
                throw new TPAnnotationException.Builder(State.class).isMissing(true).forElement(eventElement).build();
            }
        }
        categoryTypeSpecBuilder.addType(eventsTypeSpecBuilder.build());
        jsonCategory.add(CategoryHelper.EVENTS, jsonEvents);

        return Pair.create(jsonCategory, categoryTypeSpecBuilder);
    }
}
