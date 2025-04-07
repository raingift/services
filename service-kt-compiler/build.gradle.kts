plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp") version "1.9.21-1.0.16"
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("com.google.auto.service:auto-service:1.1.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
    // KSP API
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.16")
    // 代码生成工具
    implementation("com.squareup:kotlinpoet-ksp:1.16.0")
    implementation("com.squareup:kotlinpoet:1.16.0")
}