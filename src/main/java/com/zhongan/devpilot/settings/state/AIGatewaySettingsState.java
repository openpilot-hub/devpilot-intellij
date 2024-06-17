package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.enums.ZaSsoEnum;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.zhongan.devpilot.constant.DefaultConst.AI_GATEWAY_DEFAULT_HOST;

@State(name = "DevPilot_AIGatewaySettings", storages = @Storage("DevPilot_AIGatewaySettings.xml"))
public class AIGatewaySettingsState implements PersistentStateComponent<AIGatewaySettingsState> {
    private String selectedModel = ModelTypeEnum.GPT3_5.getName();

    private Map<String, String> modelBaseHostMap = new ConcurrentHashMap<>();

    @Deprecated
    private String selectedSso = ZaSsoEnum.ZA.getName();

    // za
    private String ssoToken;

    private String ssoUsername;

    // za_ti
    private String tiSsoToken;

    private String tiSsoUsername;

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
        return AI_GATEWAY_DEFAULT_HOST;
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

    public String getSelectedSso() {
        return selectedSso;
    }

    public void setSelectedSso(String selectedSso) {
        this.selectedSso = selectedSso;
    }

    public String getSsoToken() {
        return ssoToken;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }

    public String getSsoUsername() {
        return ssoUsername;
    }

    public void setSsoUsername(String ssoUsername) {
        this.ssoUsername = ssoUsername;
    }

    public String getTiSsoToken() {
        return tiSsoToken;
    }

    public void setTiSsoToken(String tiSsoToken) {
        this.tiSsoToken = tiSsoToken;
    }

    public String getTiSsoUsername() {
        return tiSsoUsername;
    }

    public void setTiSsoUsername(String tiSsoUsername) {
        this.tiSsoUsername = tiSsoUsername;
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
