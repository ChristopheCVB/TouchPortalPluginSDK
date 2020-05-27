package com.github.ChristopheCVB.TouchPortal;

import com.google.auto.service.AutoService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
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
    annotations.add(TouchPortalPluginAnnotations.Action.class.getCanonicalName());
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
    try {
      for (TypeElement typeElement : set) {
        TouchPortalPluginAnnotations.Action action = (TouchPortalPluginAnnotations.Action) typeElement;
        String actionFileName = "action"+action.name()+".json";
        FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", actionFileName, typeElement);
        JSONObject jsonAction = new JSONObject();
        jsonAction.put("name", ((TouchPortalPluginAnnotations.Action)typeElement).name());
        Writer writer = actionFileObject.openWriter();
        writer.write(jsonAction.toString());
        writer.flush();
        writer.close();
      }
    }
    catch (IOException | JSONException ioException) {
      ioException.printStackTrace();
    }
    return false;
  }
}