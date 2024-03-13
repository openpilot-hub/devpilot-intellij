//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
    public final String oldPrefix;
    public final String newPrefix;
    public final String oldSuffix;
    public final String newSuffix;
    public final int index;
    public String cursorPrefix;
    public String cursorSuffix;
    public SuggestionTrigger suggestionTrigger;
    public @Nullable CompletionMetadata completionMetadata;
    private String fullSuffix = null;

    public DevPilotCompletion(String oldPrefix, String newPrefix, String oldSuffix, String newSuffix, int index, String cursorPrefix, String cursorSuffix, @Nullable CompletionMetadata completionMetadata, SuggestionTrigger suggestionTrigger) {
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
        return new DevPilotCompletion(oldPrefix, this.newPrefix, this.oldSuffix, this.newSuffix, this.index, cursorPrefix, this.cursorSuffix, this.completionMetadata, this.suggestionTrigger);
    }

    public String getSuffix() {
        if (this.fullSuffix != null) {
            return this.fullSuffix;
        } else {
            String itemText = this.newPrefix + this.newSuffix;
            String prefix = this.oldPrefix;
            if (prefix.isEmpty()) {
                return this.fullSuffix = itemText;
            } else {
                FList<TextRange> fragments = LookupCellRenderer.getMatchingFragments(prefix, itemText);
                if (fragments != null && !fragments.isEmpty()) {
                    List<TextRange> list = new ArrayList(fragments);
                    return this.fullSuffix = itemText.substring(((TextRange)list.get(list.size() - 1)).getEndOffset());
                } else {
                    return this.fullSuffix = "";
                }
            }
        }
    }

    public int getNetLength() {
        return this.getSuffix().length();
    }

    public boolean isSnippet() {
        if (this.completionMetadata != null && this.completionMetadata.getCompletionKind() != null) {
            return this.completionMetadata.getCompletionKind() == CompletionKind.Snippet;
        } else {
            return false;
        }
    }

    public String getOldPrefix() {
        return this.oldPrefix;
    }

    public String getNewPrefix() {
        return this.newPrefix;
    }

    public String getOldSuffix() {
        return this.oldSuffix;
    }

    public String getNewSuffix() {
        return this.newSuffix;
    }

    public int getIndex() {
        return this.index;
    }

    public String getCursorPrefix() {
        return this.cursorPrefix;
    }

    public void setCursorPrefix(String cursorPrefix) {
        this.cursorPrefix = cursorPrefix;
    }

    public String getCursorSuffix() {
        return this.cursorSuffix;
    }

    public void setCursorSuffix(String cursorSuffix) {
        this.cursorSuffix = cursorSuffix;
    }

    public SuggestionTrigger getSuggestionTrigger() {
        return this.suggestionTrigger;
    }

    public void setSuggestionTrigger(SuggestionTrigger suggestionTrigger) {
        this.suggestionTrigger = suggestionTrigger;
    }

    public CompletionMetadata getCompletionMetadata() {
        return this.completionMetadata;
    }

    public void setCompletionMetadata(CompletionMetadata completionMetadata) {
        this.completionMetadata = completionMetadata;
    }

    public String getFullSuffix() {
        return this.fullSuffix;
    }

    public void setFullSuffix(String fullSuffix) {
        this.fullSuffix = fullSuffix;
    }
}
