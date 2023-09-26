package com.zhongan.codeai.settings;

import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.codeai.settings.state.CodeAILlmSettingsState;
import com.zhongan.codeai.settings.state.OpenAISettingsState;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class CodeAIConfigForm {

    private final JBRadioButton useOpenAIServiceRadioButton;

    private final JPanel openAIServicePanel;

    private final JBTextField openAIBaseHostField;

    public CodeAIConfigForm(CodeAILlmSettingsState settingsState) {
        var openAISettings = OpenAISettingsState.getInstance();
        useOpenAIServiceRadioButton = new JBRadioButton(
            CodeAIMessageBundle.get("codeai.settins.service.useOpenAIServiceRadioButtonLabel"), settingsState.isUseOpenAIService());

        openAIBaseHostField = new JBTextField(openAISettings.getOpenAIBaseHost(), 30);
        openAIServicePanel = createOpenAIServiceSectionPanel();
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

    private void registerPanelsVisibility(CodeAILlmSettingsState settings) {
        openAIServicePanel.setVisible(settings.isUseOpenAIService());
        // enforce using open ai api
        useOpenAIServiceRadioButton.addChangeListener(e -> useOpenAIServiceRadioButton.setSelected(true));
    }

    public String getOpenAIBaseHost() {
        return openAIBaseHostField.getText();
    }

}
