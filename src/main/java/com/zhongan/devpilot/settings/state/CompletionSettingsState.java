//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.zhongan.devpilot.statusBar.CompletionsStateNotifier.Companion;

@State(
        name = "DevPilot_CompletionSettings",
        storages = {@Storage("DevPilot_CompletionSettings.xml")}
)
public class CompletionSettingsState implements PersistentStateComponent<CompletionSettingsState> {
    private static Boolean enable;

    public CompletionSettingsState() {
    }

    public static CompletionSettingsState getInstance() {
        return (CompletionSettingsState)ApplicationManager.getApplication().getService(CompletionSettingsState.class);
    }

    public Boolean getEnable() {
        return enable == null ? true : enable;
    }

    public void setEnable(Boolean enable) {
        CompletionSettingsState.enable = enable;
        Companion.publish(enable);
    }

    public CompletionSettingsState getState() {
        return this;
    }

    public void loadState(CompletionSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
