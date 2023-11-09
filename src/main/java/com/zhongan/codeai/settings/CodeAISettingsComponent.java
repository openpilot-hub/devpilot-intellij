package com.zhongan.codeai.settings;

import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UI;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.settings.state.LanguageSettingsState;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import javax.swing.JPanel;

public class CodeAISettingsComponent {

    private final JPanel mainPanel;

    private final JBTextField fullNameField;

    private final CodeAIConfigForm codeAIConfigForm;

    public CodeAISettingsComponent(CodeAISettingsConfigurable codeAISettingsConfigurable, CodeAILlmSettingsState settings) {
        codeAIConfigForm = new CodeAIConfigForm(settings);

        fullNameField = new JBTextField(settings.getFullName(), 20);

        Integer languageIndex = LanguageSettingsState.getInstance().getLanguageIndex();

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(UI.PanelFactory.panel(fullNameField)
                .withLabel(CodeAIMessageBundle.get("codeai.setting.displayNameFieldLabel"))
                .resizeX(false)
                .createPanel())
            .addComponent(codeAIConfigForm.createLanguageSectionPanel(languageIndex))
            .addComponent(new TitledSeparator(CodeAIMessageBundle.get("codeai.settins.service.title")))
            .addComponent(codeAIConfigForm.getForm())
            .addVerticalGap(8)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public CodeAIConfigForm getCodeAIConfigForm() {
        return codeAIConfigForm;
    }

    // Getting the full name from the settings
    public String getFullName() {
        return fullNameField.getText();
    }

    public Integer getLanguageIndex() {
        return codeAIConfigForm.getLanguageIndex();
    }

}
