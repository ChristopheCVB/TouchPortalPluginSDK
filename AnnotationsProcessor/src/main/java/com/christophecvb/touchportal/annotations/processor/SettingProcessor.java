package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.Setting;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.GenericHelper;
import com.christophecvb.touchportal.helpers.SettingHelper;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class SettingProcessor {
    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Setting}
     *
     * @param settingElement Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; statePair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     */
    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, Element settingElement) throws Exception {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process Setting: " + settingElement.getSimpleName());
        Setting setting = settingElement.getAnnotation(Setting.class);

        TypeSpec.Builder settingTypeSpecBuilder = SpecUtils.createSettingTypeSpecBuilder(settingElement, setting);

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
}
