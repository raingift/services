plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    implementation(gradleApi())

    kapt("com.google.auto.service:auto-service:1.0-rc7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.didiglobal.booster:booster-android-gradle-api:3.2.0")
    implementation("com.didiglobal.booster:booster-task-spi:3.2.0")
    implementation("com.didiglobal.booster:booster-transform-asm:3.2.0")
    implementation("com.didiglobal.booster:booster-transform-util:3.2.0")
    compileOnly("com.android.tools.build:gradle:4.2.2")
}
