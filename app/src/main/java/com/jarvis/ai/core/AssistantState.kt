package com.jarvis.ai.core

sealed interface AssistantState {
    data object Idle : AssistantState
    data object ListeningForWakeWord : AssistantState
    data object ListeningForCommand : AssistantState
    data object Thinking : AssistantState
    data object Speaking : AssistantState
    data class Error(val message: String) : AssistantState
}
