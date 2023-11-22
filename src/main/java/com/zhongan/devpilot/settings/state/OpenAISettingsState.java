package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.zhongan.devpilot.enums.ModelTypeEnum;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@State(name = "DevPilot_OpenAISettings", storages = @Storage("DevPilot_OpenAISettings.xml"))
public class OpenAISettingsState implements PersistentStateComponent<OpenAISettingsState> {
    private String selectedModel = ModelTypeEnum.GPT3_5.getName();

    private Map<String, String> modelBaseHostMap = new ConcurrentHashMap<>();

    public static OpenAISettingsState getInstance() {
        return ApplicationManager.getApplication().getService(OpenAISettingsState.class);
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    public String getModelBaseHost(String selectedModel) {
        String openAIBaseHost = "http://openapi-cloud-pub.zhonganinfo.com";
        return modelBaseHostMap.getOrDefault(selectedModel, openAIBaseHost);
    }

    public void setModelBaseHost(String model, String host) {
        this.modelBaseHostMap.put(model, host);
    }

    public Map<String, String> getModelBaseHostMap() {
        return modelBaseHostMap;
    }

    public void setModelBaseHostMap(Map<String, String> modelBaseHostMap) {
        this.modelBaseHostMap = modelBaseHostMap;
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
