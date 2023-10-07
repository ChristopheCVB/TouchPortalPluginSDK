package com.christophecvb.touchportal.helpers;

import com.christophecvb.touchportal.annotations.Category;

import javax.lang.model.element.Element;

public class SubCategoryHelper {
    public static final String ID = GenericHelper.ID;
    public static final String NAME = GenericHelper.NAME;
    public static final String ICON_RELATIVE_PATH = "iconRelativePath";


    /**
     * Get the generated SubCategory ID
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param subCategory     {@link Category.SubCategory}
     * @return String subCategoryId
     */
    public static String getSubCategoryId(Element pluginElement, Element categoryElement, Category category, Category.SubCategory subCategory) {
        return SubCategoryHelper._getSubCategoryId(CategoryHelper.getCategoryId(pluginElement, categoryElement, category), subCategory.id());
    }

    /**
     * Get the generated SubCategory ID
     *
     * @param pluginElement   Element
     * @param categoryElement Element
     * @param category        {@link Category}
     * @param subCategoryId   String
     * @return String subCategoryId
     */
    public static String getSubCategoryId(Element pluginElement, Element categoryElement, Category category, String subCategoryId) {
        return SubCategoryHelper._getSubCategoryId(CategoryHelper.getCategoryId(pluginElement, categoryElement, category), subCategoryId);
    }

    /**
     * Internal - Get the formatted SubCategory ID
     *
     * @param categoryId    String
     * @param subCategoryId String
     * @return String subCategoryId
     */
    private static String _getSubCategoryId(String categoryId, String subCategoryId) {
        return categoryId + "." + subCategoryId;
    }
}
