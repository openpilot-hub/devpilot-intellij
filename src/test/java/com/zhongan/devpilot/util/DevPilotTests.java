package com.zhongan.devpilot.util;

import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.ui.components.JBScrollPane;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindow;
import com.zhongan.devpilot.gui.toolwindows.components.ChatDisplayPanel;
import com.zhongan.devpilot.gui.toolwindows.components.ContentComponent;
import com.zhongan.devpilot.gui.toolwindows.components.UserChatPanel;
import com.zhongan.devpilot.settings.state.LanguageSettingsState;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class DevPilotTests {

    @SuppressWarnings("unchecked")
    public static <T> T getFirst(Container container) {
        if (container != null && container.getComponentCount() > 0) {
            return (T) container.getComponent(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getLast(Container container) {
        if (container != null &&  container.getComponentCount() > 0) {
            return (T) container.getComponent(container.getComponentCount() - 1);
        }
        return null;
    }

    public static UserChatPanel getUserChatPanel(@NotNull DevPilotChatToolWindow mockedChatToolWindow) {
        JPanel chatWindowPanel = mockedChatToolWindow.getDevPilotChatToolWindowPanel();
        return getLast(chatWindowPanel);
    }

    public static ScrollablePanel getChatContentPanel(@NotNull DevPilotChatToolWindow mockedChatToolWindow) {
        JPanel chatWindowPanel = mockedChatToolWindow.getDevPilotChatToolWindowPanel();
        JBScrollPane scrollPane = getFirst(chatWindowPanel);
        if (scrollPane != null) {
            JViewport viewport = scrollPane.getViewport();
            return getFirst(viewport);
        }
        return null;
    }

    public static ContentComponent getLastContentComponent(@NotNull DevPilotChatToolWindow mockedChatToolWindow) {
        ScrollablePanel chatContentPanel = getChatContentPanel(mockedChatToolWindow);
        if (chatContentPanel != null) {
            ChatDisplayPanel chatDisplayPanel = getLast(chatContentPanel);
            JPanel responseInfoPanel = getLast(chatDisplayPanel);
            return getFirst(responseInfoPanel);
        }
        return null;
    }

    @NotNull
    public static String emplacePrompt(@NotNull EditorActionEnum actionEnum, @NotNull String selectedText) {
        String exceptedPrompt = actionEnum.getPrompt().replace("{{selectedCode}}", selectedText);
        if (LanguageSettingsState.getInstance().getLanguageIndex() == 1) {
            exceptedPrompt += "Please response in Chinese.";
        }
        return exceptedPrompt;
    }

}
