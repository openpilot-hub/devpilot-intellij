package com.zhongan.devpilot.completions.common.binary.requests.autocomplete;

import com.zhongan.devpilot.completions.common.general.CompletionKind;
import com.zhongan.devpilot.completions.common.completions.Completion;
import com.zhongan.devpilot.completions.common.completions.Completion;

public class ResultEntry implements Completion {
  public String new_prefix;
  public String old_suffix;
  public String new_suffix;
  public CompletionMetadata completion_metadata;

  @Override
  public boolean isSnippet() {
    if (this.completion_metadata == null) {
      return false;
    }

    return this.completion_metadata.getCompletion_kind() == CompletionKind.Snippet;
  }
}
