/*
 * Touch Portal Plugin SDK
 *
 * Copyright 2020 Christophe Carvalho Vilas-Boas
 * christophe.carvalhovilasboas@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.christophecvb.touchportal.annotations.processor;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.annotations.processor.utils.Pair;
import com.christophecvb.touchportal.annotations.processor.utils.SpecUtils;
import com.christophecvb.touchportal.helpers.*;
import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Touch Portal Plugin Annotations Processor
 */
@AutoService(Processor.class)
public class TouchPortalPluginAnnotationsProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;

    public Messager getMessager() {
        return this.messager;
    }

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
        annotations.add(Setting.class.getCanonicalName());
        annotations.add(Category.class.getCanonicalName());
        annotations.add(Action.class.getCanonicalName());
        annotations.add(ActionTranslation.class.getCanonicalName());
        annotations.add(ActionTranslations.class.getCanonicalName());
        annotations.add(Data.class.getCanonicalName());
        annotations.add(State.class.getCanonicalName());
        annotations.add(Event.class.getCanonicalName());
        annotations.add(Connector.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || annotations.size() == 0) {
            return false;
        }
        this.messager.printMessage(Diagnostic.Kind.NOTE, this.getClass().getSimpleName() + ".process");

        try {
            Set<? extends Element> plugins = roundEnv.getElementsAnnotatedWith(Plugin.class);
            if (plugins.size() != 1) {
                throw new TPAnnotationException.Builder(Plugin.class).count(plugins.size()).build();
            }
            for (Element pluginElement : plugins) {
                Pair<JsonObject, TypeSpec.Builder> pluginPair = PluginProcessor.process(this, roundEnv, pluginElement);

                String entryFileName = "resources/" + PluginHelper.ENTRY_TP;
                FileObject actionFileObject = this.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", entryFileName, pluginElement);
                Writer writer = actionFileObject.openWriter();
                writer.write(pluginPair.first.toString());
                writer.flush();
                writer.close();

                TypeSpec pluginTypeSpec = pluginPair.second.build();
                String packageName = ((PackageElement) pluginElement.getEnclosingElement()).getQualifiedName().toString();
                JavaFile javaConstantsFile = JavaFile.builder(packageName, pluginTypeSpec).build();
                javaConstantsFile.writeTo(this.filer);
            }
        }
        catch (Exception exception) {
            this.messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
        }

        return true;
    }
}
