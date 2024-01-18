package com.zhongan.devpilot.listener;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.zhongan.devpilot.util.ConfigChangeUtils;

import org.jetbrains.annotations.NotNull;

public class ThemeChangeListener implements LafManagerListener {

    @Override
    public void lookAndFeelChanged(@NotNull LafManager source) {
        ConfigChangeUtils.themeChanged();
    }
}