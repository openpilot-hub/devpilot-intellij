package com.zhongan.devpilot.agents.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentInfoResp {

    private String url;

    private String version;

    private String md5;

    private boolean autoUpgradeEnabled = true;

    private Map<String, String> archMd5 = new HashMap<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isAutoUpgradeEnabled() {
        return autoUpgradeEnabled;
    }

    public void setAutoUpgradeEnabled(boolean autoUpgradeEnabled) {
        this.autoUpgradeEnabled = autoUpgradeEnabled;
    }

    public Map<String, String> getArchMd5() {
        return archMd5;
    }

    public void setArchMd5(Map<String, String> archMd5) {
        this.archMd5 = archMd5;
    }
}
