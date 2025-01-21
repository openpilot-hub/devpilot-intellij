package com.zhongan.devpilot.listener;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.completions.general.DependencyContainer;
import com.zhongan.devpilot.completions.inline.CompletionPreview;
import com.zhongan.devpilot.completions.inline.DefaultCompletionAdjustment;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import com.zhongan.devpilot.enums.CompletionTypeEnum;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.zhongan.devpilot.DevPilotIcons.COMPLETION_IN_PROGRESS;
import static com.zhongan.devpilot.DevPilotIcons.SYSTEM_ICON;

public class DevPilotLineIconListener implements CaretListener {
    private static final Map<Editor, RangeHighlighter> activeHighlighters = new HashMap<>();

    private final Project project;

    public DevPilotLineIconListener(Project project) {
        this.project = project;
    }

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
        Editor editor = event.getEditor();
        if (event.getCaret() == null) {
            return;
        }

        if (editor.getCaretModel().getCaretCount() > 1) {
            return;
        }

        if (project != editor.getProject()) {
            return;
        }

        int line = event.getCaret().getLogicalPosition().line;

        // 更新光标所在行的图标
        updateGutterIcon(editor, line);
    }

    public static DevPilotGutterIconRenderer updateGutterIcon(Editor editor, int line) {
        MarkupModel markupModel = editor.getMarkupModel();

        // 清理之前的高亮
        removePreviousHighlight(editor);

        // 添加新的高亮图标
        RangeHighlighter highlighter = markupModel.addLineHighlighter(line, 0, null);
        var gutterIconRenderer = new DevPilotGutterIconRenderer(line);
        highlighter.setGutterIconRenderer(gutterIconRenderer);
        activeHighlighters.put(editor, highlighter);
        return gutterIconRenderer;
    }

    private static void removePreviousHighlight(Editor editor) {
        if (activeHighlighters.containsKey(editor)) {
            RangeHighlighter highlighter = activeHighlighters.get(editor);
            if (highlighter != null) {
                editor.getMarkupModel().removeHighlighter(highlighter);
            }
            activeHighlighters.remove(editor);
        }
    }

    public static class DevPilotGutterIconRenderer extends GutterIconRenderer {
        private final int line;

        private boolean isLoading;

        DevPilotGutterIconRenderer(int line) {
            this.line = line;
        }

        @Override
        public @NotNull Icon getIcon() {
            return isLoading ? COMPLETION_IN_PROGRESS : SYSTEM_ICON;
        }

        @Override
        public @Nullable String getTooltipText() {
            var shortcut = KeymapUtil.getShortcutText("ManualTriggerChatCompletionAction");
            return DevPilotMessageBundle.get("devpilot.chat.completion.desc") + " " + shortcut;
        }

        @Override
        public @Nullable AnAction getClickAction() {
            var self = this;
            return new AnAction("Line Action") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    var editor = e.getData(CommonDataKeys.EDITOR);
                    var project = e.getProject();

                    if (editor == null || project == null) {
                        return;
                    }

                    setLoading(true);

                    DevPilotCompletion lastShownCompletion = CompletionPreview.getCurrentCompletion(editor);
                    DependencyContainer.singletonOfInlineCompletionHandler().retrieveAndShowCompletion(
                            editor, editor.getCaretModel().getOffset(), lastShownCompletion, "",
                            new DefaultCompletionAdjustment(), CompletionTypeEnum.CHAT_COMPLETION.getType(), self
                    );
                }
            };
        }

        public boolean isLoading() {
            return isLoading;
        }

        public void setLoading(boolean loading) {
            isLoading = loading;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DevPilotGutterIconRenderer && line == ((DevPilotGutterIconRenderer) obj).line;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(line);
        }
    }
}
