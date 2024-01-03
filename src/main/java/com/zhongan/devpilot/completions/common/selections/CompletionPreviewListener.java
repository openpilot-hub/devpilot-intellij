package com.zhongan.devpilot.completions.common.selections;

import com.intellij.openapi.editor.Editor;
//import com.zhongan.devpilot.common.binary.BinaryRequestFacade;
import com.zhongan.devpilot.completions.common.binary.requests.selection.SelectionRequest;
import com.zhongan.devpilot.completions.common.capabilities.RenderingMode;
import com.zhongan.devpilot.completions.common.prediction.DevPilotCompletion;
import com.zhongan.devpilot.completions.common.capabilities.RenderingMode;

import java.util.function.Consumer;

public class CompletionPreviewListener {
//  private final BinaryRequestFacade binaryRequestFacade;

//  public CompletionPreviewListener(BinaryRequestFacade binaryRequestFacade) {
//    this.binaryRequestFacade = binaryRequestFacade;
//  }
  public CompletionPreviewListener() {
    //TODO 调用openai
  }

  public void executeSelection(
      Editor editor,
      DevPilotCompletion completion,
      String filename,
      RenderingMode renderingMode,
      Consumer<SelectionRequest> extendSelectionRequest) {
    SelectionRequest selection = new SelectionRequest();

    selection.language = SelectionUtil.asLanguage(filename);
    selection.netLength =
        completion.newPrefix.replaceFirst("^" + completion.oldPrefix, "").length();
    selection.linePrefixLength = completion.cursorPrefix.length();
    selection.lineNetPrefixLength = selection.linePrefixLength - completion.oldPrefix.length();
    selection.lineSuffixLength = completion.cursorSuffix.length();
    selection.origin =
        completion.completionMetadata != null ? completion.completionMetadata.getOrigin() : null;
    selection.length = completion.newPrefix.length();
    selection.strength = SelectionUtil.getStrength(completion);
    selection.completionKind =
        completion.completionMetadata != null
            ? completion.completionMetadata.getCompletion_kind()
            : null;
    selection.snippetContext =
        completion.completionMetadata != null
            ? completion.completionMetadata.getSnippet_context()
            : null;
    selection.suggestionRenderingMode = renderingMode;
    selection.suggestionTrigger = completion.suggestionTrigger;
    extendSelectionRequest.accept(selection);

//    binaryRequestFacade.executeRequest(new SetStateBinaryRequest(selection));
    //TODO 调用openai
    CompletionObserver.notifyListeners(editor);
  }
}
