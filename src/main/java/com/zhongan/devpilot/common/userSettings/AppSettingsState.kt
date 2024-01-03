package com.zhongan.devpilot.common.userSettings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.zhongan.devpilot.common.general.Utils.replaceCustomRepository
import com.zhongan.devpilot.common.inline.render.GraphicsUtils

val settingsDefaultColor = GraphicsUtils.niceContrastColor.rgb

const val PROPERTIES_COMPONENT_NAME = "com.devpilot.enterprise-url"

/**
 * This package (`userSettings`) is heavily influenced by the docs from here:
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html
 *
 *
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(name = "com.devpilot.userSettings.AppSettingsState", storages = [Storage("DevpilotSettings.xml")])
class AppSettingsState : PersistentStateComponent<AppSettingsState?> {
    var useDefaultColor: Boolean = false
    var debounceTime: Long = 0
    private var colorState = settingsDefaultColor

    var inlineHintColor: Int
        get() = if (useDefaultColor) {
            settingsDefaultColor
        } else {
            colorState
        }
        set(value) {
            colorState = value
        }

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        private fun getInitialCloudUrlFromProperties(): String {
            val current = PropertiesComponent.getInstance().getValue(PROPERTIES_COMPONENT_NAME)
            return current ?: ""
        }

        @JvmStatic
        val instance: AppSettingsState
            get() = ApplicationManager.getApplication().getService(AppSettingsState::class.java)
    }
}
