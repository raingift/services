package com.hermes.service.loader

/**
 * service loader plugin extension
 */
class ServiceLoaderPluginExtension {

    /**
     * enable debug log info
     * true: enable && false: disable
     */
    var enableDebug: Boolean = false

    /**
     * debug collection info
     */
    var debugCollection: Boolean = false

    /**
     * filter file
     */
    var exclude: List<String> = mutableListOf()
}