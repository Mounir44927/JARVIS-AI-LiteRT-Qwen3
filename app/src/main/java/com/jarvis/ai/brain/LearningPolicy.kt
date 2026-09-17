package com.jarvis.ai.brain

import com.jarvis.ai.data.db.Fact
import com.jarvis.ai.core.ArabicTextNormalizer

class LearningPolicy {
    data class Decision(val fact: Fact?, val requiresApproval: Boolean)

    fun explicitStore(command: String): Decision? {
        val normalized = ArabicTextNormalizer.normalize(command)
        val prefix = "تذكر ان"
        if (!normalized.startsWith(prefix)) return null
        val content = command.replaceFirst(Regex("^تذكر\\s+أن(?:ني)?\\s*"), "").replaceFirst(Regex("^تذكر\\s+ان\\s*"), "").trim(' ', '،', ':')
        if (content.isBlank()) return null
        val now = System.currentTimeMillis()
        return Decision(
            fact = Fact(category = "user_memory", content = content, source = "user", createdAt = now, updatedAt = now),
            requiresApproval = false
        )
    }

    fun suggestFromNaturalText(text: String): Decision? {
        val normalized = ArabicTextNormalizer.normalize(text)
        val markers = listOf("احب ", "افضل ", "لا احب ")
        if (markers.none { normalized.contains(it) }) return null
        val now = System.currentTimeMillis()
        return Decision(
            fact = Fact(category = "preference", content = text.trim(), source = "learned", confidence = 0.65f, createdAt = now, updatedAt = now),
            requiresApproval = true
        )
    }
}
