package com.zhongan.devpilot.statusBar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;

public interface CompletionsStateNotifier {
    void stateChanged(boolean isEnabled);

    class Companion {
        private static final Topic<CompletionsStateNotifier> COMPLETIONS_STATE_CHANGED_TOPIC =
            new Topic<>("Completions State Changed Notifier", CompletionsStateNotifier.class);

        public static void publish(boolean isEnabled) {
            ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(COMPLETIONS_STATE_CHANGED_TOPIC)
                .stateChanged(isEnabled);
        }

        public static MessageBusConnection subscribe(CompletionsStateNotifier subscriber) {
            MessageBusConnection bus = ApplicationManager.getApplication().getMessageBus().connect();
            bus.subscribe(COMPLETIONS_STATE_CHANGED_TOPIC, subscriber);
            return bus;
        }
    }

}
