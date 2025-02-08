package com.zhongan.devpilot.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.devpilot.settings.state.AvailabilityCheck;
import com.zhongan.devpilot.settings.state.ChatShortcutSettingState;
import com.zhongan.devpilot.settings.state.CompletionSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.LocalRagSettingsState;
import com.zhongan.devpilot.settings.state.PersonalAdvancedSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jetbrains.annotations.NotNull;

public class DevPilotSettingsComponent {

    private final JPanel mainPanel;

    private final JBTextField fullNameField;

    private final JBRadioButton localRagRadio;

    private final JBRadioButton autoCompletionRadio;

    private final JBRadioButton statusCheckRadio;

    private Integer index;

    private ComboBox<String> languageComboBox;

    private Integer gitLogLanguageIndex;

    private ComboBox<String> gitLogLanguageComboBox;

    private Integer methodInlayPresentationDisplayIndex;

    private ComboBox<String> methodInlayPresentationDisplayComboBox;

    private final JBTextField localStorageField;

    public DevPilotSettingsComponent(DevPilotSettingsConfigurable devPilotSettingsConfigurable, DevPilotLlmSettingsState settings) {
        fullNameField = new JBTextField(settings.getFullName(), 20);

        var instance = LanguageSettingsState.getInstance();
        index = instance.getLanguageIndex();

        Integer languageIndex = LanguageSettingsState.getInstance().getLanguageIndex();
        Integer logLanguageIndex = LanguageSettingsState.getInstance().getGitLogLanguageIndex();

        localRagRadio = new JBRadioButton(
                DevPilotMessageBundle.get("devpilot.settings.local.rag.desc"),
                LocalRagSettingsState.getInstance().getEnable());
        autoCompletionRadio = new JBRadioButton(
                DevPilotMessageBundle.get("devpilot.settings.service.code.completion.desc"),
                CompletionSettingsState.getInstance().getEnable());
        statusCheckRadio = new JBRadioButton(DevPilotMessageBundle.get("devpilot.settings.service.status.check.enable.desc"),
                AvailabilityCheck.getInstance().getEnable());

        methodInlayPresentationDisplayIndex = ChatShortcutSettingState.getInstance().getDisplayIndex();
        Integer inlayPresentationDisplayIndex = ChatShortcutSettingState.getInstance().getDisplayIndex();

        var personalAdvancedSettings = PersonalAdvancedSettingsState.getInstance();
        localStorageField = new JBTextField(personalAdvancedSettings.getLocalStorage(), 20);

        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(UI.PanelFactory.panel(fullNameField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.setting.displayNameFieldLabel"))
                        .resizeX(false)
                        .createPanel())
                .addComponent(createLanguageSectionPanel(languageIndex))
                .addComponent(createGitLogLanguageSectionPanel(logLanguageIndex))
                .addComponent(createMethodShortcutDisplayModeSectionPanel(inlayPresentationDisplayIndex))
                .addComponent(UI.PanelFactory.panel(localStorageField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.localStorageLabel"))
                        .resizeX(false)
                        .createPanel())
                .addComponent(new TitledSeparator(
                        DevPilotMessageBundle.get("devpilot.settings.local.rag.title")))
                .addComponent(localRagRadio)
                .addComponent(createTextArea(DevPilotMessageBundle.get("devpilot.settings.local.rag.explain")))
                .addVerticalGap(8)
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

    private JComponent createTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setBackground(null);
        textArea.setBorder(null);
        return textArea;
    }

    private @NotNull JComponent createMethodShortcutDisplayModeSectionPanel(Integer inlayPresentationDisplayIndex) {
        methodInlayPresentationDisplayComboBox = new ComboBox<>();
        methodInlayPresentationDisplayComboBox.addItem(DevPilotMessageBundle.get("devpilot.settings.methodShortcutHidden"));
        methodInlayPresentationDisplayComboBox.addItem(DevPilotMessageBundle.get("devpilot.settings.methodShortcutInlineDisplay"));
        methodInlayPresentationDisplayComboBox.addItem(DevPilotMessageBundle.get("devpilot.settings.methodShortcutGroupDisplay"));
        methodInlayPresentationDisplayComboBox.setSelectedIndex(inlayPresentationDisplayIndex);

        methodInlayPresentationDisplayComboBox.addActionListener(e -> {
            var box = (ComboBox<?>) e.getSource();
            methodInlayPresentationDisplayIndex = box.getSelectedIndex();
        });

        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(methodInlayPresentationDisplayComboBox)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.methodShortcutDisplayModeLabel"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(0));
        return panel;
    }

    public JPanel createLanguageSectionPanel(Integer languageIndex) {
        languageComboBox = new ComboBox<>();
        languageComboBox.addItem("English");
        languageComboBox.addItem("中文");
        languageComboBox.setSelectedIndex(languageIndex);

        languageComboBox.addActionListener(e -> {
            var box = (ComboBox<?>) e.getSource();
            index = box.getSelectedIndex();
        });

        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(languageComboBox)
                        .withLabel(DevPilotMessageBundle.get("devpilot.setting.language"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(0));
        return panel;
    }

    public JPanel createGitLogLanguageSectionPanel(Integer logLanguageIndex) {
        gitLogLanguageComboBox = new ComboBox<>();
        gitLogLanguageComboBox.addItem("English");
        gitLogLanguageComboBox.addItem("中文");
        gitLogLanguageComboBox.setSelectedIndex(logLanguageIndex);

        gitLogLanguageComboBox.addActionListener(e -> {
            var box = (ComboBox<?>) e.getSource();
            gitLogLanguageIndex = box.getSelectedIndex();
        });

        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(gitLogLanguageComboBox)
                        .withLabel(DevPilotMessageBundle.get("devpilot.setting.gitlog.language"))
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

    public Integer getGitLogLanguageIndex() {
        return gitLogLanguageIndex;
    }

    public Integer getMethodInlayPresentationDisplayIndex() {
        return methodInlayPresentationDisplayIndex;
    }

    public boolean getCompletionEnabled() {
        return autoCompletionRadio.isSelected();
    }

    public boolean getLocalRagEnabled() {
        return localRagRadio.isSelected();
    }

    public boolean getStatusCheckEnabled() {
        return statusCheckRadio.isSelected();
    }

    public String getLocalStoragePath() {
        return localStorageField.getText();
    }

    // For reset
    public void setFullName(String text) {
        fullNameField.setText(text);
    }

    public void setLanguageIndex(Integer index) {
        this.index = index;
        languageComboBox.setSelectedIndex(index);
    }

    public void setGitLogLanguageIndex(Integer gitLogLanguageIndex) {
        this.gitLogLanguageIndex = gitLogLanguageIndex;
        gitLogLanguageComboBox.setSelectedIndex(gitLogLanguageIndex);
    }

    public void setMethodInlayPresentationDisplayIndex(Integer methodInlayPresentationDisplayIndex) {
        this.methodInlayPresentationDisplayIndex = methodInlayPresentationDisplayIndex;
        methodInlayPresentationDisplayComboBox.setSelectedIndex(methodInlayPresentationDisplayIndex);
    }

    public void setCompletionEnabled(boolean selected) {
        autoCompletionRadio.setSelected(selected);
    }

    public void setStatusCheckEnabled(boolean selected) {
        statusCheckRadio.setSelected(selected);
    }

    public void setLocalRagRadioEnabled(boolean selected) {
        localRagRadio.setSelected(selected);
    }

    public void setLocalStoragePath(String text) {
        localStorageField.setText(text);
    }
}
