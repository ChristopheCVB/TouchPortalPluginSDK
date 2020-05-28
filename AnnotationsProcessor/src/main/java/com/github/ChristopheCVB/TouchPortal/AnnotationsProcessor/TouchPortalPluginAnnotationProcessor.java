package com.github.ChristopheCVB.TouchPortal.AnnotationsProcessor;

import com.github.ChristopheCVB.TouchPortal.Annotations.*;
import com.google.auto.service.AutoService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
public class TouchPortalPluginAnnotationProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Plugin.class.getCanonicalName());
        annotations.add(Action.class.getCanonicalName());
        annotations.add(State.class.getCanonicalName());
        annotations.add(Event.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        if (env.processingOver()) {
            return false;
        }
        this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName()+".process");

        try {
            JSONObject jsonPlugin = null;
            Element selectedPluginElement = null;

            Set<? extends Element> plugins = env.getElementsAnnotatedWith(Plugin.class);
            if (plugins.size() != 1)
            {
                throw new Exception("You need 1(one) @Plugin Annotation, you have " + plugins.size());
            }
            for (Element pluginElement : plugins) {
                selectedPluginElement = pluginElement;
                jsonPlugin = this.processPlugin(pluginElement);
            }

            JSONObject jsonCategory = new JSONObject();
            jsonCategory.put("id", this.getCategoryId(selectedPluginElement));
            jsonCategory.put("name", selectedPluginElement.getSimpleName());
            jsonCategory.put("imagepath", "images/icon-24.png");

            jsonPlugin.accumulate("categories", jsonCategory);

            Set<? extends Element> actions = env.getElementsAnnotatedWith(Action.class);
            for (Element action : actions) {
                jsonCategory.accumulate("actions", this.processAction(action));
            }

            String actionFileName = "resources/entry.tp";
            FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", actionFileName, selectedPluginElement);
            Writer writer = actionFileObject.openWriter();
            writer.write(jsonPlugin.toString());
            writer.flush();
            writer.close();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }

        return true;
    }

    private JSONObject processAction(Element actionElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Action: " + actionElement.getSimpleName());

        Action action = actionElement.getAnnotation(Action.class);
        JSONObject jsonAction = new JSONObject();
        jsonAction.put("name", this.getActionName(actionElement, action));
        jsonAction.put("id", this.getActionId(actionElement, action));

        Data[] dataArray = actionElement.getAnnotationsByType(Data.class);
        this.messager.printMessage(Diagnostic.Kind.NOTE, "dataArray: "+ dataArray.length);
        if (dataArray.length > 0) {
            for (Data data : dataArray) {
                JSONObject jsonData = new JSONObject();
                jsonData.put("id", data.annotationType());

                jsonAction.accumulate("data", jsonData);
            }
        }
        return jsonAction;
    }

    private JSONObject processPlugin(Element pluginElement) throws JSONException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Process Plugin: " + pluginElement.getSimpleName());
        Plugin plugin = pluginElement.getAnnotation(Plugin.class);
        JSONObject jsonPlugin = new JSONObject();
        jsonPlugin.put("sdk", 2);
        jsonPlugin.put("version", plugin.version());
        jsonPlugin.put("name", pluginElement.getSimpleName());
        jsonPlugin.put("id", this.getPluginId(pluginElement));
        JSONObject jsonConfiguration = new JSONObject();
        jsonConfiguration.put("colorDark", plugin.colorDark());
        jsonConfiguration.put("colorLight", plugin.colorLight());
        jsonPlugin.put("configuration", jsonConfiguration);

        return jsonPlugin;
    }

    private String getPluginId(Element element) {
        return ((PackageElement) element.getEnclosingElement()).getQualifiedName() + "." + element.getSimpleName();
    }

    private String getCategoryId(Element element) {
        return this.getPluginId(element) + ".basecategory";
    }

    private String getActionId(Element element, Action action) {
        return this.getCategoryId(element.getEnclosingElement()) + "." + (action.id().isEmpty() ? element.getSimpleName() : action.id());
    }

    private String getActionName(Element element, Action action) {
        return action.name().isEmpty() ? element.getSimpleName().toString() : action.name();
    }
}
