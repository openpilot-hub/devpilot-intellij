package com.zhongan.devpilot.completions.requests;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.Nullable;

public class AutocompleteRequest {
    public String before;

    public String after;

    public String filename;

    @SerializedName(value = "region_includes_beginning")
    public boolean regionIncludesBeginning;

    @SerializedName(value = "region_includes_end")
    public boolean regionIncludesEnd;

    @SerializedName(value = "max_num_results")
    public int maxResults;

    public int offset;

    public int line;

    public int character;

    @Nullable
    public Integer indentationSize;

    @Nullable
    public Boolean cachedOnly;

    @SerializedName(value = "sdk_path")
    public String sdkPath;

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isRegionIncludesBeginning() {
        return regionIncludesBeginning;
    }

    public void setRegionIncludesBeginning(boolean regionIncludesBeginning) {
        this.regionIncludesBeginning = regionIncludesBeginning;
    }

    public boolean isRegionIncludesEnd() {
        return regionIncludesEnd;
    }

    public void setRegionIncludesEnd(boolean regionIncludesEnd) {
        this.regionIncludesEnd = regionIncludesEnd;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCharacter() {
        return character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    public Integer getIndentationSize() {
        return indentationSize;
    }

    public void setIndentationSize(Integer indentationSize) {
        this.indentationSize = indentationSize;
    }

    public Boolean getCachedOnly() {
        return cachedOnly;
    }

    public void setCachedOnly(Boolean cachedOnly) {
        this.cachedOnly = cachedOnly;
    }

    public String getSdkPath() {
        return sdkPath;
    }

    public void setSdkPath(String sdkPath) {
        this.sdkPath = sdkPath;
    }
}
