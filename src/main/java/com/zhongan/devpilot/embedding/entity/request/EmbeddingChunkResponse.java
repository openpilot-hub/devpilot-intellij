package com.zhongan.devpilot.embedding.entity.request;

public class EmbeddingChunkResponse {
    private String result;

    private boolean needAbortSubmit = false;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isNeedAbortSubmit() {
        return needAbortSubmit;
    }

    public void setNeedAbortSubmit(boolean needAbortSubmit) {
        this.needAbortSubmit = needAbortSubmit;
    }

}
