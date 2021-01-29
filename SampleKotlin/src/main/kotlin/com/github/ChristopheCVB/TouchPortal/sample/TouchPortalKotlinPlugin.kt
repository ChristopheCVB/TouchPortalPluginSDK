package com.github.ChristopheCVB.TouchPortal.sample

import com.github.ChristopheCVB.TouchPortal.Annotations.Action
import com.github.ChristopheCVB.TouchPortal.Annotations.Category
import com.github.ChristopheCVB.TouchPortal.Annotations.Plugin
import com.github.ChristopheCVB.TouchPortal.Helpers.PluginHelper
import com.github.ChristopheCVB.TouchPortal.TouchPortalPlugin
import com.github.ChristopheCVB.TouchPortal.model.TPBroadcastMessage
import com.github.ChristopheCVB.TouchPortal.model.TPInfoMessage
import com.github.ChristopheCVB.TouchPortal.model.TPListChangeMessage
import com.github.ChristopheCVB.TouchPortal.model.TPSettingsMessage
import com.google.gson.JsonObject
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

@Suppress("unused")
@Plugin(version = 4100, colorDark = "#556677", colorLight = "#112233")
class TouchPortalKotlinPlugin(parallelizeActions: Boolean) : TouchPortalPlugin(parallelizeActions), TouchPortalPlugin.TouchPortalPluginListener {

    /**
     * Logger
     */
    private val LOGGER = Logger.getLogger(TouchPortalPlugin::class.java.name)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size == 1) {
                if (PluginHelper.COMMAND_START == args[0]) {
                    // Initialize the Plugin
                    val touchPortalPluginExample = TouchPortalKotlinPlugin(true)

                    // Initiate the connection with the Touch Portal Plugin System
                    val connectedPairedAndListening = touchPortalPluginExample.connectThenPairAndListen(touchPortalPluginExample)
                    @Suppress("ControlFlowWithEmptyBody")
                    if (connectedPairedAndListening) {
                        // Let's go!
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

    @Action(description = "Log Current Time Millis", categoryId = "BaseCategory")
    fun logTime() {
        LOGGER.log(Level.INFO, System.currentTimeMillis().toString())
    }

    override fun onDisconnected(exception: Exception?) {
        exitProcess(0)
    }

    override fun onReceived(jsonMessage: JsonObject) {}
    override fun onListChanged(tpListChangeMessage: TPListChangeMessage) {}
    override fun onInfo(tpInfoMessage: TPInfoMessage?) {}
    override fun onBroadcast(tpBroadcastMessage: TPBroadcastMessage) {}
    override fun onSettings(tpSettingsMessage: TPSettingsMessage) {}
}
