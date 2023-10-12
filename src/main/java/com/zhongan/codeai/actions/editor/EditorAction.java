package com.zhongan.codeai.actions.editor;

public enum EditorAction {
  PERFORMANCE_CHECK("Performance Check",
          "Performance check in the following code",
          "{{selectedCode}}\nGiving the code above, please fix any performance issues.\nRemember you are very familiar with performance optimization.\n"),
  GENERATE_COMMENTS("Generate Comments",
          "Generate comments in the following code",
          "{{selectedCode}}\nGiving the code above, please generate comments for it.");

  private final String label;
  private final String userMessage;
  private final String prompt;

  EditorAction(String label, String userMessage, String prompt) {
    this.label = label;
    this.userMessage = userMessage;
    this.prompt = prompt;
  }

  public String getLabel() {
    return label;
  }

  public String getPrompt() {
    return prompt;
  }

  public String getUserMessage() {
    return userMessage;
  }
}
