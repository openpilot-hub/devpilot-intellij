package com.zhongan.codeai.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "OpenPilot_OpenAISettings", storages = @Storage("OpenPilot_OpenAISettings.xml"))
public class OpenAISettingsState implements PersistentStateComponent<OpenAISettingsState> {
    private String openAIBaseHost = "http://sky-gateway-test.zhonganonline.com";

    public static OpenAISettingsState getInstance() {
        return ApplicationManager.getApplication().getService(OpenAISettingsState.class);
    }

    public String getOpenAIBaseHost() {
        return openAIBaseHost;
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
