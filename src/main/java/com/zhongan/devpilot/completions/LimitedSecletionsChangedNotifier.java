package com.zhongan.devpilot.completions;

import com.intellij.util.messages.Topic;

public interface LimitedSecletionsChangedNotifier {
  Topic<LimitedSecletionsChangedNotifier> LIMITED_SELECTIONS_CHANGED_TOPIC =
      Topic.create("Limited Selections Changed Notifier", LimitedSecletionsChangedNotifier.class);

  void limitedChanged(boolean limited);
}
