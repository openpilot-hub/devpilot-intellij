package com.zhongan.devpilot.completions.common.binary;

public interface BinaryRequest<R> {
  Class<R> response();

}
