package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.zhongan.devpilot.enums.OpenAIModelNameEnum;

@State(name = "DevPilot_OpenAISettings", storages = @Storage("DevPilot_OpenAISettings.xml"))
public class OpenAISettingsState implements PersistentStateComponent<OpenAISettingsState> {
    private String modelHost;

    private String privateKey;

    private String modelName = OpenAIModelNameEnum.GPT3_5_TURBO.getName();

    private String customModelName;

    public static OpenAISettingsState getInstance() {
        return ApplicationManager.getApplication().getService(OpenAISettingsState.class);
    }

    public String getModelHost() {
        return modelHost;
    }

    public void setModelHost(String modelHost) {
        this.modelHost = modelHost;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getCustomModelName() {
        return customModelName;
    }

    public void setCustomModelName(String customModelName) {
        this.customModelName = customModelName;
    }

    @Override
    public OpenAISettingsState getState() {
        return this;
    }

    @Override
    public void loadState(OpenAISettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
