package com.christophecvb.touchportal.packager

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

class TouchPortalPluginPackager implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def extension = project.extensions.create('tpPlugin', TouchPortalPluginPackagerExtension)

        project.tasks.withType(JavaCompile) { task ->
            task.doFirst {
                println('Adding -parameters to Compiler Args')
                options.compilerArgs.add('-parameters')
            }
        }

        project.tasks.named('jar', Jar) { task ->
            dependsOn project.configurations.runtimeClasspath

            manifest {
                attributes 'Implementation-Title': "${extension.mainClassSimpleName.get()}",
                        'Implementation-Version': "${project.version}",
                        'Main-Class': "${project.group}.${extension.mainClassSimpleName.get()}"
            }
            from {
                project.configurations.runtimeClasspath.findAll { it.name.endsWith('jar') }.collect { project.zipTree(it) }
            }
        }

        def copyResources = project.tasks.register('copyResources', Copy) {
            group 'Touch Portal Plugin'
            from(project.file("${project.buildDir}/resources/main/"))
            into("${project.buildDir}/plugin/${extension.mainClassSimpleName.get()}/")

            doLast {
                println 'Resources Copied into plugin directory'
            }
        }

        def copyJar = project.tasks.register('copyJar', Copy) {
            group 'Touch Portal Plugin'
            dependsOn project.jar
            from(project.file("${project.buildDir}/libs/"))
            into("${project.buildDir}/plugin/${extension.mainClassSimpleName.get()}/")
            rename {
                "${extension.mainClassSimpleName.get()}.jar"
            }

            doLast {
                println 'Jar Copied into plugin directory'
            }
        }

        def copyGeneratedResources = project.tasks.register('copyGeneratedResources', Copy) {
            group 'Touch Portal Plugin'
            dependsOn copyJar
            from(project.file("${project.buildDir}/generated/sources/annotationProcessor/java/main/resources/"))
            into("${project.buildDir}/plugin/${extension.mainClassSimpleName.get()}/")

            doLast {
                println 'Generated Resources Copied into plugin directory'
            }
        }

        def packagePlugin = project.tasks.register('packagePlugin', Zip) {
            group 'Touch Portal Plugin'
            description 'Package the Project into a TPP'
            dependsOn copyResources, copyGeneratedResources

            archiveFileName = "${extension.mainClassSimpleName.get()}.tpp"
            destinationDirectory = project.file("${project.buildDir}/plugin")
            from "${project.buildDir}/plugin/"
            exclude "*.tpp"
            includeEmptyDirs = false

            doLast {
                println 'Plugin Packaged'
            }
        }

//        project.tasks {
//
//            task packagePlugin(type: Zip) {
//                group 'Touch Portal Plugin'
//                description 'Package the Project into a TPP'
//                dependsOn copyResources, copyGeneratedResources, copyJar
//
//                archiveFileName = "${mainClassSimpleName}.tpp"
//                destinationDirectory = file("$buildDir/plugin")
//                from "$buildDir/plugin/"
//                exclude "*.tpp"
//                includeEmptyDirs = false
//
//                doLast {
//                    println 'Plugin Packaged'
//                }
//            }
//        }
    }
}

abstract class TouchPortalPluginPackagerExtension {
    abstract Property<String> getMainClassSimpleName()

    TouchPortalPluginPackagerExtension() {
        mainClassSimpleName.convention('TouchPortalPlugin')
    }
}