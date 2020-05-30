package com.github.ChristopheCVB.TouchPortal.Helpers;

import com.github.ChristopheCVB.TouchPortal.Annotations.Plugin;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

/**
 * Touch Portal Plugin Plugin Helper
 */
public class PluginHelper {
    public static final String SDK = "sdk";
    public static final String VERSION = "version";
    public static final String NAME = GenericHelper.NAME;
    public static final String ID = GenericHelper.ID;
    public static final String CONFIGURATION = "configuration";
    public static final String CONFIGURATION_COLOR_DARK = "colorDark";
    public static final String CONFIGURATION_COLOR_LIGHT = "colorLight";
    public static final String PLUGIN_START_COMMAND = "plugin_start_cmd";
    public static final String CATEGORIES = "categories";

    /**
     * Argument passed to the jar to start the plugin
     */
    public static final String COMMAND_START = "start";
    /**
     * Touch Portal entry file
     */
    public static final String ENTRY_TP = "entry.tp";

    /**
     * Get the formatted Plugin ID
     *
     * @param element Element
     * @return String pluginId
     */
    public static String getPluginId(Element element) {
        return PluginHelper._getPluginId(((PackageElement) element.getEnclosingElement()).getQualifiedName() + "." + element.getSimpleName());
    }

    /**
     * Get the formatted Plugin Name
     *
     * @param element Element
     * @param plugin {@link Plugin}
     * @return String pluginName
     */
    public static String getPluginName(Element element, Plugin plugin) {
        return plugin.name().isEmpty() ? element.getSimpleName().toString() : plugin.name();
    }

    /**
     * Get the formatted Plugin ID
     *
     * @param pluginClass Class
     * @return pluginId
     */
    public static String getPluginId(Class pluginClass) {
        return PluginHelper._getPluginId(pluginClass.getName());
    }

    /**
     * Internal Get the formatted Plugin ID
     *
     * @param pluginClassName String
     * @return String pluginId
     */
    private static String _getPluginId(String pluginClassName) {
        return pluginClassName;
    }
}
