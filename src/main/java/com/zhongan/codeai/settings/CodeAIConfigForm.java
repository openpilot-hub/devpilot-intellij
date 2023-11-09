package com.zhongan.codeai.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.settings.state.LanguageSettingsState;
import com.zhongan.codeai.settings.state.OpenAISettingsState;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class CodeAIConfigForm {

    private final JBRadioButton useOpenAIServiceRadioButton;

    private final JPanel openAIServicePanel;

    private final JBTextField openAIBaseHostField;

    private Integer index;

    public CodeAIConfigForm(CodeAILlmSettingsState settingsState) {
        var openAISettings = OpenAISettingsState.getInstance();
        var instance = LanguageSettingsState.getInstance();
        useOpenAIServiceRadioButton = new JBRadioButton(
            CodeAIMessageBundle.get("codeai.settins.service.useOpenAIServiceRadioButtonLabel"), settingsState.isUseOpenAIService());
        openAIBaseHostField = new JBTextField(openAISettings.getOpenAIBaseHost(), 30);
        openAIServicePanel = createOpenAIServiceSectionPanel();
        index = instance.getLanguageIndex();
        registerPanelsVisibility(settingsState);
    }

    public JComponent getForm() {
        var form = FormBuilder.createFormBuilder()
            .addComponent(useOpenAIServiceRadioButton)
            .addComponent(openAIServicePanel)
            .getPanel();
        form.setBorder(JBUI.Borders.emptyLeft(16));
        return form;
    }

    private JPanel createOpenAIServiceSectionPanel() {
        var panel = UI.PanelFactory.grid()
            .add(UI.PanelFactory.panel(openAIBaseHostField)
                .withLabel(CodeAIMessageBundle.get("codeai.settins.service.openAIServiceHost"))
                .resizeX(false))
            .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(16));
        return panel;
    }

    public JPanel createLanguageSectionPanel(Integer languageIndex) {
        ComboBox comboBox = new ComboBox();
        comboBox.addItem("English");
        comboBox.addItem("中文");
        comboBox.setSelectedIndex(languageIndex);

        comboBox.addActionListener(e -> {
            ComboBox box = (ComboBox) e.getSource();
            index = box.getSelectedIndex();
        });

        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(comboBox)
                        .withLabel(CodeAIMessageBundle.get("codeai.setting.language"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(0));
        return panel;
    }

    private void registerPanelsVisibility(CodeAILlmSettingsState settings) {
        openAIServicePanel.setVisible(settings.isUseOpenAIService());
        // enforce using open ai api
        useOpenAIServiceRadioButton.addChangeListener(e -> useOpenAIServiceRadioButton.setSelected(true));
    }

    public String getOpenAIBaseHost() {
        return openAIBaseHostField.getText();
    }

    public Integer getLanguageIndex() {
        return index;
    }

}
