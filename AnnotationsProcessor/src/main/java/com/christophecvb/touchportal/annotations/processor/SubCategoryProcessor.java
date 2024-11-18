package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.Category;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.SubCategoryHelper;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

public class SubCategoryProcessor {

    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, Element pluginElement, Category category, Element categoryElement, Category.SubCategory subCategory) {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process SubCategory: " + subCategory.id());

        TypeSpec.Builder categoryTypeSpecBuilder = SpecUtils.createSubCategoryTypeSpecBuilder(pluginElement, categoryElement, category, subCategory).addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        JsonObject jsonSubCategory = new JsonObject();

        jsonSubCategory.addProperty(SubCategoryHelper.ID, SubCategoryHelper.getSubCategoryId(pluginElement, categoryElement, category, subCategory));
        jsonSubCategory.addProperty(SubCategoryHelper.NAME, subCategory.name());
        if (!subCategory.imagePath().isEmpty()) {
            jsonSubCategory.addProperty(SubCategoryHelper.IMAGE_PATH, subCategory.imagePath());
        }

        return Pair.create(jsonSubCategory, categoryTypeSpecBuilder);
    }
}
