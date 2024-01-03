package com.zhongan.devpilot.common.binary.requests.selection;

import com.google.gson.annotations.SerializedName;
import com.zhongan.devpilot.common.capabilities.RenderingMode;
import com.zhongan.devpilot.common.general.CompletionKind;
import com.zhongan.devpilot.common.general.CompletionOrigin;
import com.zhongan.devpilot.common.general.SuggestionTrigger;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class SelectionRequest {
  // the file extension: rs | js etc.
  public String language;
  // suggestion total length ('namespace'.length)
  public Integer length;
  public CompletionOrigin origin;
  // length - what's already written ('space'.length)
  @SerializedName(value = "net_length")
  public Integer netLength;
  // the percentage showed with the suggestion
  public String strength;
  // index of the selected suggestion (1)
  public Integer index;
  // text written before the suggestion start ('String name'.length)
  @SerializedName(value = "line_prefix_length")
  public Integer linePrefixLength;
  // line_prefix_length - the part of the text that's in the suggestion ('String '.length)
  @SerializedName(value = "line_net_prefix_length")
  public Integer lineNetPrefixLength;
  // text written the place at which the suggestion showed (' = "L'.length)
  @SerializedName(value = "line_suffix_length")
  public Integer lineSuffixLength;

  @SerializedName(value = "num_of_suggestions")
  public Integer suggestionsCount;

  @SerializedName(value = "num_of_vanilla_suggestions")
  public Integer vanillaSuggestionsCount;

  @SerializedName(value = "num_of_deep_local_suggestions")
  public Integer deepLocalSuggestionsCount;

  @SerializedName(value = "num_of_deep_cloud_suggestions")
  public Integer deepCloudSuggestionsCount;

  @SerializedName(value = "num_of_lsp_suggestions")
  public Integer lspSuggestionsCount;

  public List<SelectionSuggestionRequest> suggestions;

  @SerializedName(value = "completion_kind")
  public CompletionKind completionKind;

  @Nullable
  @SerializedName(value = "suggestion_trigger")
  public SuggestionTrigger suggestionTrigger;

  @Nullable
  @SerializedName(value = "snippet_context")
  public Map<String, Object> snippetContext;

  @Nullable
  @SerializedName(value = "suggestion_rendering_mode")
  public RenderingMode suggestionRenderingMode;
}
