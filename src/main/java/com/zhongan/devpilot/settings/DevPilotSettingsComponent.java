package com.zhongan.devpilot.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.StatusCheckSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import javax.swing.JPanel;

public class DevPilotSettingsComponent {

    private final JPanel mainPanel;

    private final JBTextField fullNameField;

    private final JBRadioButton autoCompletionRadio;

    private final JBRadioButton statusCheckRadio;

    private Integer index;

    public DevPilotSettingsComponent(DevPilotSettingsConfigurable devPilotSettingsConfigurable, DevPilotLlmSettingsState settings) {
        fullNameField = new JBTextField(settings.getFullName(), 20);

        var instance = LanguageSettingsState.getInstance();
        index = instance.getLanguageIndex();

        Integer languageIndex = LanguageSettingsState.getInstance().getLanguageIndex();

        autoCompletionRadio = new JBRadioButton(
                DevPilotMessageBundle.get("devpilot.settings.service.code.completion.desc"),
                CompletionSettingsState.getInstance().getEnable());
        statusCheckRadio = new JBRadioButton(DevPilotMessageBundle.get("devpilot.settings.service.status.check.enable.desc"),
                StatusCheckSettingsState.getInstance().getEnable());

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(UI.PanelFactory.panel(fullNameField)
                .withLabel(DevPilotMessageBundle.get("devpilot.setting.displayNameFieldLabel"))
                .resizeX(false)
                .createPanel())
            .addComponent(createLanguageSectionPanel(languageIndex))
            .addComponent(new TitledSeparator(
                    DevPilotMessageBundle.get("devpilot.settings.service.code.completion.title")))
            .addComponent(autoCompletionRadio)
            .addVerticalGap(8)

            .addComponent(new TitledSeparator(
                    DevPilotMessageBundle.get("devpilot.settings.service.status.check.title")))
            .addComponent(statusCheckRadio)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    public JPanel createLanguageSectionPanel(Integer languageIndex) {
        var comboBox = new ComboBox<>();
        comboBox.addItem("English");
        comboBox.addItem("中文");
        comboBox.setSelectedIndex(languageIndex);

        comboBox.addActionListener(e -> {
            var box = (ComboBox<?>) e.getSource();
            index = box.getSelectedIndex();
        });

        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(comboBox)
                        .withLabel(DevPilotMessageBundle.get("devpilot.setting.language"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(0));
        return panel;
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    // Getting the full name from the settings
    public String getFullName() {
        return fullNameField.getText();
    }

    public Integer getLanguageIndex() {
        return index;
    }

    public boolean getCompletionEnabled() {
        return autoCompletionRadio.isSelected();
    }

    public boolean getStatusCheckEnabled() {
        return statusCheckRadio.isSelected();
    }

}
