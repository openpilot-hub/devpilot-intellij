package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

/**
 * ollama设置.
 *
 * @author liliang
 * @date 2024/03/18
 */
@State(name = "DevPilot_OllamaSettings", storages = @Storage("DevPilot_OllamaSettings.xml"))
public class OllamaSettingsState implements PersistentStateComponent<OllamaSettingsState> {
    private String modelHost = "http://localhost:11434";

    private String modelName = "";

    public static OllamaSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(OllamaSettingsState.class);
    }

    public String getModelHost() {
        return modelHost;
    }

    public void setModelHost(String modelHost) {
        this.modelHost = modelHost;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public OllamaSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(OllamaSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
