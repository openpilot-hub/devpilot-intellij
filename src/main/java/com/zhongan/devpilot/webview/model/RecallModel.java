package com.zhongan.devpilot.webview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecallModel {
    private List<Step> steps;

    private List<CodeReferenceModel> remoteRefs;

    private List<CodeReferenceModel> localRefs;

    public RecallModel(List<Step> steps, List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs) {
        this.steps = steps;
        this.remoteRefs = remoteRefs;
        this.localRefs = localRefs;
    }

    public static RecallModel create(int step) {
        return create(step, null, null);
    }

    // 1 - step1 doing; 2 - step2 doing; 3 - step3 doing; 4 - step3 done
    public static RecallModel create(int step, List<CodeReferenceModel> remoteRefs, List<CodeReferenceModel> localRefs) {
        var steps = new ArrayList<Step>();

        for (int i = 1; i <= step - 1; i++) {
            steps.add(new Step("done"));
        }

        if (step < 4) {
            steps.add(new Step("loading"));
        }

        if (remoteRefs == null) {
            remoteRefs = new ArrayList<>();
        }

        if (localRefs == null) {
            localRefs = new ArrayList<>();
        }

        return new RecallModel(steps, remoteRefs, localRefs);
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public List<CodeReferenceModel> getRemoteRefs() {
        return remoteRefs;
    }

    public void setRemoteRefs(List<CodeReferenceModel> remoteRefs) {
        this.remoteRefs = remoteRefs;
    }

    public List<CodeReferenceModel> getLocalRefs() {
        return localRefs;
    }

    public void setLocalRefs(List<CodeReferenceModel> localRefs) {
        this.localRefs = localRefs;
    }

    static class Step {
        private String status;

        Step(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
