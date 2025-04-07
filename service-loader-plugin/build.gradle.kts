plugins {
    id("java-library")
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm")
    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(gradleApi())

    implementation("com.android.tools.build:gradle:8.2.2")
    compileOnly("commons-io:commons-io:2.16.1")
    compileOnly("commons-codec:commons-codec:1.15")
    compileOnly("org.ow2.asm:asm-commons:9.7")
    compileOnly("org.ow2.asm:asm-tree:9.4")
}

// The project version will be used as your plugin version when publishing.
group = "io.github.raingitft"
version = "1.0.0"

@Suppress("UnstableApiUsage")
gradlePlugin {
    plugins {
        create("ServiceLoaderPlugin") {
            id = "io.github.raingitft.ServiceLoaderPlugin"
            implementationClass = "com.hermes.service.loader.ServiceLoaderPlugin"
            displayName = "ServiceLoader AGP8.2+ plugin"
            description = "ServiceLoader AGP8.2+ plugin"
            tags.set(
                listOf(
                    "ServiceLoader",
                    "AGP8",
                    "ServiceLoaderPlugin plugin",
                    "AGP8",
                    "Transform",
                    "Auto Register"
                )
            )
        }
    }
}