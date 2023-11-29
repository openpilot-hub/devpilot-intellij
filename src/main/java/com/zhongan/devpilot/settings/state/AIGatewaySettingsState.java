package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.zhongan.devpilot.enums.ModelTypeEnum;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@State(name = "DevPilot_AIGatewaySettings", storages = @Storage("DevPilot_AIGatewaySettings.xml"))
public class AIGatewaySettingsState implements PersistentStateComponent<AIGatewaySettingsState> {
    private String selectedModel = ModelTypeEnum.GPT3_5.getName();

    private Map<String, String> modelBaseHostMap = new ConcurrentHashMap<>();

    public static AIGatewaySettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AIGatewaySettingsState.class);
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    public String getModelBaseHost(String selectedModel) {
        String openAIBaseHost = "";
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
    public AIGatewaySettingsState getState() {
        return this;
    }

    @Override
    public void loadState(AIGatewaySettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
