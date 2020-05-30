package com.github.ChristopheCVB.TouchPortal.Helpers;

import javax.lang.model.element.Element;

/**
 * Touch Portal Plugin Generic Helper
 */
public class GenericHelper {
    protected static final String ID = "id";
    protected static final String NAME = "name";
    protected static final String TYPE = "type";
    protected static final String TYPE_CHOICE = "choice";
    protected static final String DESCRIPTION = "description";
    protected static final String VALUE = "value";
    protected static final String DEFAULT = "default";

    /**
     * Retrieve the internal Touch Portal type according to the Java's element type
     * @param element Element
     * @return String tpType
     */
    public static String getTouchPortalType(Element element) {
        String tpType;
        String elementType = element.asType().toString();
        switch (elementType) {
            case "byte":
            case "char":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "java.lang.Byte":
            case "java.lang.Char":
            case "java.lang.Short":
            case "java.lang.Integer":
            case "java.lang.Long":
            case "java.lang.Float":
            case "java.lang.Double":
                tpType = "Number";
                break;

            case "boolean":
            case "java.lang.Boolean":
                tpType = "switch";
                break;

            default:
                if (elementType.endsWith("[]")) {
                    tpType = "choice";
                }
                else {
                    tpType = "text";
                }
                break;
        }
        return tpType;
    }
}
