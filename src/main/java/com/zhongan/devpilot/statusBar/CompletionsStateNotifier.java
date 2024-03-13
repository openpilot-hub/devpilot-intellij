//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.statusBar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;

public interface CompletionsStateNotifier {
    void stateChanged(boolean var1);

    public static class Companion {
        private static final Topic<CompletionsStateNotifier> COMPLETIONS_STATE_CHANGED_TOPIC = new Topic("Completions State Changed Notifier", CompletionsStateNotifier.class);

        public Companion() {
        }

        public static void publish(boolean isEnabled) {
            ((CompletionsStateNotifier)ApplicationManager.getApplication().getMessageBus().syncPublisher(COMPLETIONS_STATE_CHANGED_TOPIC)).stateChanged(isEnabled);
        }

        public static MessageBusConnection subscribe(CompletionsStateNotifier subscriber) {
            MessageBusConnection bus = ApplicationManager.getApplication().getMessageBus().connect();
            bus.subscribe(COMPLETIONS_STATE_CHANGED_TOPIC, subscriber);
            return bus;
        }
    }

}
