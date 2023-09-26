package com.zhongan.codeai.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "CodeAI_Settings", storages = @Storage("CodeAI_Settings.xml"))
public class CodeAILlmSettingsState implements PersistentStateComponent<CodeAILlmSettingsState> {

    private boolean useOpenAIService = true;

    public static CodeAILlmSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(CodeAILlmSettingsState.class);
    }

    @Override
    public CodeAILlmSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(CodeAILlmSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean isUseOpenAIService() {
        return useOpenAIService;
    }
}
