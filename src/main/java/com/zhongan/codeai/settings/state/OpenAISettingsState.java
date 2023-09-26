package com.zhongan.codeai.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "CodeAI_OpenAISettings", storages = @Storage("CodeAI_OpenAISettings.xml"))
public class OpenAISettingsState implements PersistentStateComponent<OpenAISettingsState> {
    private String openAIBaseHost = "https://codeai.zhongan.com";

    public String getOpenAIBaseHost() {
        return openAIBaseHost;
    }

    public static OpenAISettingsState getInstance() {
        return ApplicationManager.getApplication().getService(OpenAISettingsState.class);
    }

    public void setOpenAIBaseHost(String openAIBaseHost) {
        this.openAIBaseHost = openAIBaseHost;
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
