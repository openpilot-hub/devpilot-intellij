package com.zhongan.devpilot.sse;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindowService;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.session.ChatSessionManager;
import com.zhongan.devpilot.session.ChatSessionManagerService;
import com.zhongan.devpilot.sse.entity.PartialMessage;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.webview.model.AgentDecisionModel;
import com.zhongan.devpilot.webview.model.MessageModel;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DeepThinkingEventProcessor {

    private static final Logger LOG = Logger.getInstance(DeepThinkingEventProcessor.class);

    public static final DeepThinkingEventProcessor INSTANCE = new DeepThinkingEventProcessor();

    private ChatSessionManager sessionManager;

    private DevPilotChatToolWindowService service;

    private void prepareProcessorContext(Project project) {
        sessionManager = project.getService(ChatSessionManagerService.class).getSessionManager();
        service = project.getService(DevPilotChatToolWindowService.class);
    }

    public void processDeepThinkingEvent(Project project, Map<String, String> eventData) {
        try {
            String tag = eventData.get("tag");
            if (StringUtils.isEmpty(tag)) {
                LOG.warn("DeepThinking event missing event tag:" + tag + ".");
                return;
            }
            prepareProcessorContext(project);

            if (StringUtils.equalsIgnoreCase(tag, "partial-steaming") || StringUtils.equalsIgnoreCase(tag, "partial-completed")) {
                processEvent(project, eventData, tag);
            } else if (StringUtils.equalsIgnoreCase(tag, "request-failed")) {
                processFailedEvent(project, eventData, tag);
            } else if (StringUtils.equalsIgnoreCase(tag, "request-cancelled")) {
                processCancelledEvent(project, eventData, tag);
            } else if (StringUtils.equalsIgnoreCase(tag, "mcp-decision")) {
                processMcpDecision(eventData, tag);
            } else if (StringUtils.equalsIgnoreCase(tag, "max-rounds-reached")) {
                processMaxRoundsReached(project, eventData, tag);
            } else {
                LOG.warn("Unknown DeepThinking event tag:" + tag + ".");
            }
        } catch (Exception e) {
            LOG.error("Error processing DeepThinking event", e);
            DevPilotNotification.error("处理深度思考事件时发生错误: " + e.getMessage());
        }
    }

    private void processMcpDecision(Map<String, String> eventData, String tag) {
        PartialMessage partialMessage = preparePartialMessage(eventData, tag, Boolean.TRUE);
        if (null != partialMessage) {
            MessageModel lastMessage = getLastMessage();
            boolean existAssistantFlag = isLastMessageFromAssistant(lastMessage);
            if (existAssistantFlag) {
                List<AgentDecisionModel> decisions = lastMessage.getDecisions();
                decisions.stream()
                        .filter(decision -> StringUtils.equalsIgnoreCase(decision.getResponseId(), partialMessage.getResponseId()))
                        .findFirst()
                        .ifPresent(matchedDecision -> {
                            matchedDecision.setResult(partialMessage.getResult());
                            matchedDecision.setName(partialMessage.getServerName());
                            matchedDecision.setComponentName(partialMessage.getComponentName());
                            matchedDecision.setComponentType(partialMessage.getComponentType());
                        });
            }
        }
    }

    private void processFailedEvent(Project project, Map<String, String> eventData, String tag) {
        LOG.warn("Processing failed event with eventData:[" + JsonUtils.toJson(eventData) + "].");
        PartialMessage partialMessage = preparePartialMessage(eventData, tag, Boolean.FALSE);
        if (null != partialMessage) {
            var llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);
            if (partialMessage.getStatusCode() == 401) {
                llmProvider.handleNoAuth(service);
            } else if (partialMessage.getStatusCode() == 400) {
                if (StringUtils.containsIgnoreCase(partialMessage.getThought(), "context length is too long")) {
                    llmProvider.handleContextTooLong(service);
                } else if (StringUtils.containsIgnoreCase(partialMessage.getThought(), "DevPilot version is too old, please upgrade.")
                        || StringUtils.containsIgnoreCase(partialMessage.getThought(), "plugin version is too low")) {
                    llmProvider.handlePluginVersionTooLow(service, true);
                }
            } else {
                MessageModel lastMessage = getLastMessage();
                boolean existAssistantFlag = isLastMessageFromAssistant(lastMessage);
                if (existAssistantFlag && lastMessage.getStreaming()) {
                    lastMessage.setStreaming(Boolean.FALSE);
                }
                service.callWebView(Boolean.FALSE);
            }
            project.getService(ChatSessionManagerService.class).getSessionManager().handleRequestMessageListSaved(partialMessage.getSession());
        }
    }

    private void processCancelledEvent(Project project, Map<String, String> eventData, String tag) {
        PartialMessage partialMessage = preparePartialMessage(eventData, tag, Boolean.FALSE);
        if (null != partialMessage) {
            MessageModel lastMessage = getLastMessage();
            boolean existAssistantFlag = isLastMessageFromAssistant(lastMessage);
            if (existAssistantFlag && lastMessage.getStreaming()) {
                lastMessage.setStreaming(Boolean.FALSE);
            }
            sessionManager.getCurrentSession().setAbort(Boolean.TRUE);
            service.callWebView(Boolean.FALSE);
            project.getService(ChatSessionManagerService.class).getSessionManager().handleRequestMessageListSaved(partialMessage.getSession());
        }
    }

    private PartialMessage preparePartialMessage(Map<String, String> eventData, String tag, boolean excludeAborted) {
        PartialMessage partialMessage = JsonUtils.fromJson(eventData.get("message"), PartialMessage.class);

        boolean isInValidPartialMessage = partialMessage == null || StringUtils.isEmpty(partialMessage.getSessionId());

        if (isInValidPartialMessage) {
            LOG.warn("DeepThinking partial event missing sessionId or data.---" + tag + "---");
            return null;
        }

        if (!StringUtils.equalsIgnoreCase(partialMessage.getSessionId(), sessionManager.getCurrentSession().getId())) {
            LOG.warn("Ignore ---" + partialMessage.getSessionId() + "===" + sessionManager.getCurrentSession().getId() + "---" + tag + "----");
            return null;
        }

        if (excludeAborted && sessionManager.getCurrentSession().isAbort()) {
            LOG.warn("Ignore ---canceled session" + partialMessage.getSessionId() + "===" + sessionManager.getCurrentSession().getId() + "---" + tag + "----");
            return null;
        }

        return partialMessage;
    }

    private void processEvent(Project project, Map<String, String> eventData, String tag) {
        try {

            PartialMessage partialMessage = preparePartialMessage(eventData, tag, Boolean.TRUE);
            if (null == partialMessage) {
                return;
            }

            if (StringUtils.equalsIgnoreCase(tag, "partial-completed")) {
                LOG.info("Process partial-completed event for " + partialMessage.getSessionId() + ".");
            }

            boolean steaming = isSteamingMessage(tag, partialMessage.getActionType());

            boolean isActionDetermined = partialMessage.isCompleted()
                    || (partialMessage.isThoughtCompleted() && partialMessage.getActionType() != null && partialMessage.getAction() != null);

            if (isActionDetermined) {
                handleActionTypeDetermined(project, partialMessage, steaming, tag);
            } else {
                handleThinkingInProgress(project, partialMessage, steaming);
            }

        } catch (Exception e) {
            LOG.error("Error processing DeepThinking partial event", e);
        }
    }

    private boolean isSteamingMessage(String tag, String actionType) {
        return !(isPartialCompleted(tag) && isFinalStateMessageAction(actionType));
    }

    private boolean isPartialCompleted(String tag) {
        return StringUtils.equalsIgnoreCase("partial-completed", tag);
    }

    private boolean isFinalStateMessageAction(String actionType) {
        return StringUtils.equalsIgnoreCase(actionType, "Say") || StringUtils.equalsIgnoreCase(actionType, "Ask");
    }

    private MessageModel getLastMessage() {
        var currentSession = sessionManager.getCurrentSession();
        var historyMessageList = currentSession.getHistoryMessageList();
        return historyMessageList.isEmpty() ? null : historyMessageList.get(historyMessageList.size() - 1);
    }

    private boolean isLastMessageFromAssistant(MessageModel lastMessage) {
        return lastMessage != null && "assistant".equals(lastMessage.getRole());
    }

    private AgentDecisionModel getLastPartialDecision(MessageModel assistantMessage) {
        AgentDecisionModel lastDecision = null;

        var decisions = assistantMessage.getDecisions();
        if (decisions.isEmpty()) {
            return lastDecision;
        }
        AgentDecisionModel decision = decisions.get(decisions.size() - 1);
        if (!decision.isCompleted()) {
            lastDecision = decision;
        }
        return lastDecision;
    }

    private void addThinkingDecision(MessageModel assistantMessage, String thought, String responseId) {
        AgentDecisionModel decision = new AgentDecisionModel();
        decision.setCompleted(false);
        decision.setThought(thought);
        decision.setResponseId(responseId);
        assistantMessage.getDecisions().add(decision);
    }

    private AgentDecisionModel addActionDecision(MessageModel assistantMessage, boolean isCompleted, String thought, String actionType, String responseId) {
        AgentDecisionModel decision = new AgentDecisionModel();
        decision.setResponseId(responseId);
        updateDecision(decision, isCompleted, thought, actionType);
        assistantMessage.getDecisions().add(decision);
        return decision;
    }

    private void updateDecision(AgentDecisionModel decision, boolean isCompleted, String thought, String actionType) {
        decision.setCompleted(isCompleted);
        decision.setThought(thought);
        decision.setActionType(actionType);
    }

    private MessageModel getOrBuildLastMessage(MessageModel lastMessage, boolean existAssistantFlag, boolean steaming) {
        MessageModel assistantMessage;
        if (existAssistantFlag) {
            assistantMessage = lastMessage;
            assistantMessage.setStreaming(steaming);
        } else {
            assistantMessage = MessageModel.buildEmptyAssistantMessage(steaming);
            assistantMessage.setChatMode(sessionManager.getCurrentSession().getChatMode());
        }
        return assistantMessage;
    }

    private void refreshUI(Project project, MessageModel assistantMessage, boolean existAssistantFlag) {
        var llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);
        llmProvider.restoreMessage(assistantMessage);

        if (existAssistantFlag) {
            service.callWebView(Boolean.FALSE);
        } else {
            service.callWebView(assistantMessage);
            service.addMessage(assistantMessage);
        }
    }

    private void handleThinkingInProgress(Project project, PartialMessage partialMessage, boolean steaming) {
        MessageModel lastMessage = getLastMessage();
        boolean existAssistantFlag = isLastMessageFromAssistant(lastMessage);

        MessageModel assistantMessage = getOrBuildLastMessage(lastMessage, existAssistantFlag, steaming);
        AgentDecisionModel lastDecision = getLastPartialDecision(assistantMessage);

        if (null == lastDecision) {
            addThinkingDecision(assistantMessage, partialMessage.getThought(), partialMessage.getResponseId());
        } else {
            lastDecision.setThought(partialMessage.getThought());
        }

        refreshUI(project, assistantMessage, existAssistantFlag);
    }

    private void handleActionTypeDetermined(Project project, PartialMessage partialMessage, boolean steaming, String tag) {
        MessageModel lastMessage = getLastMessage();
        boolean existAssistantFlag = isLastMessageFromAssistant(lastMessage);
        boolean isPartialCompleted = isPartialCompleted(tag);

        MessageModel assistantMessage = getOrBuildLastMessage(lastMessage, existAssistantFlag, steaming);
        AgentDecisionModel lastDecision = getLastPartialDecision(assistantMessage);

        if (null == lastDecision) {
            lastDecision = addActionDecision(assistantMessage, isPartialCompleted, partialMessage.getThought(), partialMessage.getActionType(), partialMessage.getResponseId());
        } else {
            updateDecision(lastDecision, isPartialCompleted, partialMessage.getThought(), partialMessage.getActionType());
        }

        if (isFinalStateMessageAction(partialMessage.getActionType())) {
            assistantMessage.setContent(getPartialMessageAction(partialMessage.getAction()));
            assistantMessage.setStreaming(!isPartialCompleted);
        } else {
            lastDecision.setActionContent(getPartialMessageAction(partialMessage.getAction()));
        }

        refreshUI(project, assistantMessage, existAssistantFlag);

        if (isPartialCompleted) {
            var currentSession = sessionManager.getCurrentSession();
            sessionManager.saveSession(currentSession);
        }
    }

    private String getPartialMessageAction(Object action) {
        String result;
        if (action instanceof String) {
            result = (String) action;
        } else {
            result = JsonUtils.toJson(action);
        }
        return result;
    }

    private void processMaxRoundsReached(Project project, Map<String, String> eventData, String tag) {
        PartialMessage partialMessage = preparePartialMessage(eventData, tag, Boolean.FALSE);
        if (null == partialMessage) {
            return;
        }
        MessageModel lastMessage = getLastMessage();
        boolean existAssistantFlag = isLastMessageFromAssistant(lastMessage);
        MessageModel assistantMessage = getOrBuildLastMessage(lastMessage, existAssistantFlag, Boolean.FALSE);
        AgentDecisionModel lastDecision = new AgentDecisionModel();
        lastDecision.setActionType(partialMessage.getActionType());
        lastDecision.setThought(partialMessage.getThought());
        lastDecision.setCompleted(Boolean.TRUE);
        lastDecision.setResponseId(partialMessage.getResponseId());
        assistantMessage.getDecisions().add(lastDecision);
        assistantMessage.setStreaming(Boolean.FALSE);
        refreshUI(project, assistantMessage, existAssistantFlag);
        project.getService(ChatSessionManagerService.class).getSessionManager().handleRequestMessageListSaved(partialMessage.getSession());
    }

}