package com.zhongan.devpilot.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.devpilot.enums.ModelServiceEnum;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.enums.OpenAIModelNameEnum;
import com.zhongan.devpilot.settings.state.*;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.OkhttpUtils;
import okhttp3.Call;
import okhttp3.Request;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class DevPilotConfigForm {

    private final JPanel comboBoxPanel;

    private final ComboBox<ModelServiceEnum> modelComboBox;

    private final JPanel openAIServicePanel;

    private final JBTextField openAIBaseHostField;

    private final JBTextField openAIKeyField;

    private final ComboBox<OpenAIModelNameEnum> openAIModelNameComboBox;

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
        var modelNameEnum = OpenAIModelNameEnum.fromName(openAISettings.getModelName());
        openAICustomModelNameField.setEnabled(modelNameEnum == OpenAIModelNameEnum.CUSTOM);
        openAIModelNameComboBox = new ComboBox<>(OpenAIModelNameEnum.values());
        openAIModelNameComboBox.setSelectedItem(modelNameEnum);
        openAIModelNameComboBox.addItemListener(e -> {
            var selected = (OpenAIModelNameEnum) e.getItem();
            openAICustomModelNameField.setEnabled(selected == OpenAIModelNameEnum.CUSTOM);
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
        refreshOllamaModels();
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
    }


    public void refreshOllamaModels() {
        ollamaModelComboBox.removeAllItems();
        // 请求ollama接口,获取模型列表
        String host = ollamaBaseHostField.getText();
        if (null == host || "".equals(host)) {
            JOptionPane.showMessageDialog(null, DevPilotMessageBundle.get("devpilot.settings.service.ollamaHostError"), DevPilotMessageBundle.get("devpilot.settings.service.dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length()-1);
        }
        try {
            var request = new Request.Builder()
                    .get()
                    .url(host + "/api/tags")
                    .build();
            Call call = OkhttpUtils.getClient().newCall(request);
            okhttp3.Response response = call.execute();
            if (response.isSuccessful()) {
                var result = Objects.requireNonNull(response.body()).string();
                JsonObject dataJson = JsonParser.parseString(result).getAsJsonObject();
                JsonArray modelArray = dataJson.get("models").getAsJsonArray();
                for (JsonElement modelEle : modelArray) {
                    String modelName = modelEle.getAsJsonObject().get("name").getAsString();
                    ollamaModelComboBox.addItem(modelName);
                }
            }
            response.close();
        } catch (Exception ex) {
            // throw new RuntimeException(ex);
            JOptionPane.showMessageDialog(null, DevPilotMessageBundle.get("devpilot.settings.service.ollamaHostError"), DevPilotMessageBundle.get("devpilot.settings.service.dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
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
        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(openAIBaseHostField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.modelHostLabel"))
                        .resizeX(false))
                .add(UI.PanelFactory.panel(openAIKeyField)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.apiKeyLabel"))
                        .resizeX(false))
                .add(UI.PanelFactory.panel(openAIModelNameComboBox)
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

    public OpenAIModelNameEnum getOpenAIModelName() {
        return (OpenAIModelNameEnum) openAIModelNameComboBox.getSelectedItem();
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
