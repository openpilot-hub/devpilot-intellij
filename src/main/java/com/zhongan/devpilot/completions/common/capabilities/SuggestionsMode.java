package com.zhongan.devpilot.completions.common.capabilities;

/** This is being crated according to the Capabilities from the binary */
public enum SuggestionsMode {
  INLINE {
    @Override
    public boolean isInlineEnabled() {
      return true;
    }

    @Override
    public boolean isPopupEnabled() {
      return true;
    }
  },
  AUTOCOMPLETE {
    @Override
    public boolean isInlineEnabled() {
      return false;
    }

    @Override
    public boolean isPopupEnabled() {
      return true;
    }
  },
  HYBRID {
    @Override
    public boolean isInlineEnabled() {
      return true;
    }

    @Override
    public boolean isPopupEnabled() {
      return true;
    }
  };

  public abstract boolean isInlineEnabled();

  public abstract boolean isPopupEnabled();
}
