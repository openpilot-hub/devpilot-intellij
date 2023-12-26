package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "DevPilot_TrialServiceSettings", storages = @Storage("DevPilot_TrialServiceSettings.xml"))
public class TrialServiceSettingsState implements PersistentStateComponent<TrialServiceSettingsState> {
    private String githubToken;

    private String githubUsername;

    private Long githubUserId;

    public static TrialServiceSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(TrialServiceSettingsState.class);
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public Long getGithubUserId() {
        return githubUserId;
    }

    public void setGithubUserId(Long githubUserId) {
        this.githubUserId = githubUserId;
    }

    @Override
    public TrialServiceSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(TrialServiceSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
