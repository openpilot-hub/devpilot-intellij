package com.zhongan.devpilot.common.binary;

public interface BinaryRequest<R> {
  Class<R> response();

}
