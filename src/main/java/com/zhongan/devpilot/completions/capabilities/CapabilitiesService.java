package com.zhongan.devpilot.completions.capabilities;

import com.intellij.openapi.components.ServiceManager;

public class CapabilitiesService {
    public static CapabilitiesService getInstance() {
        return ServiceManager.getService(CapabilitiesService.class);
    }


    public boolean isCapabilityEnabled(Capability capability) {
        //TODO 从配置中读取
        return true;
    }

}
