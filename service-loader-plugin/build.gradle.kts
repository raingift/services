plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    implementation(gradleApi())

    kapt("com.google.auto.service:auto-service:1.0-rc7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.didiglobal.booster:booster-android-gradle-api:1.3.1")
    implementation("com.didiglobal.booster:booster-task-spi:1.3.1")
    implementation("com.didiglobal.booster:booster-transform-asm:1.3.0")
    implementation("com.didiglobal.booster:booster-transform-util:1.3.1")
    compileOnly("com.android.tools.build:gradle:4.2.1")
}
