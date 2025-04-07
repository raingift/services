// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/central/") }
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    kotlin("jvm") version embeddedKotlinVersion apply false
    kotlin("kapt") version embeddedKotlinVersion apply false
    id("maven-publish")
}


subprojects {
    println("sub project: ${this.name}")
    if(this.name == "app") {
        return@subprojects
    }

    apply(plugin = "java-library")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    group = "com.hermes.service.spi"
    version = "1.0.0"

    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/central/") }
        google()
        mavenCentral()
        jcenter() // Warning: this repository is going to shut down soon
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    val sourcesJar by this@subprojects.tasks.registering(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    val javadocJar by this@subprojects.tasks.registering(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        from(tasks["javadoc"])
    }

    publishing {
        repositories {
            maven {
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
            mavenLocal()
        }
        publications {
            create<MavenPublication>("mavenJava") {
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"

                from(components["java"])

                artifact(sourcesJar.get())
                artifact(javadocJar.get())
            }
        }
    }

}

fun Project.java(configure: JavaPluginExtension.() -> Unit): Unit =
        (this as ExtensionAware).extensions.configure("java", configure)

val Project.sourceSets: SourceSetContainer
    get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

val SourceSetContainer.main: NamedDomainObjectProvider<SourceSet>
    get() = named<SourceSet>("main")

val Project.publishing: PublishingExtension
    get() =
        (this as ExtensionAware).extensions.getByName("publishing") as PublishingExtension

fun Project.publishing(configure: PublishingExtension.() -> Unit): Unit =
        (this as ExtensionAware).extensions.configure("publishing", configure)

val Project.signing: SigningExtension
    get() =
        (this as ExtensionAware).extensions.getByName("signing") as SigningExtension

fun Project.signing(configure: SigningExtension.() -> Unit): Unit =
        (this as ExtensionAware).extensions.configure("signing", configure)
