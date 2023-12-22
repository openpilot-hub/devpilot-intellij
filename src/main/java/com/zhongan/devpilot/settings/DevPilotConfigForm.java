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
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.settings.state.CodeLlamaSettingsState;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import com.zhongan.devpilot.settings.state.OpenAISettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;
import com.zhongan.devpilot.util.ZaSsoUtils;

import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.ide.BuiltInServerManager;

public class DevPilotConfigForm {

    private final JPanel comboBoxPanel;

    private final ComboBox<ModelServiceEnum> modelComboBox;

    private final JPanel openAIServicePanel;

    private final JBTextField openAIBaseHostField;

    private final JBTextField openAIKeyField;

    private final JPanel aiGatewayServicePanel;

    private final JBTextField aiGatewayBaseHostField;

    private final ComboBox<ModelTypeEnum> aiGatewayModelComboBox;

    private final JPanel codeLlamaServicePanel;

    private final JBTextField codeLlamaBaseHostField;

    private final ComboBox<ZaSsoEnum> zaSsoComboBox;

    private final JButton zaSsoButton;

    private final JBLabel userInfoLabel;

    private final JPanel loginPanel;

    private Integer index;

    public DevPilotConfigForm() {
        var devPilotSettings = DevPilotLlmSettingsState.getInstance();

        var selectedModel = devPilotSettings.getSelectedModel();
        ModelServiceEnum selectedEnum = ModelServiceEnum.fromName(selectedModel);

        var openAISettings = OpenAISettingsState.getInstance();
        openAIBaseHostField = new JBTextField(openAISettings.getModelHost(), 30);
        openAIKeyField = new JBTextField(openAISettings.getPrivateKey(), 30);
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

        var zaSsoUsername = zaSsoUsername();

        zaSsoButton = createZaSsoButton();
        if (isLogin() && StringUtils.isNotBlank(zaSsoUsername)) {
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
            if (isLogin()) {
                userInfoLabel.setText(DevPilotMessageBundle.get("devpilot.settings.service.welcome") + " " + zaSsoUsername());
                zaSsoButton.setText(DevPilotMessageBundle.get("devpilot.settings.service.logout"));
            } else {
                userInfoLabel.setText(null);
                zaSsoButton.setText(DevPilotMessageBundle.get("devpilot.settings.service.zaSsoDesc"));
            }
        });

        zaSsoComboBox = ssoComboBox;

        aiGatewayServicePanel = createAIGatewayServicePanel();

        var codeLlamaSettings = CodeLlamaSettingsState.getInstance();
        codeLlamaBaseHostField = new JBTextField(codeLlamaSettings.getModelHost(), 30);
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

    private JButton createZaSsoButton() {
        String showText = DevPilotMessageBundle.get("devpilot.settings.service.zaSsoDesc");

        if (isLogin()) {
            showText = DevPilotMessageBundle.get("devpilot.settings.service.logout");
        }

        JButton button = new JButton(showText);
        button.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (isLogin()) {
                    button.setText(DevPilotMessageBundle.get("devpilot.settings.service.zaSsoDesc"));
                    // logout clear user session info
                    logout();
                    userInfoLabel.setText("");
                } else {
                    String url = ZaSsoUtils.getZaSsoAuthUrl(getSelectedZaSso(), BuiltInServerManager.getInstance().getPort());
                    BrowserUtil.browse(url);
                }
            }
        });

        return button;
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

    private ZaSsoEnum getSelectedZaSso() {
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

    private boolean isLogin() {
        var settings = AIGatewaySettingsState.getInstance();
        var zaSsoEnum = getSelectedZaSso();
        switch (zaSsoEnum) {
            case ZA_TI:
                return StringUtils.isNotBlank(settings.getTiSsoToken()) && StringUtils.isNotBlank(settings.getTiSsoUsername());
            case ZA:
            default:
                return StringUtils.isNotBlank(settings.getSsoToken()) && StringUtils.isNotBlank(settings.getSsoUsername());
        }
    }

    private String zaSsoUsername() {
        var settings = AIGatewaySettingsState.getInstance();
        var zaSsoEnum = getSelectedZaSso();
        switch (zaSsoEnum) {
            case ZA_TI:
                return settings.getTiSsoUsername();
            case ZA:
            default:
                return settings.getSsoUsername();
        }
    }

    private void logout() {
        var settings = AIGatewaySettingsState.getInstance();
        var zaSsoEnum = getSelectedZaSso();
        switch (zaSsoEnum) {
            case ZA_TI:
                settings.setTiSsoUsername(null);
                settings.setTiSsoToken(null);
                break;
            case ZA:
            default:
                settings.setSsoUsername(null);
                settings.setSsoToken(null);
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

    public void zaSsoLogin(String token, String username) {
        var zaSsoEnum = getSelectedZaSso();
        switch (zaSsoEnum) {
            case ZA:
                AIGatewaySettingsState.getInstance().setSsoToken(token);
                AIGatewaySettingsState.getInstance().setSsoUsername(username);
                break;
            case ZA_TI:
                AIGatewaySettingsState.getInstance().setTiSsoToken(token);
                AIGatewaySettingsState.getInstance().setTiSsoUsername(username);
                break;
            default:
                break;
        }
        zaSsoButton.setText(DevPilotMessageBundle.get("devpilot.settings.service.logout"));
        userInfoLabel.setText(DevPilotMessageBundle.get("devpilot.settings.service.welcome") + " " + username);
    }

    public ZaSsoEnum getSelectedSso() {
        return (ZaSsoEnum) zaSsoComboBox.getSelectedItem();
    }
}
