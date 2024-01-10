package com.zhongan.devpilot.completions.common.capabilities;

public enum SuggestionsMode {
    INLINE {
        @Override
        public boolean isInlineEnabled() {
            return true;
        }

        @Override
        public boolean isPopupEnabled() {
            return false;
        }
    };

    public abstract boolean isInlineEnabled();

    public abstract boolean isPopupEnabled();
}
