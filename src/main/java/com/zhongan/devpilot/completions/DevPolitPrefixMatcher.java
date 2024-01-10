package com.zhongan.devpilot.completions;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.zhongan.devpilot.completions.common.prediction.DevPilotCompletion;
import org.jetbrains.annotations.NotNull;

public class DevPolitPrefixMatcher extends PrefixMatcher {
    final PrefixMatcher inner;

    public DevPolitPrefixMatcher(PrefixMatcher inner) {
        super(inner.getPrefix());
        this.inner = inner;
    }

    @Override
    public boolean prefixMatches(@NotNull LookupElement element) {
        if (element.getObject() instanceof DevPilotCompletion) {
            return true;
        } else if (element instanceof LookupElementDecorator) {
            return prefixMatches(((LookupElementDecorator) element).getDelegate());
        }

        return super.prefixMatches(element);
    }

    @Override
    public boolean isStartMatch(LookupElement element) {
        if (element.getObject() instanceof DevPilotCompletion) {
            return true;
        }

        return super.isStartMatch(element);
    }

    @Override
    public boolean prefixMatches(@NotNull String name) {
        return this.inner.prefixMatches(name);
    }

    @NotNull
    @Override
    public PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
        return new DevPolitPrefixMatcher(this.inner.cloneWithPrefix(prefix));
    }
}
