package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.Category;
import com.christophecvb.touchportal.annotations.Plugin;
import com.christophecvb.touchportal.annotations.Setting;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
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

public class PluginProcessor {
    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Plugin}
     *
     * @param processor     {@link TouchPortalPluginAnnotationsProcessor}
     * @param roundEnv      RoundEnvironment
     * @param pluginElement Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; pluginPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     * @throws TPAnnotationException If an Annotation is misused
     */
    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, RoundEnvironment roundEnv, Element pluginElement) throws GenericHelper.TPTypeException, TPAnnotationException {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);

        TypeSpec.Builder pluginTypeSpecBuilder = SpecUtils.createPluginTypeSpecBuilder(pluginElement, plugin);

        JsonObject jsonPlugin = new JsonObject();
        jsonPlugin.addProperty(PluginHelper.SDK, PluginHelper.TOUCH_PORTAL_PLUGIN_VERSION);
        jsonPlugin.addProperty(PluginHelper.VERSION, plugin.version());
        jsonPlugin.addProperty(PluginHelper.NAME, PluginHelper.getPluginName(pluginElement, plugin));
        jsonPlugin.addProperty(PluginHelper.ID, PluginHelper.getPluginId(pluginElement));
        JsonObject jsonConfiguration = new JsonObject();
        jsonConfiguration.addProperty(PluginHelper.CONFIGURATION_COLOR_DARK, plugin.colorDark());
        jsonConfiguration.addProperty(PluginHelper.CONFIGURATION_COLOR_LIGHT, plugin.colorLight());
        jsonConfiguration.addProperty(PluginHelper.CONFIGURATION_PARENT_CATEGORY, plugin.parentCategory().getKey());
        jsonPlugin.add(PluginHelper.CONFIGURATION, jsonConfiguration);
        jsonPlugin.addProperty(PluginHelper.PLUGIN_START_COMMAND, PluginHelper.TP_JAVA + " -Dapple.awt.UIElement=true -jar ./" + pluginElement.getSimpleName() + ".jar " + PluginHelper.COMMAND_START);
        jsonPlugin.addProperty(PluginHelper.PLUGIN_START_COMMAND + PluginHelper.PLUGIN_START_COMMAND_SUFFIX_WIN, PluginHelper.TP_JAVA + " -jar ./" + pluginElement.getSimpleName() + ".jar " + PluginHelper.COMMAND_START);
        jsonPlugin.addProperty(PluginHelper.PLUGIN_START_COMMAND + PluginHelper.PLUGIN_START_COMMAND_SUFFIX_MACOS, PluginHelper.TP_JAVA + " -Dapple.awt.UIElement=true -jar ./" + pluginElement.getSimpleName() + ".jar " + PluginHelper.COMMAND_START);
        jsonPlugin.addProperty(PluginHelper.PLUGIN_START_COMMAND + PluginHelper.PLUGIN_START_COMMAND_SUFFIX_LINUX, PluginHelper.TP_JAVA + " -jar ./" + pluginElement.getSimpleName() + ".jar " + PluginHelper.COMMAND_START);

        TypeSpec.Builder settingsTypeSpecBuilder = TypeSpec.classBuilder("Settings").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        JsonArray jsonSettings = new JsonArray();
        Set<? extends Element> settingElements = roundEnv.getElementsAnnotatedWith(Setting.class);
        for (Element settingElement : settingElements) {
            Pair<JsonObject, TypeSpec.Builder> settingResult = SettingProcessor.process(processor, settingElement);
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
            Pair<JsonObject, TypeSpec.Builder> categoryResult = CategoryProcessor.process(processor, roundEnv, pluginElement, plugin, categoryElement);
            jsonCategories.add(categoryResult.first);
            pluginTypeSpecBuilder.addType(categoryResult.second.build());
        }
        if (jsonCategories.size() > 0) {
            jsonPlugin.add(PluginHelper.CATEGORIES, jsonCategories);
        }
        else {
            throw new TPAnnotationException.Builder(Category.class).isMissing(true).build();
        }

        return Pair.create(jsonPlugin, pluginTypeSpecBuilder);
    }
}
