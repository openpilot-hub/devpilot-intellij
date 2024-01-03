package com.zhongan.devpilot.common.inline

enum class CompletionOrder {
    PREVIOUS {
        override fun diff() = -1
    },
    NEXT {
        override fun diff() = 1
    };

    abstract fun diff(): Int
}
