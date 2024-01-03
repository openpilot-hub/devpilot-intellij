package com.zhongan.devpilot.common.binary.requests.autocomplete

/**
 * The intent which triggered a snippet completion request in the binary.
 */
enum class UserIntent {
    Comment,
    Block,
    FunctionDeclaration,
    NoScope,
    NewLine,
    CustomTriggerPoints
}
