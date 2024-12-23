package com.zhongan.devpilot.embedding.entity;

public class FunctionPartBlockMeta {

    private int blockStartOffset;

    private int blockEndOffset;

    private String codeBody;

    public int getBlockStartOffset() {
        return blockStartOffset;
    }

    public void setBlockStartOffset(int blockStartOffset) {
        this.blockStartOffset = blockStartOffset;
    }

    public int getBlockEndOffset() {
        return blockEndOffset;
    }

    public void setBlockEndOffset(int blockEndOffset) {
        this.blockEndOffset = blockEndOffset;
    }

    public String getCodeBody() {
        return codeBody;
    }

    public void setCodeBody(String codeBody) {
        this.codeBody = codeBody;
    }

}
