package com.zhongan.devpilot.actions.editor.inlay;

import com.intellij.codeInsight.hints.ImmediateConfigurable;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

import java.util.List;

import javax.swing.JPanel;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChatShortcutHintBaseProvider implements InlayHintsProvider<NoSettings> {

    private List<String> supportedElementTypes;

    public ChatShortcutHintBaseProvider(List<String> supportedElementTypes) {
        this.supportedElementTypes = supportedElementTypes;
    }

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return new SettingsKey<>("DevPilot.chat.shortcut.provider");
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return "DevPilot.chat.shortcut";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings noSettings) {
        return changeListener -> new JPanel();
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile, @NotNull Editor editor,
                                               @NotNull NoSettings noSettings, @NotNull InlayHintsSink inlayHintsSink) {
        return new ChatShortcutHintCollector(editor, supportedElementTypes);
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }
}
