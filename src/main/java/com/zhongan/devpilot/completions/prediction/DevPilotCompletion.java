package com.zhongan.devpilot.completions.prediction;

import com.intellij.codeInsight.lookup.impl.LookupCellRenderer;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.FList;
import com.zhongan.devpilot.completions.Completion;
import com.zhongan.devpilot.completions.general.CompletionKind;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.requests.CompletionMetadata;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import static com.zhongan.devpilot.completions.inline.CompletionPreviewUtils.shouldRemoveSuffix;

public class DevPilotCompletion implements Completion {
    public final String id;

    public Editor editor;

    public final String oldPrefix;

    public final String newPrefix;

    public final String oldSuffix;

    public final String newSuffix;

    public final int index;

    public String cursorPrefix;

    public String cursorSuffix;

    public SuggestionTrigger suggestionTrigger;

    @Nullable // if new plugin with old binary
    public CompletionMetadata completionMetadata;

    private String fullSuffix = null;

    LineStateItems lineStateItems;

    public DevPilotCompletion(
            Editor editor,
            String id,
            String oldPrefix,
            String newPrefix,
            String oldSuffix,
            String newSuffix,
            int index,
            String cursorPrefix,
            String cursorSuffix,
            @Nullable CompletionMetadata completionMetadata,
            SuggestionTrigger suggestionTrigger) {
        this.editor = editor;
        this.id = id;
        this.oldPrefix = oldPrefix;
        this.newPrefix = newPrefix;
        this.oldSuffix = oldSuffix;
        this.newSuffix = newSuffix;
        this.index = index;
        this.cursorPrefix = cursorPrefix;
        this.cursorSuffix = cursorSuffix;
        this.completionMetadata = completionMetadata;
        this.suggestionTrigger = suggestionTrigger;
        lineStateItems = new LineStateItems();
        init();
    }

    public DevPilotCompletion createAdjustedCompletion(String oldPrefix, String cursorPrefix) {
        return new DevPilotCompletion(
                this.editor,
                this.id,
                oldPrefix,
                this.newPrefix,
                this.oldSuffix,
                this.newSuffix,
                this.index,
                cursorPrefix,
                this.cursorSuffix,
                this.completionMetadata,
                this.suggestionTrigger);
    }

    private void init() {
        splitLines(prepare(this.getSuffix()));
    }

    private String prepare(String suffix) {
        int cursorOffset = ReadAction.compute(() ->
                editor.getCaretModel().getOffset());

        if (shouldRemoveSuffix(this)) {
            WriteAction.runAndWait(() -> {
                editor.getDocument().deleteString(cursorOffset, cursorOffset + this.oldSuffix.length());
            });
        }
        return suffix;
    }

    public static class LineStateItems {

        private List<LineState> lineStates;

        private int index;

        public void clear() {
            lineStates.clear();
            index = 0;
        }

        public void acceptLine(int index, int offset) {
            if (index < 0 || index >= lineStates.size()) {
                return;
            }
            lineStates.get(index).setAccepted(true);
            lineStates.get(index).setOffset(offset);
        }

        public LineState getNextLineState() {
            return lineStates.get(index);
        }

        public String getBeforeLine() {
            return lineStates.get(index - 1).line;
        }

        public String getUnacceptedLines() {
            List<String> result = new ArrayList<>();
            for (LineState lineState : lineStates) {
                if (!lineState.isAccepted()) {
                    result.add(lineState.line);
                }
            }
            return "\n" + String.join("\n", result);  // must add "\n", otherwise block preview will occur change line issue.
        }

        public void init(List<LineState> lineStates) {
            this.lineStates = lineStates;
            this.index = 0;
        }

        public List<LineState> getLineStates() {
            return lineStates;
        }

        public void setLineStates(List<LineState> lineStates) {
            this.lineStates = lineStates;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public static class LineState {

            private boolean accepted = false;

            private String line;

            private int offset;

            public boolean isAccepted() {
                return accepted;
            }

            public void setAccepted(boolean accepted) {
                this.accepted = accepted;
            }

            public String getLine() {
                return line;
            }

            public void setLine(String line) {
                this.line = line;
            }

            public int getOffset() {
                return offset;
            }

            public void setOffset(int offset) {
                this.offset = offset;
            }
        }
    }

