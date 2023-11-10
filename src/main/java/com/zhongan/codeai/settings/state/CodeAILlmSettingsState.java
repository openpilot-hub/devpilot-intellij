package com.zhongan.codeai.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.UUID;

@State(name = "OpenPilot_Settings", storages = @Storage("OpenPilot_Settings.xml"))
public class CodeAILlmSettingsState implements PersistentStateComponent<CodeAILlmSettingsState> {

    private String fullName = "User";

    private String uuid;

    public static CodeAILlmSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(CodeAILlmSettingsState.class);
    }

    public String getUuid() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
        }

        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public CodeAILlmSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(CodeAILlmSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // getting fullName
    public String getFullName() {
        if (fullName == null || fullName.isEmpty()) {
            return System.getProperty("user.name", "User");
        }
        return fullName;
    }

    public void setFullName(String displayName) {
        this.fullName = displayName;
    }

}
