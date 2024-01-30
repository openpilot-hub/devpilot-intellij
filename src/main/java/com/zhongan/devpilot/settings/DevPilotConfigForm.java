package com.zhongan.devpilot.settings;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.zhongan.devpilot.enums.ModelServiceEnum;
import com.zhongan.devpilot.enums.ModelTypeEnum;
import com.zhongan.devpilot.enums.OpenAIModelNameEnum;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.OpenAISettingsState;
import com.zhongan.devpilot.settings.state.TrialServiceSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.WxAuthUtils;
import com.zhongan.devpilot.util.ZaSsoUtils;

import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

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

    private final ComboBox<ZaSsoEnum> zaSsoComboBox;

    private final JButton zaSsoButton;

    private final JBLabel userInfoLabel;

    private final JPanel loginPanel;

    private final JPanel trialServicePanel;

    private final JButton wxAuthButton;

    private final JBLabel wxUserInfoLabel;

    private final JPanel wxLoginPanel;

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

        var zaSsoUsername = ZaSsoUtils.zaSsoUsername(getSelectedZaSso());

        zaSsoButton = createZaSsoButton();
        if (ZaSsoUtils.isLogin(getSelectedZaSso()) && StringUtils.isNotBlank(zaSsoUsername)) {
            userInfoLabel = new JBLabel(DevPilotMessageBundle.get("devpilot.settings.service.welcome") + " " + zaSsoUsername);
        } else {
            userInfoLabel = new JBLabel();
        }

        var loginWrapper = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        loginWrapper.add(zaSsoButton);
        loginWrapper.add(Box.createHorizontalStrut(5));
        loginWrapper.add(userInfoLabel);

        loginPanel = loginWrapper;

        var ssoComboBox = new ComboBox<>(ZaSsoEnum.values());
        ssoComboBox.setSelectedItem(ZaSsoEnum.fromName(aiGatewaySettings.getSelectedSso()));
        ssoComboBox.addItemListener(e -> {
            switchSelectedSso();
        });

        zaSsoComboBox = ssoComboBox;

        aiGatewayServicePanel = createAIGatewayServicePanel();

        var codeLlamaSettings = CodeLlamaSettingsState.getInstance();
        codeLlamaBaseHostField = new JBTextField(codeLlamaSettings.getModelHost(), 30);
        codeLlamaModelNameField = new JBTextField(codeLlamaSettings.getModelName(), 30);
        codeLlamaServicePanel = createCodeLlamaServicePanel();

        var trialServiceSettings = TrialServiceSettingsState.getInstance();
        var username = trialServiceSettings.getWxUsername();

        wxAuthButton = createWxAuthButton();
        if (WxAuthUtils.isLogin()) {
            wxUserInfoLabel = new JBLabel(DevPilotMessageBundle.get("devpilot.settings.service.welcome") + " " + username);
        } else {
            wxUserInfoLabel = new JBLabel();
        }

        var wxLoginWrapper = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        wxLoginWrapper.add(wxAuthButton);
        wxLoginWrapper.add(Box.createHorizontalStrut(5));
        wxLoginWrapper.add(wxUserInfoLabel);

        wxLoginPanel = wxLoginWrapper;

        trialServicePanel = createTrialServicePanel();

        panelShow(selectedEnum);

        var combo = new ComboBox<>(ModelServiceEnum.values());
        combo.setSelectedItem(selectedEnum);
        combo.addItemListener(e -> {
            var selected = (ModelServiceEnum) e.getItem();
            panelShow(selected);
        });

        modelComboBox = combo;
        comboBoxPanel = createOpenAIServiceSectionPanel(
                DevPilotMessageBundle.get("devpilot.settings.service.serviceTypeLabel"), modelComboBox);

        var instance = LanguageSettingsState.getInstance();
        index = instance.getLanguageIndex();
    }

    public JComponent getForm() {
        var form = FormBuilder.createFormBuilder()
                .addComponent(comboBoxPanel)
                .addComponent(openAIServicePanel)
                .addComponent(codeLlamaServicePanel)
                .addComponent(aiGatewayServicePanel)
                .addComponent(trialServicePanel)
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

    private JButton createZaSsoButton() {
        String showText = DevPilotMessageBundle.get("devpilot.settings.service.zaSsoDesc");

        if (ZaSsoUtils.isLogin(getSelectedZaSso())) {
            showText = DevPilotMessageBundle.get("devpilot.settings.service.logout");
        }

        JButton button = new JButton(showText);
        button.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (ZaSsoUtils.isLogin(getSelectedZaSso())) {
                    button.setText(DevPilotMessageBundle.get("devpilot.settings.service.zaSsoDesc"));
                    // logout clear user session info
                    ZaSsoUtils.logout(getSelectedZaSso());
                    userInfoLabel.setText("");
                } else {
                    String url = ZaSsoUtils.getZaSsoAuthUrl(getSelectedZaSso());
                    BrowserUtil.browse(url);
                }
            }
        });

        return button;
    }

    private void switchSelectedSso() {
        if (ZaSsoUtils.isLogin(getSelectedZaSso())) {
            userInfoLabel.setText(DevPilotMessageBundle.get("devpilot.settings.service.welcome") + " " + ZaSsoUtils.zaSsoUsername(getSelectedZaSso()));
            zaSsoButton.setText(DevPilotMessageBundle.get("devpilot.settings.service.logout"));
        } else {
            userInfoLabel.setText(null);
            zaSsoButton.setText(DevPilotMessageBundle.get("devpilot.settings.service.zaSsoDesc"));
        }
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
                .add(UI.PanelFactory.panel(zaSsoComboBox)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.zaSsoLabel"))
                        .resizeX(false))
                .add(UI.PanelFactory.panel(loginPanel)
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

    private JPanel createTrialServicePanel() {
        var panel = UI.PanelFactory.grid()
                .add(UI.PanelFactory.panel(wxLoginPanel)
                        .withLabel(DevPilotMessageBundle.get("devpilot.settings.service.zaWxLabel"))
                        .resizeX(false))
                .createPanel();
        panel.setBorder(JBUI.Borders.emptyLeft(16));
        return panel;
    }

    private JButton createWxAuthButton() {
        String showText = DevPilotMessageBundle.get("devpilot.settings.service.zaWxDesc");

        if (WxAuthUtils.isLogin()) {
            showText = DevPilotMessageBundle.get("devpilot.settings.service.logout");
        }

        JButton button = new JButton(showText);
        button.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (WxAuthUtils.isLogin()) {
                    button.setText(DevPilotMessageBundle.get("devpilot.settings.service.zaWxDesc"));
                    // logout clear user session info
                    WxAuthUtils.logout();
                    wxUserInfoLabel.setText("");
                } else {
                    String url = WxAuthUtils.getWxAuthUrl();
                    BrowserUtil.browse(url);
                }
            }
        });

        return button;
    }

    private void panelShow(ModelServiceEnum serviceEnum) {
        switch (serviceEnum) {
            case OPENAI:
                openAIServicePanel.setVisible(true);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(false);
                trialServicePanel.setVisible(false);
                break;
            case LLAMA:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(true);
                aiGatewayServicePanel.setVisible(false);
                trialServicePanel.setVisible(false);
                break;
            case AIGATEWAY:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(true);
                trialServicePanel.setVisible(false);
                break;
            case TRIAL:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(false);
                trialServicePanel.setVisible(true);
                break;
            default:
                openAIServicePanel.setVisible(false);
                codeLlamaServicePanel.setVisible(false);
                aiGatewayServicePanel.setVisible(false);
                trialServicePanel.setVisible(false);
                break;
        }
    }

    public ZaSsoEnum getSelectedZaSso() {
        if (zaSsoComboBox == null) {
            var settings = AIGatewaySettingsState.getInstance();
            return ZaSsoEnum.fromName(settings.getSelectedSso());
        }

        var zaSsoEnum = (ZaSsoEnum) zaSsoComboBox.getSelectedItem();
        if (zaSsoEnum == null) {
            zaSsoEnum = ZaSsoEnum.ZA;
        }
        return zaSsoEnum;
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

    public void zaSsoLogin(ZaSsoEnum zaSsoEnum, String token, String username) {
        panelShow(ModelServiceEnum.AIGATEWAY);
        zaSsoComboBox.setSelectedItem(zaSsoEnum);
        switchSelectedSso();
        ZaSsoUtils.login(zaSsoEnum, token, username);
        zaSsoButton.setText(DevPilotMessageBundle.get("devpilot.settings.service.logout"));
        userInfoLabel.setText(DevPilotMessageBundle.get("devpilot.settings.service.welcome") + " " + username);
    }

    public void wxLogin(String username, String token, String userid) {
        panelShow(ModelServiceEnum.TRIAL);
        WxAuthUtils.login(token, username, userid);
        wxAuthButton.setText(DevPilotMessageBundle.get("devpilot.settings.service.logout"));
        wxUserInfoLabel.setText(DevPilotMessageBundle.get("devpilot.settings.service.welcome") + " " + username);
    }

    public OpenAIModelNameEnum getOpenAIModelName() {
        return (OpenAIModelNameEnum) openAIModelNameComboBox.getSelectedItem();
    }

    public String getOpenAICustomModelName() {
        return openAICustomModelNameField.getText();
    }

    public String getCodeLlamaModelName() {
        return codeLlamaModelNameField.getText();
    }
}
