package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.ConnectorHelper;
import com.christophecvb.touchportal.helpers.GenericHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.Set;

public class ConnectorProcessor {

    /**
     * Generates a JsonObject and a TypeSpec.Builder representing the {@link Connector}
     *
     * @param processor         {@link TouchPortalPluginAnnotationsProcessor}
     * @param roundEnv          RoundEnvironment
     * @param pluginElement     Element
     * @param plugin            {@link Plugin}
     * @param categoryElement   Element
     * @param category          {@link Category}
     * @param connectorElement  Element
     * @return Pair&lt;JsonObject, TypeSpec.Builder&gt; actionPair
     * @throws GenericHelper.TPTypeException If a used type is not Supported
     * @throws TPAnnotationException If an Annotation is misused
     */
    public static Pair<JsonObject, TypeSpec.Builder> process(TouchPortalPluginAnnotationsProcessor processor, RoundEnvironment roundEnv, Element pluginElement, Plugin plugin, Element categoryElement, Category category, Element connectorElement) throws GenericHelper.TPTypeException, TPAnnotationException {
        processor.getMessager().printMessage(Diagnostic.Kind.NOTE, "Process Connector: " + connectorElement.getSimpleName());
        Connector connector = connectorElement.getAnnotation(Connector.class);

        TypeSpec.Builder connectorTypeSpecBuilder = SpecUtils.createConnectorTypeSpecBuilder(pluginElement, categoryElement, category, connectorElement, connector);

        JsonObject jsonConnector = new JsonObject();
        jsonConnector.addProperty(ConnectorHelper.ID, ConnectorHelper.getConnectorId(pluginElement, categoryElement, category, connectorElement, connector));
        jsonConnector.addProperty(ConnectorHelper.NAME, ConnectorHelper.getConnectorName(connectorElement, connector));
        jsonConnector.addProperty(ConnectorHelper.FORMAT, connector.format());

        String classMethod = pluginElement.getSimpleName() + "." + connectorElement.getSimpleName();
        boolean connectorValueFound = false;
        Set<? extends Element> connectorValueElements = roundEnv.getElementsAnnotatedWith(ConnectorValue.class);
        for (Element connectorValueElement : connectorValueElements) {
            Element enclosingElement = connectorValueElement.getEnclosingElement();
            if (connectorElement.equals(enclosingElement)) {
                String classMethodParameter = classMethod + "(" + connectorValueElement.getSimpleName() + ")";
                String desiredType = GenericHelper.getTouchPortalType(classMethodParameter, connectorValueElement);
                if (!desiredType.equals(GenericHelper.TP_TYPE_NUMBER)) {
                    throw new GenericHelper.TPTypeException.Builder(classMethodParameter).typeUnsupported(desiredType).forAnnotation(GenericHelper.TPTypeException.ForAnnotation.CONNECTOR_VALUE).build();
                }
                connectorValueFound = true;
                break;
            }
        }
        if (!connectorValueFound) {
            throw new TPAnnotationException.Builder(ConnectorValue.class).isMissing(true).forElement(connectorElement).build();
        }

        JsonArray jsonConnectorData = new JsonArray();
        Set<? extends Element> dataElements = roundEnv.getElementsAnnotatedWith(Data.class);
        for (Element dataElement : dataElements) {
            Element enclosingElement = dataElement.getEnclosingElement();
            if (connectorElement.equals(enclosingElement)) {
                Pair<JsonObject, TypeSpec.Builder> connectorDataResult = DataProcessor.process(processor, roundEnv, pluginElement, plugin, categoryElement, category, connectorElement, connector, jsonConnector, dataElement);
                jsonConnectorData.add(connectorDataResult.first);
                if (connectorDataResult.second != null) {
                    connectorTypeSpecBuilder.addType(connectorDataResult.second.build());
                }
            }
        }
        if (jsonConnectorData.size() > 0) {
            jsonConnector.add(ConnectorHelper.DATA, jsonConnectorData);
        }

        return Pair.create(jsonConnector, connectorTypeSpecBuilder);
    }
}
