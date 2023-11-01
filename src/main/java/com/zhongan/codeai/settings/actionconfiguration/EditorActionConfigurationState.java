package com.zhongan.codeai.settings.actionconfiguration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.zhongan.codeai.enums.EditorActionEnum.EXPLAIN_THIS;
import static com.zhongan.codeai.enums.EditorActionEnum.FIX_THIS;
import static com.zhongan.codeai.enums.EditorActionEnum.GENERATE_COMMENTS;
import static com.zhongan.codeai.enums.EditorActionEnum.GENERATE_TESTS;
import static com.zhongan.codeai.enums.EditorActionEnum.PERFORMANCE_CHECK;
import static com.zhongan.codeai.enums.EditorActionEnum.REVIEW_CODE;

@State(
    name = "com.zhongan.codeai.settings.actionconfiguration.EditorActionConfigurationState",
    storages = @Storage("CodeAIActionConfiguration.xml")
)
public class EditorActionConfigurationState implements PersistentStateComponent<EditorActionConfigurationState> {

    private final Map<String, String> defaultActions = new LinkedHashMap<>(Map.of(
        PERFORMANCE_CHECK.getLabel(), PERFORMANCE_CHECK.getPrompt(),
        GENERATE_COMMENTS.getLabel(), GENERATE_COMMENTS.getPrompt(),
        GENERATE_TESTS.getLabel(), GENERATE_TESTS.getPrompt(),
        FIX_THIS.getLabel(), FIX_THIS.getPrompt(),
        EXPLAIN_THIS.getLabel(), EXPLAIN_THIS.getPrompt(),
        REVIEW_CODE.getLabel(), REVIEW_CODE.getPrompt()
    ));

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
