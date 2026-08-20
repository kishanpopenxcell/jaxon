package com.example.domain.speech

data class WakeMatch(
    val matchedPhrase: String,
    val trailingCommand: String?
)

/**
 * Matches "hey jaxon" (and common mis-transcriptions of "Jaxon") inside a list of speech
 * recognizer hypotheses, and extracts anything spoken right after the wake phrase so a
 * single utterance like "hey jaxon what time is it" can skip straight to command execution.
 */
object WakeWordDetector {

    private val WAKE_PREFIXES = listOf(
        "hey jaxon", "hey jackson", "hey jaxson", "hey jason", "hey jax",
        "hi jaxon", "hi jackson", "hi jason",
        "ok jaxon", "okay jaxon", "ok jackson", "okay jackson",
        "a jaxon", "he jaxon"
    )

    fun findWake(candidates: List<String>): WakeMatch? {
        for (raw in candidates) {
            val normalized = normalize(raw)
            for (prefix in WAKE_PREFIXES) {
                val index = normalized.indexOf(prefix)
                if (index == -1) continue
                val remainder = normalized.substring(index + prefix.length).trim()
                return WakeMatch(
                    matchedPhrase = prefix,
                    trailingCommand = remainder.ifBlank { null }
                )
            }
        }
        return null
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
