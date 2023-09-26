package com.zhongan.codeai.settings.actionconfiguration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.LinkedHashMap;
import java.util.Map;

@State(
    name = "com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState",
    storages = @Storage("CodeAIActionConfiguration.xml")
)
public class EditorActionConfigurationState implements PersistentStateComponent<EditorActionConfigurationState> {

    private Map<String, String> defaultActions = new LinkedHashMap<>(Map.of(
        "Generate Tests", "Generate tests for the selected code {{selectedCode}}",
        "Generate Docs", "Generate docs for the selected code {{selectedCode}}",
        "Fix This", "Find bugs in the selected code {{selectedCode}}",
        "Explain This", "Explain the selected code {{selectedCode}}"));

    public static EditorActionConfigurationState getInstance() {
        return ApplicationManager.getApplication().getService(EditorActionConfigurationState.class);
    }

    @Override
    public EditorActionConfigurationState getState() {
        return this;
    }

    @Override
    public void loadState(EditorActionConfigurationState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public Map<String, String> getDefaultActions() {
        return defaultActions;
    }

}
