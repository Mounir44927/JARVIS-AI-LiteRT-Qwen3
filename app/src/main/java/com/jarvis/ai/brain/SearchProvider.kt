package com.jarvis.ai.brain

interface SearchProvider {
    fun isConfigured(): Boolean = false
}

class GeminiGroundedSearchProvider : SearchProvider
