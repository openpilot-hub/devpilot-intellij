package com.zhongan.devpilot.completions.common.selections;

import com.intellij.openapi.editor.Editor;

public interface CompletionListener {
  void onCompletion(Editor editor);
}
