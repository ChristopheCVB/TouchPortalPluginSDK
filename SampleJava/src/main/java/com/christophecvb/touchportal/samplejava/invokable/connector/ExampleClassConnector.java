package com.christophecvb.touchportal.samplejava.invokable.connector;

import com.christophecvb.touchportal.TPConnector;
import com.christophecvb.touchportal.TouchPortalPlugin;
import com.christophecvb.touchportal.annotations.Connector;
import com.christophecvb.touchportal.annotations.ConnectorValue;
import com.christophecvb.touchportal.annotations.Data;
import com.christophecvb.touchportal.model.TPListChangedMessage;
import com.christophecvb.touchportal.samplejava.TouchPortalSampleJavaPlugin;

import java.util.logging.Logger;

@Connector(
        name = "Example Class Connector",
        categoryId = "CategoryWithSubs",
        format = "Connect Example Class Connector with param {$param$}",
        subCategoryId = "Cat1"
)
public class ExampleClassConnector extends TPConnector<TouchPortalSampleJavaPlugin> {
    private final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());

    @ConnectorValue
    private Integer value;
    @Data
    private String param;

    public ExampleClassConnector(TouchPortalSampleJavaPlugin touchPortalPlugin) {
        super(touchPortalPlugin);
    }

    @Override
    public void onInvoke() {
        LOGGER.info("Example Class Connector with param=" + this.param + " and value=" + this.value);
    }

    @Override
    public void onListChanged(TPListChangedMessage tpListChangedMessage) {
        LOGGER.info("ExampleClassConnector.onListChanged");
    }
}
