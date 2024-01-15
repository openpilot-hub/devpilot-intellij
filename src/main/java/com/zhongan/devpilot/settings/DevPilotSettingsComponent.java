package com.zhongan.devpilot.settings;

import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UI;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import javax.swing.JPanel;

public class DevPilotSettingsComponent {

    private final JPanel mainPanel;

    private final JBTextField fullNameField;

    private final DevPilotConfigForm devPilotConfigForm;

    private final JBRadioButton autoCompletionRadio;

    public DevPilotSettingsComponent(DevPilotSettingsConfigurable devPilotSettingsConfigurable, DevPilotLlmSettingsState settings) {
        devPilotConfigForm = new DevPilotConfigForm();

        fullNameField = new JBTextField(settings.getFullName(), 20);

        Integer languageIndex = LanguageSettingsState.getInstance().getLanguageIndex();

        autoCompletionRadio = new JBRadioButton(
                DevPilotMessageBundle.get("devpilot.settings.service.code.completion.desc"),
                CompletionSettingsState.getInstance().getEnable());

        //todo auto completioncompletion

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(UI.PanelFactory.panel(fullNameField)
                .withLabel(DevPilotMessageBundle.get("devpilot.setting.displayNameFieldLabel"))
                .resizeX(false)
                .createPanel())
            .addComponent(devPilotConfigForm.createLanguageSectionPanel(languageIndex))
            .addComponent(new TitledSeparator(
                    DevPilotMessageBundle.get("devpilot.settings.service.code.completion.title")))
            .addComponent(autoCompletionRadio)
            .addComponent(new TitledSeparator(DevPilotMessageBundle.get("devpilot.settings.service.title")))
            .addComponent(devPilotConfigForm.getForm())
            .addVerticalGap(8)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public DevPilotConfigForm getDevPilotConfigForm() {
        return devPilotConfigForm;
    }

    // Getting the full name from the settings
    public String getFullName() {
        return fullNameField.getText();
    }

    public Integer getLanguageIndex() {
        return devPilotConfigForm.getLanguageIndex();
    }

    public boolean getCompletionEnabled() {
        return autoCompletionRadio.isSelected();
    }
}
