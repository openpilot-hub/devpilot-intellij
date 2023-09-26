package com.zhongan.codeai.settings;

import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.FormBuilder;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import javax.swing.JPanel;

public class CodeAISettingsComponent {

    private final JPanel mainPanel;

    private final CodeAIConfigForm codeAIConfigForm;

    public CodeAISettingsComponent(CodeAISettingsConfigurable codeAISettingsConfigurable, CodeAILlmSettingsState settings) {
        codeAIConfigForm = new CodeAIConfigForm(settings);
        mainPanel = FormBuilder.createFormBuilder()
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
}
