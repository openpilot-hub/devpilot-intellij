package com.zhongan.devpilot.agents.model;

public class AgentInfoResp {

    private String url;

    private String version;

    private String md5;

    private boolean autoUpgradeEnabled = true;

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

}
