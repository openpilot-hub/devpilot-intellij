package com.zhongan.devpilot.completions.common.selections;

import com.intellij.openapi.editor.Editor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompletionObserver {
  private static final List<CompletionListener> completionListeners = new ArrayList<>();

  public static void subscribe(CompletionListener listener) {
    completionListeners.add(listener);
  }

  public static void unsubscribe(CompletionListener listener) {
    completionListeners.remove(listener);
  }

  public static void notifyListeners(Editor editor) {
    for (CompletionListener completionListener : new CopyOnWriteArrayList<>(completionListeners)) {
      completionListener.onCompletion(editor);
    }
  }
}
