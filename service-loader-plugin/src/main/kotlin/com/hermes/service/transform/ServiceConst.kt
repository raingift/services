package com.hermes.service.transform

/**
 * filter class packages name
 */
internal val PACKAGES_IGNORED = setOf("android", "androidx", "kotlin", "kotlinx", "java", "javax")

/**
 * filter config file
 */
const val META_INFO_SERVICES = "META-INF/services/"

/**
 * The register code is generated into this class
 */
const val GENERATE_TO_CLASS_NAME = "com/hermes/service/register/ServiceRegistry"

/**
 * The register code is generated into this class
 */
const val GENERATE_TO_CLASS_FILE_NAME = "$GENERATE_TO_CLASS_NAME.class"

/**
 * register method name in class: {@link #GENERATE_TO_CLASS_NAME}
 */
const val REGISTER_METHOD_NAME = "register"