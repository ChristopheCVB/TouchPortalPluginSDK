package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.ActionHelper;
import com.christophecvb.touchportal.helpers.GenericHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.Set;

public class ActionProcessor {
    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Action}
     *
     * @param processor       {@link TouchPortalPluginAnnotationsProcessor}
     * @param roundEnv        RoundEnvironment
     * @param pluginElement   Element
     * @param plugin          {@link Plugin}
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param actionElement   Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; actionPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element actionElement) throws GenericHelper.TPTypeException {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());
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
                Pair<JsonObject, TypeSpec.Builder> actionDataResult = DataProcessor.process(processor, roundEnv, pluginElement, plugin, categoryElement, category, actionElement, action, jsonAction, dataElement);
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
}
