package com.hermes.service.loader

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.internal.plugins.AppPlugin
import com.hermes.service.transform.ServiceTransformTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.java

@Suppress("unused")
class ServiceLoaderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("serviceLoader", ServiceLoaderPluginExtension::class.java)
        // Only app module will use this plugin
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            println("Init ServiceLoaderGradlePlugin")
            val androidComponents =
                project.extensions.getByType(AndroidComponentsExtension::class.java)

            println("-------- ServiceLoaderGradlePlugin Current environment --------")
            println("Gradle Version ${project.gradle.gradleVersion}")
            println("${androidComponents.pluginVersion}")
            println("JDK Version ${System.getProperty("java.version")}")

            androidComponents.onVariants { variant ->
                variant.instrumentation.excludes.addAll(
                    "androidx/**",
                    "android/**",
                    "com/google/**",
                )
                val taskProviderTransformAllClassesTask =
                    project.tasks.register(
                        "${variant.name}TransformAllClassesTask",
                        ServiceTransformTask::class.java
                    )
                // https://github.com/android/gradle-recipes
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskProviderTransformAllClassesTask)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        ServiceTransformTask::allJars,
                        ServiceTransformTask::allDirectories,
                        ServiceTransformTask::output
                    )
            }
        }
    }
}