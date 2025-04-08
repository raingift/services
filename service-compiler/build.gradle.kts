plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    kapt("com.google.auto.service:auto-service:1.0-rc7")
    implementation("com.google.auto.service:auto-service:1.0-rc7")

    implementation("com.squareup:javapoet:1.13.0")
}