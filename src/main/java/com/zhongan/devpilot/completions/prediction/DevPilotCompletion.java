package com.zhongan.devpilot.completions.prediction;

import com.intellij.codeInsight.lookup.impl.LookupCellRenderer;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.FList;
import com.zhongan.devpilot.completions.Completion;
import com.zhongan.devpilot.completions.general.CompletionKind;
import com.zhongan.devpilot.completions.general.SuggestionTrigger;
import com.zhongan.devpilot.completions.requests.CompletionMetadata;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class DevPilotCompletion implements Completion {
    public final String id;

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

    public DevPilotCompletion(
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
    }

    public DevPilotCompletion createAdjustedCompletion(String oldPrefix, String cursorPrefix) {
        return new DevPilotCompletion(
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
}
