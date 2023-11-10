package com.zhongan.codeai.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.codeai.enums.ModelTypeEnum;
import com.zhongan.codeai.settings.state.LanguageSettingsState;
import com.zhongan.codeai.settings.state.OpenAISettingsState;
import com.zhongan.codeai.util.CodeAIMessageBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class CodeAIConfigForm {

    private final JPanel comboBoxPanel;

    private final ComboBox<ModelTypeEnum> modelComboBox;

    private final JPanel openAIServicePanel;

    private final JBTextField openAIBaseHostField;

    private Integer index;

    public CodeAIConfigForm() {
        var openAISettings = OpenAISettingsState.getInstance();

        var selectedModel = openAISettings.getSelectedModel();
        ModelTypeEnum selectedEnum = ModelTypeEnum.fromName(selectedModel);

        var host = openAISettings.getModelBaseHost(selectedModel);
        openAIBaseHostField = new JBTextField(host, 30);
        openAIServicePanel = createOpenAIServiceSectionPanel(
                CodeAIMessageBundle.get("codeai.settins.service.modelHostLabel"), openAIBaseHostField);

        var combo = new ComboBox<>(ModelTypeEnum.values());
        combo.setSelectedItem(selectedEnum);
        combo.addItemListener(e -> {
            var selected = (ModelTypeEnum) e.getItem();
            openAISettings.setSelectedModel(selected.getName());
            openAIBaseHostField.setText(openAISettings.getModelBaseHost(selected.getName()));
        });

        modelComboBox = combo;
        comboBoxPanel = createOpenAIServiceSectionPanel(
                CodeAIMessageBundle.get("codeai.settins.service.modelTypeLabel"), modelComboBox);

        var instance = LanguageSettingsState.getInstance();
        index = instance.getLanguageIndex();
    }

    public JComponent getForm() {
        var form = FormBuilder.createFormBuilder()
            .addComponent(comboBoxPanel)
            .addComponent(openAIServicePanel)
            .getPanel();
        form.setBorder(JBUI.Borders.emptyLeft(16));
        return form;
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
                        .withLabel(CodeAIMessageBundle.get("codeai.setting.language"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(0));
        return panel;
    }

    private JPanel createOpenAIServiceSectionPanel(String label, JComponent component) {
        var panel = UI.PanelFactory.grid()
            .add(UI.PanelFactory.panel(component)
                .withLabel(label)
                .resizeX(false))
            .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(16));
        return panel;
    }

    public String getOpenAIBaseHost() {
        return openAIBaseHostField.getText();
    }

    public ModelTypeEnum getSelectedModel() {
        return (ModelTypeEnum) modelComboBox.getSelectedItem();
    }

    public Integer getLanguageIndex() {
        return index;
    }
}
