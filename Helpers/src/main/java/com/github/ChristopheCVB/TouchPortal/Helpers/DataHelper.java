package com.github.ChristopheCVB.TouchPortal.Helpers;

import com.github.ChristopheCVB.TouchPortal.Annotations.Action;
import com.github.ChristopheCVB.TouchPortal.Annotations.Data;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.lang.model.element.Element;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Touch Portal Plugin Data Helper
 */
public class DataHelper {
    public static final String ID = GenericHelper.ID;
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_CHOICE = GenericHelper.TYPE_CHOICE;
    public static final String LABEL = "label";
    public static final String DEFAULT = GenericHelper.DEFAULT;
    public static final String VALUE_CHOICES = "valueChoices";

    protected static final String KEY_DATA = "data";

    /**
     * Get the formatted Data Id
     *
     * @param element Element
     * @param data {@link Data}
     * @param action {@link Action}
     * @return String dataId
     */
    public static String getActionDataId(Element element, Data data, Action action) {
        return DataHelper._getActionDataId(ActionHelper.getActionId(element.getEnclosingElement(), action), data.id().isEmpty() ? element.getSimpleName().toString() : data.id());
    }

    /**
     * Get the formatted Data Label
     *
     * @param dataElement Element
     * @param data {@link Data}
     * @return String dataLabel
     */
    public static String getActionDataLabel(Element dataElement, Data data) {
        return data.label().isEmpty() ? dataElement.getSimpleName().toString() : data.label();
    }

    /**
     * Retrieve the Action Data Id
     *
     * @param pluginClass Class
     * @param actionMethodName String
     * @param actionParameterName String
     * @return String actionDataId
     */
    public static String getActionDataId(Class pluginClass, String actionMethodName, String actionParameterName) {
        String actionDataId = "";
        for (Method declaredMethod : pluginClass.getDeclaredMethods()) {
            if (declaredMethod.getName().equals(actionMethodName)) {
                for (Parameter parameter : declaredMethod.getParameters()) {
                    Data data = parameter.getAnnotation(Data.class);
                    if (data != null && parameter.getName().equals(actionParameterName)) {
                        actionDataId = DataHelper._getActionDataId(ActionHelper.getActionId(pluginClass, actionMethodName), data.id().isEmpty() ? actionParameterName : data.id());
                    }
                }
            }
        }
        return actionDataId;
    }

    private static String _getActionDataId(String actionId, String dataId) {
        return actionId + "." + DataHelper.KEY_DATA + "." + dataId;
    }
}
