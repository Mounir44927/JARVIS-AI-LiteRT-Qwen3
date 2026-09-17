package com.jarvis.ai.brain

import java.net.URI

object BackendEndpointValidator {
    fun isValidBaseUrl(value: String): Boolean {
        val raw = value.trim()
        if (raw.isBlank()) return false
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        return (scheme == "https" || scheme == "http") &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo.isNullOrBlank() &&
            uri.query.isNullOrBlank() &&
            uri.fragment.isNullOrBlank()
    }
}
