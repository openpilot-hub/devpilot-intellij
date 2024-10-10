package com.zhongan.devpilot.settings.actionconfiguration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.ArrayList;
import java.util.List;

import static com.zhongan.devpilot.enums.EditorActionEnum.COMMENT_METHOD;
import static com.zhongan.devpilot.enums.EditorActionEnum.EXPLAIN_CODE;
import static com.zhongan.devpilot.enums.EditorActionEnum.FIX_CODE;
import static com.zhongan.devpilot.enums.EditorActionEnum.GENERATE_COMMENTS;
import static com.zhongan.devpilot.enums.EditorActionEnum.GENERATE_TESTS;

@State(
    name = "com.zhongan.devpilot.settings.actionconfiguration.EditorActionConfigurationState",
    storages = @Storage("DevPilotActionConfiguration.xml")
)
public class EditorActionConfigurationState implements PersistentStateComponent<EditorActionConfigurationState> {

    private final List<String> defaultActions;

    {
        defaultActions = new ArrayList<>();
        defaultActions.add(EXPLAIN_CODE.getLabel());
        defaultActions.add(FIX_CODE.getLabel());
        defaultActions.add(GENERATE_COMMENTS.getLabel());
        defaultActions.add(GENERATE_TESTS.getLabel());
        defaultActions.add(COMMENT_METHOD.getLabel());
    }

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

    public List<String> getDefaultActions() {
        return defaultActions;
    }

}
