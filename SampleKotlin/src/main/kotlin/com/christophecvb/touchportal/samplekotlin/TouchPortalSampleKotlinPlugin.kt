/*
 * Touch Portal Plugin SDK
 *
 * Copyright 2020 Christophe Carvalho Vilas-Boas
 * christophe.carvalhovilasboas@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.christophecvb.touchportal.samplekotlin

import com.christophecvb.touchportal.annotations.Action
import com.christophecvb.touchportal.annotations.Category
import com.christophecvb.touchportal.annotations.Plugin
import com.christophecvb.touchportal.helpers.PluginHelper
import com.christophecvb.touchportal.TouchPortalPlugin
import com.christophecvb.touchportal.annotations.Data
import com.christophecvb.touchportal.model.*
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
    override fun onNotificationOptionClicked(tpNotificationOptionClickedMessage: TPNotificationOptionClickedMessage) {}
}
