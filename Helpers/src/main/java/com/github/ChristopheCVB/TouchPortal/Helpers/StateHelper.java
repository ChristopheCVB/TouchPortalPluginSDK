package com.github.ChristopheCVB.TouchPortal.Helpers;

import com.github.ChristopheCVB.TouchPortal.Annotations.Action;
import com.github.ChristopheCVB.TouchPortal.Annotations.State;

import javax.lang.model.element.Element;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Touch Portal Plugin State Helper
 */
public class StateHelper {
    public static final String ID = GenericHelper.ID;
    public static final String TYPE = GenericHelper.TYPE;
    public static final String TYPE_CHOICE = "choice";
    public static final String TYPE_TEXT = "text";
    public static final String DESC = "desc";
    public static final String DEFAULT = GenericHelper.DEFAULT;
    public static final String VALUE_CHOICES = "valueChoices";

    protected static final String KEY_STATE = "state";

    /**
     * Get the formatted State ID
     *
     * @param element Element
     * @param state {@link State}
     * @return String stateId
     */
    public static String getStateId(Element element, State state) {
        return StateHelper._getStateId(CategoryHelper.getCategoryId(element.getEnclosingElement()), state.id().isEmpty() ? element.getSimpleName().toString() : state.id());
    }

    /**
     * Get the formatted State Desc
     *
     * @param element Element
     * @param state {@link State}
     * @return String stateDesc
     */
    public static String getStateDesc(Element element, State state) {
        return state.desc().isEmpty() ? element.getSimpleName().toString() : state.desc();
    }

    /**
     * Get the formatted State ID
     *
     * @param pluginClass Class
     * @param stateFieldName String
     * @return String stateId
     */
    public static String getStateId(Class pluginClass, String stateFieldName) {
        String stateId = "";

        try {
            Field stateField = pluginClass.getDeclaredField(stateFieldName);
            if (stateField.isAnnotationPresent(State.class)) {
                State state = stateField.getAnnotation(State.class);
                stateId = StateHelper._getStateId(CategoryHelper.getCategoryId(pluginClass), state.id().isEmpty() ? stateFieldName : state.id());
            }
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return stateId;
    }

    /**
     * Internal Get the formatted State ID
     *
     * @param categoryId String
     * @param rawStateId String
     * @return stateId
     */
    private static String _getStateId(String categoryId, String rawStateId) {
        return categoryId + "." + StateHelper.KEY_STATE + "." + rawStateId;
    }
}