    private void splitLines(String suffix) {
        List<LineStateItems.LineState> res = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (c == '\n') {
                LineStateItems.LineState lineState = new LineStateItems.LineState();
                lineState.setLine(currentLine.toString());
                lineState.setAccepted(false);
                res.add(lineState);
                currentLine = new StringBuilder();
            } else {
                currentLine.append(c);
            }
        }
        if (currentLine.length() > 0) {
            LineStateItems.LineState lineState = new LineStateItems.LineState();
            lineState.setLine(currentLine.toString());
            lineState.setAccepted(false);
            res.add(lineState);
        }
        lineStateItems.init(res);
    }

    public LineStateItems.LineState getNextUnacceptLineState() {
        return lineStateItems.getLineStates().get(lineStateItems.index);
    }

    public String getUnacceptedLines() {
        return this.lineStateItems.getUnacceptedLines().substring(1);   // skip first \n to avoid extra lines count in accept telemetry
    }

    public void acceptLine(int offset) {
        lineStateItems.acceptLine(this.lineStateItems.index++, offset);
    }

    public int getCurrentCompletionPosition() {
        int size = lineStateItems.getLineStates().size();
        if (lineStateItems.getIndex() >= size || lineStateItems.getIndex() <= 0) {
            return 0;
        }

        return lineStateItems.getLineStates().get(lineStateItems.getIndex() - 1).getOffset();
    }

    public String getCurrentCompletionCode() {
        int size = lineStateItems.getLineStates().size();
        if (lineStateItems.getIndex() >= size || lineStateItems.getIndex() <= 0) {
            return "";
        }

        return lineStateItems.getLineStates().get(lineStateItems.getIndex() - 1).getLine() + "\n";
    }

    public void clear() {
        lineStateItems.clear();
    }

    public String getSuffix() {
        if (fullSuffix != null) {
            return fullSuffix;
        }

        String itemText = this.newPrefix + this.newSuffix;
        String prefix = this.oldPrefix;
        if (prefix.isEmpty()) {
            return fullSuffix = itemText;
        }

        FList<TextRange> fragments = LookupCellRenderer.getMatchingFragments(prefix, itemText);
        if (fragments != null && !fragments.isEmpty()) {
            List<TextRange> list = new ArrayList<>(fragments);
            return fullSuffix = itemText.substring(list.get(list.size() - 1).getEndOffset());
        }

        return fullSuffix = "";
    }

    public int getNetLength() {
        return getSuffix().length();
    }

    @Override
    public boolean isSnippet() {
        if (this.completionMetadata == null || this.completionMetadata.getCompletionKind() == null) {
            return false;
        }

        return this.completionMetadata.getCompletionKind() == CompletionKind.Snippet;
    }

    public String getOldPrefix() {
        return oldPrefix;
    }

    public String getNewPrefix() {
        return newPrefix;
    }

    public String getOldSuffix() {
        return oldSuffix;
    }

    public String getNewSuffix() {
        return newSuffix;
    }

    public int getIndex() {
        return index;
    }

    public String getCursorPrefix() {
        return cursorPrefix;
    }

    public void setCursorPrefix(String cursorPrefix) {
        this.cursorPrefix = cursorPrefix;
    }

    public String getCursorSuffix() {
        return cursorSuffix;
    }

    public void setCursorSuffix(String cursorSuffix) {
        this.cursorSuffix = cursorSuffix;
    }

    public SuggestionTrigger getSuggestionTrigger() {
        return suggestionTrigger;
    }

    public void setSuggestionTrigger(SuggestionTrigger suggestionTrigger) {
        this.suggestionTrigger = suggestionTrigger;
    }

    public CompletionMetadata getCompletionMetadata() {
        return completionMetadata;
    }

    public void setCompletionMetadata(CompletionMetadata completionMetadata) {
        this.completionMetadata = completionMetadata;
    }

    public String getFullSuffix() {
        return fullSuffix;
    }

    public void setFullSuffix(String fullSuffix) {
        this.fullSuffix = fullSuffix;
    }

    public LineStateItems getLineStateItems() {
        return lineStateItems;
    }

    public void setLineStateItems(LineStateItems lineStateItems) {
        this.lineStateItems = lineStateItems;
    }
}
