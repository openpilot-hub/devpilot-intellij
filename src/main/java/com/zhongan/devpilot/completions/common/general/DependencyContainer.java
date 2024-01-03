package com.zhongan.devpilot.completions.common.general;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
//import com.zhongan.devpilot.common.binary.*;
//import com.zhongan.devpilot.common.binary.fetch.*;
import com.zhongan.devpilot.completions.common.capabilities.SuggestionsModeService;
import com.zhongan.devpilot.completions.common.inline.InlineCompletionHandler;
import com.zhongan.devpilot.completions.common.inline.DevPolitInlineLookupListener;
import com.zhongan.devpilot.completions.common.prediction.CompletionFacade;
import com.zhongan.devpilot.completions.common.selections.CompletionPreviewListener;
import com.zhongan.devpilot.completions.common.inline.DevPolitInlineLookupListener;
import org.jetbrains.annotations.NotNull;

public class DependencyContainer {
  public static int binaryRequestsTimeoutsThresholdMillis =
      StaticConfig.BINARY_TIMEOUTS_THRESHOLD_MILLIS;
//  private static BinaryProcessRequesterProvider BINARY_PROCESS_REQUESTER_PROVIDER_INSTANCE = null;
  private static InlineCompletionHandler INLINE_COMPLETION_HANDLER_INSTANCE = null;

  // For Integration Tests
//  private static BinaryRun binaryRunMock = null;
//  private static BinaryProcessGatewayProvider binaryProcessGatewayProviderMock = null;
  private static SuggestionsModeService suggestionsModeServiceMock = null;
  private static CompletionsEventSender completionsEventSender = null;
  private static Gson gson = instanceOfGson();

  public static synchronized Gson instanceOfGson() {
    if (gson == null) {
      gson = new GsonBuilder().registerTypeAdapter(Double.class, doubleOrIntSerializer()).create();
    }

    return gson;
  }

  public static synchronized DevPolitInlineLookupListener instanceOfDevPolitInlineLookupListener() {
    return new DevPolitInlineLookupListener();
  }

  public static CompletionPreviewListener instanceOfCompletionPreviewListener() {
//    final BinaryRequestFacade binaryRequestFacade = instanceOfBinaryRequestFacade();
//    return new CompletionPreviewListener(binaryRequestFacade);
    //TODO 调用openai
    return new CompletionPreviewListener();
  }

//  public static BinaryRequestFacade instanceOfBinaryRequestFacade() {
//    return new BinaryRequestFacade(singletonOfBinaryProcessRequesterProvider());
//  }

  public static InlineCompletionHandler singletonOfInlineCompletionHandler() {
    if (INLINE_COMPLETION_HANDLER_INSTANCE == null) {
      INLINE_COMPLETION_HANDLER_INSTANCE =
          new InlineCompletionHandler(
              instanceOfCompletionFacade(),
//              instanceOfBinaryRequestFacade(),
              instanceOfSuggestionsModeService());
    }

    return INLINE_COMPLETION_HANDLER_INSTANCE;
  }

  @NotNull
  public static CompletionFacade instanceOfCompletionFacade() {
    return new CompletionFacade(
//        instanceOfBinaryRequestFacade(),
            instanceOfSuggestionsModeService());
  }

/*  public static void setTesting(
      BinaryRun binaryRunMock,
      BinaryProcessGatewayProvider binaryProcessGatewayProviderMock,
      SuggestionsModeService suggestionsModeServiceMock,
      CompletionsEventSender completionsEventSenderMock,
      int binaryRequestsTimeoutsThreshold) {
    DependencyContainer.binaryRunMock = binaryRunMock;
    DependencyContainer.binaryProcessGatewayProviderMock = binaryProcessGatewayProviderMock;
    DependencyContainer.suggestionsModeServiceMock = suggestionsModeServiceMock;
    DependencyContainer.completionsEventSender = completionsEventSenderMock;
    DependencyContainer.binaryRequestsTimeoutsThresholdMillis = binaryRequestsTimeoutsThreshold;
  }*/

  public static SuggestionsModeService instanceOfSuggestionsModeService() {
    if (suggestionsModeServiceMock != null) {
      return suggestionsModeServiceMock;
    }

    return new SuggestionsModeService();
  }

/*  private static BinaryProcessRequesterProvider singletonOfBinaryProcessRequesterProvider() {
    if (BINARY_PROCESS_REQUESTER_PROVIDER_INSTANCE == null) {
      BINARY_PROCESS_REQUESTER_PROVIDER_INSTANCE =
          BinaryProcessRequesterProvider.create(
              instanceOfBinaryRun(),
              instanceOfBinaryProcessGatewayProvider(),
              binaryRequestsTimeoutsThresholdMillis);
    }

    return BINARY_PROCESS_REQUESTER_PROVIDER_INSTANCE;
  }*/

/*  private static BinaryProcessGatewayProvider instanceOfBinaryProcessGatewayProvider() {
    if (binaryProcessGatewayProviderMock != null) {
      return binaryProcessGatewayProviderMock;
    }

    return new BinaryProcessGatewayProvider();
  }*/

  public static CompletionsEventSender instanceOfCompletionsEventSender() {
    if (completionsEventSender != null) {
      return completionsEventSender;
    }

//    return new CompletionsEventSender(instanceOfBinaryRequestFacade());
    return new CompletionsEventSender();
  }

//  @NotNull
//  public static BinaryRun instanceOfBinaryRun() {
//    if (binaryRunMock != null) {
//      return binaryRunMock;
//    }
//
//    return new BinaryRun(instanceOfBinaryFetcher());
//  }

//  @NotNull
//  private static BinaryVersionFetcher instanceOfBinaryFetcher() {
//    return new BinaryVersionFetcher(
//        instanceOfLocalBinaryVersions(),
//        instanceOfBinaryRemoteSource(),
//        instanceOfBinaryDownloader(),
//        instanceOfBundleDownloader());
//  }

//  @NotNull
//  private static BinaryDownloader instanceOfBinaryDownloader() {
//    return new BinaryDownloader(instanceOfBinaryPropositionValidator(), instanceOfDownloader());
//  }

//  @NotNull
//  private static BundleDownloader instanceOfBundleDownloader() {
//    return new BundleDownloader(instanceOfBundlePropositionValidator(), instanceOfDownloader());
//  }

//  @NotNull
//  private static GeneralDownloader instanceOfDownloader() {
//    return new GeneralDownloader();
//  }

//  @NotNull
//  private static TempBinaryValidator instanceOfBinaryPropositionValidator() {
//    return new TempBinaryValidator(instanceOfBinaryValidator());
//  }

 /* @NotNull
  private static TempBundleValidator instanceOfBundlePropositionValidator() {
    return new TempBundleValidator();
  }

  @NotNull
  private static BinaryRemoteSource instanceOfBinaryRemoteSource() {
    return new BinaryRemoteSource();
  }

  @NotNull
  private static LocalBinaryVersions instanceOfLocalBinaryVersions() {
    return new LocalBinaryVersions(instanceOfBinaryValidator());
  }
*/
//  @NotNull
//  private static BinaryValidator instanceOfBinaryValidator() {
//    return new BinaryValidator();
//  }

  @NotNull
  private static JsonSerializer<Double> doubleOrIntSerializer() {
    return (src, type, jsonSerializationContext) -> {
      if (src == src.longValue()) {
        return new JsonPrimitive(src.longValue());
      }
      return new JsonPrimitive(src);
    };
  }
}
