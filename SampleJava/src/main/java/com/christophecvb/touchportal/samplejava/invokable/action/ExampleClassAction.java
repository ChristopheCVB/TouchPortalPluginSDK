package com.christophecvb.touchportal.samplejava.invokable.action;

import com.christophecvb.touchportal.TPAction;
import com.christophecvb.touchportal.TouchPortalPlugin;
import com.christophecvb.touchportal.annotations.Action;
import com.christophecvb.touchportal.annotations.ActionTranslation;
import com.christophecvb.touchportal.annotations.Data;
import com.christophecvb.touchportal.annotations.Language;
import com.christophecvb.touchportal.model.TPListChangedMessage;
import com.christophecvb.touchportal.samplejava.TouchPortalSampleJavaPlugin;

import java.util.logging.Logger;

@Action(
        name = "Example Class Action",
        format = "Example Class Action with param {$param$}",
        categoryId = "BaseCategory"
)
@ActionTranslation(
        language = Language.FRENCH,
        name = "Exemple d'Action via une Classe",
        format = "Exemple d'Action via une Classe avec le paramètre {$param$}"
)
public class ExampleClassAction extends TPAction<TouchPortalSampleJavaPlugin> {
    private final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());

    @Data
    private String param;

    public ExampleClassAction(TouchPortalSampleJavaPlugin touchPortalPlugin) {
        super(touchPortalPlugin);
    }

    @Override
    public void onInvoke() {
        LOGGER.info("ExampleClassAction.onInvoke this.param=" + this.param);
    }

    @Override
    public void onListChanged(TPListChangedMessage tpListChangedMessage) {
        LOGGER.info("ExampleClassAction.onListChanged");
    }
}
