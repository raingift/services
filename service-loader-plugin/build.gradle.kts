plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    implementation(gradleApi())

    kapt("com.google.auto.service:auto-service:1.0-rc7")
    implementation("com.didiglobal.booster:booster-android-gradle-api:4.0.0")
    implementation("com.didiglobal.booster:booster-task-spi:4.0.0")
    implementation("com.didiglobal.booster:booster-transform-asm:4.0.0")
    implementation("com.didiglobal.booster:booster-transform-util:4.0.0")
}