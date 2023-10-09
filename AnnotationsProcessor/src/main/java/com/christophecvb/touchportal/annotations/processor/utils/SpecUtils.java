package com.christophecvb.touchportal.annotations.processor.utils;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.helpers.*;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

public class SpecUtils {
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Plugin}
     *
     * @param pluginElement Element
     * @param plugin        {@link Plugin}
     * @return TypeSpec.Builder pluginTypeSpecBuilder
     */
    public static TypeSpec.Builder createPluginTypeSpecBuilder(Element pluginElement, Plugin plugin) {
        String simpleClassName = pluginElement.getSimpleName().toString() + "Constants";

        TypeSpec.Builder pluginTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName));
        pluginTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        pluginTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", PluginHelper.getPluginId(pluginElement)));
        pluginTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("name", plugin.name()));
        pluginTypeSpecBuilder.addField(SpecUtils.getStaticFinalLongFieldSpec("version", plugin.version()));

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
    public static TypeSpec.Builder createCategoryTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category) {
        String simpleClassName = category.id().isEmpty() ? categoryElement.getSimpleName().toString() : category.id();

        TypeSpec.Builder categoryTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        categoryTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        categoryTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", CategoryHelper.getCategoryId(pluginElement, categoryElement, category)));
        categoryTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("name", category.name()));
        categoryTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("image_path", category.imagePath()));

        return categoryTypeSpecBuilder;
    }


    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Category.SubCategory}
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param subCategory     {@link Category.SubCategory}
     * @return TypeSpec.Builder subCategoryTypeSpecBuilder
     */
    public static TypeSpec.Builder createSubCategoryTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Category.SubCategory subCategory) {
        TypeSpec.Builder subCategoryTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(subCategory.id())).addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        subCategoryTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", SubCategoryHelper.getSubCategoryId(pluginElement, categoryElement, category, subCategory)));
        subCategoryTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("name", subCategory.name()));
        subCategoryTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("imagepath", subCategory.imagePath()));

        return subCategoryTypeSpecBuilder;
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
    public static TypeSpec.Builder createActionTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action) {
        String simpleClassName = action.id().isEmpty() ? actionElement.getSimpleName().toString() : action.id();

        TypeSpec.Builder actionTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        actionTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        if (!action.subCategoryId().isEmpty()) {
            actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", ActionHelper.getActionId(pluginElement, categoryElement, category, action.subCategoryId(), actionElement, action)));
            actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("sub_category_id", SubCategoryHelper.getSubCategoryId(pluginElement, categoryElement, category, action.subCategoryId())));
        } else {
            actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", ActionHelper.getActionId(pluginElement, categoryElement, category, actionElement, action)));
        }
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("name", ActionHelper.getActionName(actionElement, action)));
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("prefix", action.prefix()));
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("description", action.description()));
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("type", action.type()));
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("format", action.format()));
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalBooleanFieldSpec("has_hold_functionality", action.hasHoldFunctionality()));

        return actionTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Action}
     *
     * @param pluginElement     Element
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @param connector         {@link Action}
     * @return TypeSpec.Builder actionTypeSpecBuilder
     */
    public static TypeSpec.Builder createConnectorTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element connectorElement, Connector connector) {
        String simpleClassName = connector.id().isEmpty() ? connectorElement.getSimpleName().toString() : connector.id();

        TypeSpec.Builder actionTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        actionTypeSpecBuilder.addModifiers(Modifier.PUBLIC);

        if (!connector.subCategoryId().isEmpty()) {
            actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", ConnectorHelper.getConnectorId(pluginElement, categoryElement, category, connector.subCategoryId(), connectorElement, connector)));
            actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("sub_category_id", SubCategoryHelper.getSubCategoryId(pluginElement, categoryElement, category, connector.subCategoryId())));
        } else {
            actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", ConnectorHelper.getConnectorId(pluginElement, categoryElement, category, connectorElement, connector)));
        }
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("name", ConnectorHelper.getConnectorName(connectorElement, connector)));
        actionTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("format", connector.format()));

        return actionTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Data} for an {@link Action}
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
    public static TypeSpec.Builder createActionDataTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element actionElement, Action action, Element dataElement, Data data) {
        String simpleClassName = data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id();

        TypeSpec.Builder actionDataTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", DataHelper.getActionDataId(pluginElement, categoryElement, category, actionElement, action, dataElement, data)));
        actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("label", DataHelper.getDataLabel(dataElement, data)));
        actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("default_value", data.defaultValue()));
        actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringArrayFieldSpec("value_choices", data.valueChoices()));
        actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringArrayFieldSpec("extensions", data.extensions()));
        actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalBooleanFieldSpec("is_directory", data.isDirectory()));
        actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalBooleanFieldSpec("is_color", data.isColor()));
        if (data.minValue() > Double.NEGATIVE_INFINITY) {
            actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalDoubleFieldSpec("min_value", data.minValue()));
        }
        if (data.maxValue() < Double.POSITIVE_INFINITY) {
            actionDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalDoubleFieldSpec("max_value", data.maxValue()));
        }

        return actionDataTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Data} for a {@link Connector}
     *
     * @param pluginElement     Element
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @param connector         {@link Connector}
     * @param dataElement       Element
     * @param data              {@link Data}
     * @return TypeSpec.Builder dataTypeSpecBuilder
     */
    public static TypeSpec.Builder createConnectorDataTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element connectorElement, Connector connector, Element dataElement, Data data) {
        String simpleClassName = data.id().isEmpty() ? dataElement.getSimpleName().toString() : data.id();

        TypeSpec.Builder connectorDataTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", DataHelper.getConnectorDataId(pluginElement, categoryElement, category, connectorElement, connector, dataElement, data)));
        connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("label", DataHelper.getDataLabel(dataElement, data)));
        connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("default_value", data.defaultValue()));
        connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringArrayFieldSpec("value_choices", data.valueChoices()));
        connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringArrayFieldSpec("extensions", data.extensions()));
        connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalBooleanFieldSpec("is_directory", data.isDirectory()));
        connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalBooleanFieldSpec("is_color", data.isColor()));
        if (data.minValue() > Double.NEGATIVE_INFINITY) {
            connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalDoubleFieldSpec("min_value", data.minValue()));
        }
        if (data.maxValue() < Double.POSITIVE_INFINITY) {
            connectorDataTypeSpecBuilder.addField(SpecUtils.getStaticFinalDoubleFieldSpec("max_value", data.maxValue()));
        }

        return connectorDataTypeSpecBuilder;
    }

    /**
     * Generates a TypeSpec.Builder with Constants for the {@link Setting}
     *
     * @param settingElement  Element
     * @param setting         {@link Setting}
     * @return TypeSpec.Builder stateTypeSpecBuilder
     */
    public static TypeSpec.Builder createSettingTypeSpecBuilder(Element settingElement, Setting setting) {
        String simpleClassName = settingElement.getSimpleName().toString();

        TypeSpec.Builder stateTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("name", SettingHelper.getSettingName(settingElement, setting)));
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("default", setting.defaultValue()));
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalDoubleFieldSpec("max_length", setting.maxLength()));
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalBooleanFieldSpec("is_password", setting.isPassword()));
        if (setting.minValue() > Double.NEGATIVE_INFINITY) {
            stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalDoubleFieldSpec("min_value", setting.minValue()));
        }
        if (setting.maxValue() < Double.POSITIVE_INFINITY) {
            stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalDoubleFieldSpec("max_value", setting.maxValue()));
        }

        return stateTypeSpecBuilder;
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
    public static TypeSpec.Builder createStateTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element stateElement, State state) {
        String simpleClassName = state.id().isEmpty() ? stateElement.getSimpleName().toString() : state.id();

        TypeSpec.Builder stateTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", StateHelper.getStateId(pluginElement, categoryElement, category, stateElement, state)));
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("desc", StateHelper.getStateDesc(stateElement, state)));
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("default_value", state.defaultValue()));
        stateTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringArrayFieldSpec("value_choices", state.valueChoices()));

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
    public static TypeSpec.Builder createEventTypeSpecBuilder(Element pluginElement, Element categoryElement, Category category, Element eventElement, Event event) {
        String simpleClassName = event.id().isEmpty() ? eventElement.getSimpleName().toString() : event.id();

        TypeSpec.Builder eventTypeSpecBuilder = TypeSpec.classBuilder(SpecUtils.capitalize(simpleClassName)).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        if (!event.subCategoryId().isEmpty()) {
            eventTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", EventHelper.getEventId(pluginElement, categoryElement, category, event.subCategoryId(), eventElement, event)));
            eventTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("sub_category_id", SubCategoryHelper.getSubCategoryId(pluginElement, categoryElement, category, event.subCategoryId())));
        } else {
            eventTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("id", EventHelper.getEventId(pluginElement, categoryElement, category, eventElement, event)));
        }
        eventTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("name", EventHelper.getEventName(eventElement, event)));
        eventTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringFieldSpec("format", event.format()));
        eventTypeSpecBuilder.addField(SpecUtils.getStaticFinalStringArrayFieldSpec("value_choices", event.valueChoices()));

        return eventTypeSpecBuilder;
    }

    /**
     * Internal Get a Static Final String Field initialised with value
     *
     * @param fieldName String
     * @param value     String
     * @return FieldSpec fieldSpec
     */
    public static FieldSpec getStaticFinalStringFieldSpec(String fieldName, String value) {
        return FieldSpec.builder(String.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$S", value).build();
    }

    /**
     * Internal Get a Static Final long Field initialised with value
     *
     * @param fieldName String
     * @param value     long
     * @return FieldSpec fieldSpec
     */
    public static FieldSpec getStaticFinalDoubleFieldSpec(String fieldName, double value) {
        return FieldSpec.builder(double.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$L", value).build();
    }

    /**
     * Internal Get a Static Final long Field initialised with value
     *
     * @param fieldName String
     * @param value     long
     * @return FieldSpec fieldSpec
     */
    public static FieldSpec getStaticFinalLongFieldSpec(String fieldName, long value) {
        return FieldSpec.builder(long.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$L", value).build();
    }

    /**
     * Internal Get a Static Final boolean Field initialised with value
     *
     * @param fieldName String
     * @param value     boolean
     * @return FieldSpec fieldSpec
     */
    public static FieldSpec getStaticFinalBooleanFieldSpec(String fieldName, boolean value) {
        return FieldSpec.builder(boolean.class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("$L", value).build();
    }

    /**
     * Internal Get a Static Final boolean Field initialised with value
     *
     * @param fieldName String
     * @param values    String[]
     * @return FieldSpec fieldSpec
     */
    public static FieldSpec getStaticFinalStringArrayFieldSpec(String fieldName, String[] values) {
        ArrayTypeName stringArray = ArrayTypeName.of(String.class);
        String literal = "{\"" + String.join("\",\"", values) + "\"}";
        return FieldSpec.builder(String[].class, fieldName.toUpperCase()).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer("new $1T $2L", stringArray, literal).build();
    }
}
