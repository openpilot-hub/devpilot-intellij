package com.zhongan.devpilot.embedding.entity.request;

import java.util.List;

public class EmbeddingQueryResponse {
    private List<HitData> hitsData;

    public List<HitData> getHitsData() {
        return hitsData;
    }

    public void setHitsData(List<HitData> hitsData) {
        this.hitsData = hitsData;
    }

    public static class HitData {
        private String filePath;

        private String fileHash;

        private String chunkHash;

        private int startOffset;

        private int endOffset;

        private int startLine;

        private int endLine;

        private int startColumn;

        private int endColumn;

        private String score;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileHash() {
            return fileHash;
        }

        public void setFileHash(String fileHash) {
            this.fileHash = fileHash;
        }

        public String getChunkHash() {
            return chunkHash;
        }

        public void setChunkHash(String chunkHash) {
            this.chunkHash = chunkHash;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(int startOffset) {
            this.startOffset = startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public void setEndOffset(int endOffset) {
            this.endOffset = endOffset;
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public int getStartColumn() {
            return startColumn;
        }

        public void setStartColumn(int startColumn) {
            this.startColumn = startColumn;
        }

        public int getEndColumn() {
            return endColumn;
        }

        public void setEndColumn(int endColumn) {
            this.endColumn = endColumn;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }
    }
}
