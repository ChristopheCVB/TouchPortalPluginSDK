package com.christophecvb.touchportal.samplekotlin

import com.christophecvb.touchportal.annotations.Action
import com.christophecvb.touchportal.annotations.Category
import com.christophecvb.touchportal.annotations.Plugin
import com.christophecvb.touchportal.helpers.PluginHelper
import com.christophecvb.touchportal.TouchPortalPlugin
import com.christophecvb.touchportal.annotations.Data
import com.christophecvb.touchportal.model.TPBroadcastMessage
import com.christophecvb.touchportal.model.TPInfoMessage
import com.christophecvb.touchportal.model.TPListChangeMessage
import com.christophecvb.touchportal.model.TPSettingsMessage
import com.google.gson.JsonObject
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

@Suppress("unused")
@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#556677", colorLight = "#112233")
class TouchPortalSampleKotlinPlugin(parallelizeActions: Boolean) : TouchPortalPlugin(parallelizeActions), TouchPortalPlugin.TouchPortalPluginListener {

    companion object {
        /**
         * Logger
         */
        private val LOGGER = Logger.getLogger(TouchPortalPlugin::class.java.name)

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size == 1) {
                if (PluginHelper.COMMAND_START == args[0]) {
                    // Initialize the Plugin
                    val touchPortalSampleKotlinPlugin = TouchPortalSampleKotlinPlugin(true)

                    // Initiate the connection with the Touch Portal Plugin System
                    val connectedPairedAndListening = touchPortalSampleKotlinPlugin.connectThenPairAndListen(touchPortalSampleKotlinPlugin)
                    @Suppress("ControlFlowWithEmptyBody")
                    if (connectedPairedAndListening) {
                        // Let's go!
                        LOGGER.log(Level.INFO, "Plugin with ID[${TouchPortalSampleKotlinPluginConstants.ID}] Connected and Paired!")
                    }
                }
            }
        }
    }

    @Suppress("unused")
    enum class Categories {
        @Category(imagePath = "images/icon-24.png")
        BaseCategory
    }

    @Action(name = "Log Current Time Millis", categoryId = "BaseCategory")
    fun logTime() {
        LOGGER.log(Level.INFO, System.currentTimeMillis().toString())
    }

    @Action(name = "Action with Text", format = "Action with {\$text\$}", categoryId = "BaseCategory")
    fun actionWithText(@Data text: String) {
        LOGGER.log(Level.INFO, text)
    }

    override fun onDisconnected(exception: Exception?) {
        exitProcess(0)
    }

    override fun onReceived(jsonMessage: JsonObject) {}
    override fun onListChanged(tpListChangeMessage: TPListChangeMessage) {}
    override fun onInfo(tpInfoMessage: TPInfoMessage) {}
    override fun onBroadcast(tpBroadcastMessage: TPBroadcastMessage) {}
    override fun onSettings(tpSettingsMessage: TPSettingsMessage) {}
}
