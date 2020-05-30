package com.github.ChristopheCVB.TouchPortal.Helpers;

import javax.lang.model.element.Element;

/**
 * Touch Portal Plugin Category Helper
 */
public class CategoryHelper {
    public static final String ID = GenericHelper.ID;
    public static final String NAME = GenericHelper.NAME;
    public static final String IMAGE_PATH = "imagepath";
    public static final String ACTIONS = "actions";
    public static final String EVENTS = "events";
    public static final String STATES = "states";

    protected static final String KEY_CATEGORY = "basecategory";

    /**
     * Get the formatted Category ID
     *
     * @param element Element
     * @return String categoryId
     */
    public static String getCategoryId(Element element) {
        return CategoryHelper._getCategoryId(PluginHelper.getPluginId(element));
    }

    /**
     * Get the formatted Category ID
     *
     * @param pluginClass Class
     * @return String categoryId
     */
    public static String getCategoryId(Class pluginClass) {
        return CategoryHelper._getCategoryId(PluginHelper.getPluginId(pluginClass));
    }

    /**
     * Internal Get the formatted Category ID
     *
     * @param pluginId String
     * @return categoryId
     */
    private static String _getCategoryId(String pluginId) {
        return pluginId + "." + CategoryHelper.KEY_CATEGORY;
    }
}
