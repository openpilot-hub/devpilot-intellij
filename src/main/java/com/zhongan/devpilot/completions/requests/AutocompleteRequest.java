//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.requests;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class AutocompleteRequest {
    public String before;
    public String after;
    public String filename;
    @SerializedName("region_includes_beginning")
    public boolean regionIncludesBeginning;
    @SerializedName("region_includes_end")
    public boolean regionIncludesEnd;
    @SerializedName("max_num_results")
    public int maxResults;
    public int offset;
    public int line;
    public int character;
    public @Nullable Integer indentationSize;
    public @Nullable Boolean cachedOnly;
    @SerializedName("sdk_path")
    public String sdkPath;

    public AutocompleteRequest() {
    }

    public String getBefore() {
        return this.before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return this.after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isRegionIncludesBeginning() {
        return this.regionIncludesBeginning;
    }

    public void setRegionIncludesBeginning(boolean regionIncludesBeginning) {
        this.regionIncludesBeginning = regionIncludesBeginning;
    }

    public boolean isRegionIncludesEnd() {
        return this.regionIncludesEnd;
    }

    public void setRegionIncludesEnd(boolean regionIncludesEnd) {
        this.regionIncludesEnd = regionIncludesEnd;
    }

    public int getMaxResults() {
        return this.maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLine() {
        return this.line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCharacter() {
        return this.character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    public Integer getIndentationSize() {
        return this.indentationSize;
    }

    public void setIndentationSize(Integer indentationSize) {
        this.indentationSize = indentationSize;
    }

    public Boolean getCachedOnly() {
        return this.cachedOnly;
    }

    public void setCachedOnly(Boolean cachedOnly) {
        this.cachedOnly = cachedOnly;
    }

    public String getSdkPath() {
        return this.sdkPath;
    }

    public void setSdkPath(String sdkPath) {
        this.sdkPath = sdkPath;
    }
}
