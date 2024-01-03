package com.zhongan.devpilot.completions.prediction;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.zhongan.devpilot.common.prediction.DevPilotCompletion;

public class DevPilotWeigher extends LookupElementWeigher {
  public DevPilotWeigher() {
    super("DevPilotLookupElementWeigher", false, true);
  }

  @Override
  public Integer weigh(LookupElement element) {
    if (element.getObject() instanceof DevPilotCompletion) {
      return ((DevPilotCompletion) element.getObject()).index;
    }

    return Integer.MAX_VALUE;
  }
}
