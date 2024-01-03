package com.zhongan.devpilot.common.selections;

import static com.zhongan.devpilot.common.general.Utils.toInt;
import static java.util.stream.Collectors.*;

import com.zhongan.devpilot.common.binary.requests.selection.SelectionRequest;
import com.zhongan.devpilot.common.binary.requests.selection.SelectionSuggestionRequest;
import com.zhongan.devpilot.common.general.CompletionOrigin;
import com.zhongan.devpilot.common.prediction.DevPilotCompletion;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class SelectionUtil {

  public static void addSuggestionsCount(
      SelectionRequest selection, List<DevPilotCompletion> suggestions) {
    Map<CompletionOrigin, Long> originCount =
        suggestions.stream()
            .filter((suggestion) -> suggestion.getOrigin() != null)
            .collect(groupingBy(DevPilotCompletion::getOrigin, counting()));

    selection.suggestionsCount = suggestions.size();
    selection.deepCloudSuggestionsCount =
        toInt(originCount.get(CompletionOrigin.CLOUD))
            + toInt(originCount.get(CompletionOrigin.CLOUD2))
            + toInt(originCount.get(CompletionOrigin.ANBU));
    selection.deepLocalSuggestionsCount = toInt(originCount.get(CompletionOrigin.LOCAL));
    selection.lspSuggestionsCount = toInt(originCount.get(CompletionOrigin.LSP));
    selection.vanillaSuggestionsCount = toInt(originCount.get(CompletionOrigin.VANILLA));

    selection.suggestions =
        suggestions.stream()
            .map(
                suggestion -> {
                  CompletionOrigin origin =
                      suggestion.completionMetadata != null
                          ? suggestion.completionMetadata.getOrigin()
                          : null;
                  String name = origin != null ? origin.name() : null;
                  return new SelectionSuggestionRequest(
                      suggestion.newPrefix.length(), getStrength(suggestion), name);
                })
            .collect(toList());
  }

  public static String getStrength(DevPilotCompletion item) {
    if (item.completionMetadata == null) {
      return null;
    }

    if (item.completionMetadata.getOrigin() == CompletionOrigin.LSP) {
      return null;
    }

    return item.completionMetadata.getDetail();
  }

  @NotNull
  public static String asLanguage(String name) {
    String[] split = name.split("\\.");

    return Arrays.stream(split).skip(Math.max(1, split.length - 1)).findAny().orElse("undefined");
  }
}
