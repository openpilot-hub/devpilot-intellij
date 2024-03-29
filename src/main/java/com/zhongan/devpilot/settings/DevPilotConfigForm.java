package com.zhongan.devpilot.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.devpilot.enums.ModelServiceEnum;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.integrations.llms.LlmProvider;
import com.zhongan.devpilot.integrations.llms.ollama.OllamaServiceProvider;
import com.zhongan.devpilot.integrations.llms.openai.OpenAIServiceProvider;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.OllamaSettingsState;
import com.zhongan.devpilot.settings.state.OpenAISettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class DevPilotConfigForm {

    private static final String CUSTOM_MODEL = "custom";

    private final JPanel comboBoxPanel;

    private final ComboBox<ModelServiceEnum> modelComboBox;

    private final JPanel openAIServicePanel;

    private final JBTextField openAIBaseHostField;

    private final JBTextField openAIKeyField;

    private final ComboBox<String> openAIModelNameComboBox;

    private final JButton openAIRefreshModelBtn;

    private final JBTextField openAICustomModelNameField;

    private final JPanel aiGatewayServicePanel;

    private final JBTextField aiGatewayBaseHostField;

    private final ComboBox<ModelTypeEnum> aiGatewayModelComboBox;

    private final JPanel codeLlamaServicePanel;

    private final JBTextField codeLlamaBaseHostField;

    private final JBTextField codeLlamaModelNameField;

    /**
     * ollama panel
     */
    private final JPanel ollamaServicePanel;

    private final JBTextField ollamaBaseHostField;

    private final JButton ollamaRefreshModelBtn;

    private final ComboBox<String> ollamaModelComboBox;

    private Integer index;

    public DevPilotConfigForm() {
        var devPilotSettings = DevPilotLlmSettingsState.getInstance();

        var selectedModel = devPilotSettings.getSelectedModel();
        ModelServiceEnum selectedEnum = ModelServiceEnum.fromName(selectedModel);

        var openAISettings = OpenAISettingsState.getInstance();
        openAIBaseHostField = new JBTextField(openAISettings.getModelHost(), 30);
        openAIKeyField = new JBTextField(openAISettings.getPrivateKey(), 30);
        openAICustomModelNameField = new JBTextField(openAISettings.getCustomModelName(), 15);
        var modelName = openAISettings.getModelName();
        openAICustomModelNameField.setEnabled(CUSTOM_MODEL.equals(modelName));
        openAIModelNameComboBox = new ComboBox<>();
        openAIModelNameComboBox.addItemListener(e -> {
            var selected = (String) e.getItem();
            openAICustomModelNameField.setEnabled(CUSTOM_MODEL.equals(selected));
        });
        openAIRefreshModelBtn = new JButton(DevPilotMessageBundle.get("devpilot.settings.service.refreshModelList"));
        openAIRefreshModelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                refreshOpenAIModels();
            }
        });

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

        var ollamaSettingsState = OllamaSettingsState.getInstance();
        ollamaBaseHostField = new JBTextField(ollamaSettingsState.getModelHost(), 30);
        ollamaRefreshModelBtn = new JButton(DevPilotMessageBundle.get("devpilot.settings.service.refreshModelList"));
        ollamaModelComboBox = new ComboBox<>();
        ollamaRefreshModelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                refreshOllamaModels();
            }
        });

        ollamaServicePanel = createOllamaServicePanel();

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

        refreshOpenAIModels();
        refreshOllamaModels();
    }

    public void refreshOllamaModels() {
        ollamaModelComboBox.removeAllItems();
        String host = ollamaBaseHostField.getText();
        if (null == host || "".equals(host)) {
            return;
        }

        LlmProvider llmProvider = ApplicationManager.getApplication().getService(OllamaServiceProvider.class);
        List<String> modelList = llmProvider.listModels(host, "");
        for (String modelName : modelList) {
            ollamaModelComboBox.addItem(modelName);
        }
        ollamaModelComboBox.setSelectedItem(OllamaSettingsState.getInstance().getModelName());
    }

    public void refreshOpenAIModels() {
        openAIModelNameComboBox.removeAllItems();
        String host = openAIBaseHostField.getText();
        String apiKey = openAIKeyField.getText();
        if (null == host || "".equals(host) || null == apiKey || "".equals(apiKey)) {
            openAIModelNameComboBox.addItem(CUSTOM_MODEL);
            return;
        }

        LlmProvider llmProvider = ApplicationManager.getApplication().getService(OpenAIServiceProvider.class);
        List<String> modelList = llmProvider.listModels(host, apiKey);
        for (String modelName : modelList) {
            openAIModelNameComboBox.addItem(modelName);
        }
        openAIModelNameComboBox.addItem(CUSTOM_MODEL);
        openAIModelNameComboBox.setSelectedItem(OpenAISettingsState.getInstance().getModelName());
    }

    public JComponent getForm() {
        var form = FormBuilder.createFormBuilder()
            .addComponent(comboBoxPanel)
            .addComponent(openAIServicePanel)
            .addComponent(codeLlamaServicePanel)
            .addComponent(aiGatewayServicePanel)
            .addComponent(ollamaServicePanel)
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
        JPanel modelPanel = new JPanel();
        modelPanel.setLayout(new BoxLayout(modelPanel, BoxLayout.X_AXIS));
        modelPanel.add(openAIModelNameComboBox);
        modelPanel.add(openAIRefreshModelBtn);

        var panel = UI.PanelFactory.grid()
            .add(UI.PanelFactory.panel(openAIBaseHostField)
                .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelHostLabel"))
                .resizeX(false))
            .add(UI.PanelFactory.panel(openAIKeyField)
                .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.apiKeyLabel"))
                .resizeX(false))
            .add(UI.PanelFactory.panel(modelPanel)
                .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelNameLabel"))
                .resizeX(false))
            .add(UI.PanelFactory.panel(openAICustomModelNameField)
                .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.customModelNameLabel"))
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

    private JPanel createOllamaServicePanel() {
        JPanel modelPanel = new JPanel();
        modelPanel.setLayout(new BoxLayout(modelPanel, BoxLayout.X_AXIS));
        modelPanel.add(ollamaModelComboBox);
        modelPanel.add(ollamaRefreshModelBtn);

        var panel = UI.PanelFactory.grid()
            .add(UI.PanelFactory.panel(ollamaBaseHostField)
                .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelHostLabel"))
                .resizeX(false))
            .add(UI.PanelFactory.panel(modelPanel)
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
                ollamaServicePanel.setVisible(false);
                break;
            case LLAMA:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(true);
                aiGatewayServicePanel.setVisible(false);
                ollamaServicePanel.setVisible(false);
                break;
            case AIGATEWAY:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(true);
                ollamaServicePanel.setVisible(false);
                break;
            case OLLAMA:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(false);
                ollamaServicePanel.setVisible(true);
                break;
            default:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(false);
                ollamaServicePanel.setVisible(false);
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
        String modelName = (String) openAIModelNameComboBox.getSelectedItem();
        return modelName == null ? "" : modelName;
    }

    public String getOllamaBaseHost() {
        return ollamaBaseHostField.getText();
    }

    public String getOllamaModelName() {
        String modelName = (String) ollamaModelComboBox.getSelectedItem();
        return modelName == null ? "" : modelName;
    }

    public String getOpenAICustomModelName() {
        return openAICustomModelNameField.getText();
    }

    public String getCodeLlamaModelName() {
        return codeLlamaModelNameField.getText();
    }
}
