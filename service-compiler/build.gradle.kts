plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    kapt("com.google.auto.service:auto-service:1.1.1")
    implementation("com.squareup:javapoet:1.13.0")

    implementation("com.google.auto.service:auto-service:1.1.1")
}