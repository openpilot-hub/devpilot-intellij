package com.zhongan.devpilot.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.devpilot.enums.ModelServiceEnum;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.OpenAISettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class DevPilotConfigForm {

    private final JPanel comboBoxPanel;

    private final ComboBox<ModelServiceEnum> modelComboBox;

    private final JPanel openAIServicePanel;

    private final JBTextField openAIBaseHostField;

    private final JBTextField openAIKeyField;

    private final JBTextField openAIModelNameField;

    private final JPanel aiGatewayServicePanel;

    private final JBTextField aiGatewayBaseHostField;

    private final ComboBox<ModelTypeEnum> aiGatewayModelComboBox;

    private final JPanel codeLlamaServicePanel;

    private final JBTextField codeLlamaBaseHostField;

    private final JBTextField codeLlamaModelNameField;

    private Integer index;

    public DevPilotConfigForm() {
        var devPilotSettings = DevPilotLlmSettingsState.getInstance();

        var selectedModel = devPilotSettings.getSelectedModel();
        ModelServiceEnum selectedEnum = ModelServiceEnum.fromName(selectedModel);

        var openAISettings = OpenAISettingsState.getInstance();
        openAIBaseHostField = new JBTextField(openAISettings.getModelHost(), 30);
        openAIKeyField = new JBTextField(openAISettings.getPrivateKey(), 30);
        openAIModelNameField = new JBTextField(openAISettings.getModelName(), 30);
        openAIServicePanel = createOpenAIServicePanel();

        var aiGatewaySettings = AIGatewaySettingsState.getInstance();
        var aiGatewayModel = aiGatewaySettings.getSelectedModel();
        var aiGatewayModelEnum = ModelTypeEnum.fromName(aiGatewayModel);
        var host = aiGatewaySettings.getModelBaseHost(aiGatewayModel);
        aiGatewayBaseHostField = new JBTextField(host, 30);
        var modelTypeEnumComboBox = new ComboBox<>(ModelTypeEnum.values());
        modelTypeEnumComboBox.setSelectedItem(aiGatewayModelEnum);
        modelTypeEnumComboBox.addItemListener(e -> {
            var selected = (ModelTypeEnum) e.getItem();
            aiGatewayBaseHostField.setText(aiGatewaySettings.getModelBaseHost(selected.getName()));
        });
        aiGatewayModelComboBox = modelTypeEnumComboBox;
        aiGatewayServicePanel = createAIGatewayServicePanel();

        var codeLlamaSettings = CodeLlamaSettingsState.getInstance();
        codeLlamaBaseHostField = new JBTextField(codeLlamaSettings.getModelHost(), 30);
        codeLlamaModelNameField = new JBTextField(codeLlamaSettings.getModelName(), 30);
        codeLlamaServicePanel = createCodeLlamaServicePanel();

        panelShow(selectedEnum);

        var combo = new ComboBox<>(ModelServiceEnum.values());
        combo.setSelectedItem(selectedEnum);
        combo.addItemListener(e -> {
            var selected = (ModelServiceEnum) e.getItem();
            panelShow(selected);
        });

        modelComboBox = combo;
        comboBoxPanel = createOpenAIServiceSectionPanel(
                DevPilotMessageBundle.get("devpilot.settings.service.modelTypeLabel"), modelComboBox);

        var instance = LanguageSettingsState.getInstance();
        index = instance.getLanguageIndex();
    }

    public JComponent getForm() {
        var form = FormBuilder.createFormBuilder()
            .addComponent(comboBoxPanel)
            .addComponent(openAIServicePanel)
            .addComponent(codeLlamaServicePanel)
            .addComponent(aiGatewayServicePanel)
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
                        .withLabel(DevPilotMessageBundle.get("devpilot.setting.language"))
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

    private JPanel createOpenAIServicePanel() {
        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(openAIBaseHostField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelHostLabel"))
                        .resizeX(false))
                .add(UI.PanelFactory.panel(openAIKeyField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.apiKeyLabel"))
                        .resizeX(false))
                .add(UI.PanelFactory.panel(openAIModelNameField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelNameLabel"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(16));
        return panel;
    }

    private JPanel createAIGatewayServicePanel() {
        var panel = UI.PanelFactory.grid()
            .add(UI.PanelFactory.panel(aiGatewayBaseHostField)
                .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelHostLabel"))
                .resizeX(false))
            .add(UI.PanelFactory.panel(aiGatewayModelComboBox)
                .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelTypeLabel"))
                .resizeX(false))
            .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(16));
        return panel;
    }

    private JPanel createCodeLlamaServicePanel() {
        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(codeLlamaBaseHostField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelHostLabel"))
                        .resizeX(false))
                .add(UI.PanelFactory.panel(codeLlamaModelNameField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelNameLabel"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(16));
        return panel;
    }

    private void panelShow(ModelServiceEnum serviceEnum) {
        switch (serviceEnum) {
            case OPENAI:
                openAIServicePanel.setVisible(true);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(false);
                break;
            case LLAMA:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(true);
                aiGatewayServicePanel.setVisible(false);
                break;
            case AIGATEWAY:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(true);
                break;
            default:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(false);
                break;
        }
    }

    public String getOpenAIBaseHost() {
        return openAIBaseHostField.getText();
    }

    public String getOpenAIKey() {
        return openAIKeyField.getText();
    }

    public String getCodeLlamaBaseHost() {
        return codeLlamaBaseHostField.getText();
    }

    public ModelServiceEnum getSelectedModel() {
        return (ModelServiceEnum) modelComboBox.getSelectedItem();
    }

    public Integer getLanguageIndex() {
        return index;
    }

    public String getAIGatewayBaseHost() {
        return aiGatewayBaseHostField.getText();
    }

    public ModelTypeEnum getAIGatewayModel() {
        return (ModelTypeEnum) aiGatewayModelComboBox.getSelectedItem();
    }

    public String getOpenAIModelName() {
        return openAIModelNameField.getText();
    }

    public String getCodeLlamaModelName() {
        return codeLlamaModelNameField.getText();
    }
}
