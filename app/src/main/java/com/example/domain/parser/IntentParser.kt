package com.example.domain.parser

import java.util.Locale
import java.util.regex.Pattern

enum class IntentType {
    OPEN_APP,
    CALL_CONTACT,
    SHOW_TIME,
    BATTERY_INFO,
    DEVICE_STORAGE,
    OPEN_SETTINGS,
    FLASHLIGHT_TOGGLE,
    VOLUME_CONTROL,
    SET_ALARM,
    SET_TIMER,
    MAP_NAVIGATION,
    LAUNCH_SEARCH,
    MEDIA_CONTROL,
    UNKNOWN
}

data class ParsedIntent(
    val intentType: IntentType,
    val parameters: Map<String, String>,
    val originalText: String,
    val normalizedText: String,
    val confidence: Float
)

class IntentParser {

    // Synonym mapping to normalize words
    private val synonymMap = mapOf(
        "launch" to "open",
        "start" to "open",
        "run" to "open",
        "go to" to "open",
        "phone" to "call",
        "dial" to "call",
        "ring" to "call",
        "google" to "search",
        "look up" to "search",
        "find" to "search",
        "torch" to "flashlight",
        "raise" to "increase",
        "lower" to "decrease",
        "silence" to "mute",
        "directions" to "navigate",
        "get to" to "navigate"
    )

    // Modular rules containing intent rules
    private val rules = listOf(
        IntentRule(
            intentType = IntentType.OPEN_APP,
            patterns = listOf(
                Pattern.compile("\\b(?:open|launch|start|run|go to)\\s+(?<app>[a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("^(?<app>[a-zA-Z0-9\\s]+)\\s+(?:app)$", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("open", "launch", "start", "run", "app")
        ),
        IntentRule(
            intentType = IntentType.CALL_CONTACT,
            patterns = listOf(
                Pattern.compile("\\b(?:call|phone|dial|ring|make a call to)\\s+(?<contact>[a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:call)\\s+(?<number>\\d+)", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("call", "phone", "dial", "ring", "contact")
        ),
        IntentRule(
            intentType = IntentType.SHOW_TIME,
            patterns = listOf(
                Pattern.compile("\\b(?:what time is it|tell me the time|current time|show time|what is the time)\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:what is the date|what is today's date|today's date|current date|show date)\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("time", "date", "clock", "today", "hour")
        ),
        IntentRule(
            intentType = IntentType.BATTERY_INFO,
            patterns = listOf(
                Pattern.compile("\\b(?:battery level|battery percentage|check battery|how much battery)\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:battery life|battery status|power level)\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("battery", "power", "percentage", "charge")
        ),
        IntentRule(
            intentType = IntentType.DEVICE_STORAGE,
            patterns = listOf(
                Pattern.compile("\\b(?:device storage|available storage|check storage|how much space|storage space)\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:free space|disk space|memory status)\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("storage", "space", "memory", "disk", "free")
        ),
        IntentRule(
            intentType = IntentType.OPEN_SETTINGS,
            patterns = listOf(
                Pattern.compile("\\b(?:open|show|go to)\\s+(?<setting>[a-zA-Z0-9\\s]+)?\\s*settings\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:settings)\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("settings", "configuration", "control panel", "wifi", "bluetooth")
        ),
        IntentRule(
            intentType = IntentType.FLASHLIGHT_TOGGLE,
            patterns = listOf(
                Pattern.compile("\\b(?:turn|switch|toggle)\\s+(?<state>on|off)\\s+(?:flashlight|torch|light)\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:flashlight|torch)\\s+(?<state>on|off)\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("flashlight", "torch", "light", "on", "off")
        ),
        IntentRule(
            intentType = IntentType.VOLUME_CONTROL,
            patterns = listOf(
                Pattern.compile("\\b(?:increase|raise|turn up|volume up|make louder)\\s*(?:volume|audio|sound)?\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:decrease|lower|turn down|volume down|make softer)\\s*(?:volume|audio|sound)?\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?<state>mute|silence|unmute)\\s*(?:volume|audio|sound)?\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("volume", "audio", "sound", "mute", "unmute", "louder", "softer", "up", "down")
        ),
        IntentRule(
            intentType = IntentType.SET_ALARM,
            patterns = listOf(
                Pattern.compile("\\b(?:set an alarm for|alarm at|wake me up at|create alarm for)\\s+(?<time>[a-zA-Z0-9\\s:]+(?:am|pm)?|(?<hour>\\d{1,2}):(?<minute>\\d{2})\\s*(?<ampm>am|pm)?)\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("alarm", "wake", "morning", "set alarm")
        ),
        IntentRule(
            intentType = IntentType.SET_TIMER,
            patterns = listOf(
                Pattern.compile("\\b(?:set a timer for|timer for|start a timer for)\\s+(?<duration>\\d+)\\s*(?<unit>second|minute|hour|sec|min|hr|seconds|minutes|hours|secs|mins|hrs)?\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("timer", "countdown", "seconds", "minutes", "hours")
        ),
        IntentRule(
            intentType = IntentType.MAP_NAVIGATION,
            patterns = listOf(
                Pattern.compile("\\b(?:navigate to|get directions to|show directions to|maps navigate to|drive to)\\s+(?<location>[a-zA-Z0-9\\s,.-]+)", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("navigate", "directions", "maps", "drive", "location", "route")
        ),
        IntentRule(
            intentType = IntentType.LAUNCH_SEARCH,
            patterns = listOf(
                Pattern.compile("\\b(?:search for|google|look up|find information about)\\s+(?<query>[a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("search", "google", "web", "find", "who", "what", "where")
        ),
        IntentRule(
            intentType = IntentType.MEDIA_CONTROL,
            patterns = listOf(
                Pattern.compile("\\b(?<action>play|pause|stop|next|previous|resume)\\s*(?:music|song|track)?\\b", Pattern.CASE_INSENSITIVE)
            ),
            keywords = listOf("play", "pause", "stop", "music", "song", "next", "previous", "track")
        )
    )

    /**
     * Entry point to parse voice input into a structured Intent
     */
    fun parse(text: String): ParsedIntent {
        val originalText = text.trim()
        if (originalText.isEmpty()) {
            return ParsedIntent(IntentType.UNKNOWN, emptyMap(), "", "", 0.0f)
        }

        val normalized = normalizeText(originalText)
        var bestMatch: IntentRule? = null
        var maxConfidence = 0.0f
        var extractedParams = mutableMapOf<String, String>()

        // 1. Try Direct Pattern Matching and Pattern-Based Parameter Extraction
        for (rule in rules) {
            for (pattern in rule.patterns) {
                val matcher = pattern.matcher(normalized)
                if (matcher.find()) {
                    val confidence = 1.0f // Perfect pattern match
                    bestMatch = rule

                    // Extract named capture groups
                    val groupNames = getGroupNames(pattern.pattern())
                    for (name in groupNames) {
                        try {
                            val value = matcher.group(name)
                            if (value != null) {
                                extractedParams[name] = value.trim()
                            }
                        } catch (e: IllegalArgumentException) {
                            // Group not matched in this pattern
                        }
                    }
                    maxConfidence = confidence
                    break
                }
            }
            if (bestMatch != null) break
        }

        // 2. Fallback to Fuzzy / Keyword Overlay Matching if no direct pattern match
        if (bestMatch == null) {
            for (rule in rules) {
                val score = calculateKeywordFuzzyScore(normalized, rule)
                if (score > maxConfidence && score >= 0.45f) {
                    maxConfidence = score
                    bestMatch = rule
                }
            }
        }

        // Post-processing parameters if needed
        val resolvedIntentType = bestMatch?.intentType ?: IntentType.UNKNOWN

        // If no parameters were extracted, do basic rule-specific heuristic extraction
        if (extractedParams.isEmpty() && resolvedIntentType != IntentType.UNKNOWN) {
            extractedParams = extractHeuristicParams(normalized, resolvedIntentType).toMutableMap()
        }

        return ParsedIntent(
            intentType = resolvedIntentType,
            parameters = extractedParams,
            originalText = originalText,
            normalizedText = normalized,
            confidence = if (resolvedIntentType == IntentType.UNKNOWN) 0.0f else maxConfidence
        )
    }

    /**
     * Normalizes text by applying synonym mappings and lowercase formatting
     */
    private fun normalizeText(text: String): String {
        var result = text.lowercase(Locale.getDefault())
        // Replace symbols or clean extra spaces
        result = result.replace(Regex("[?,.!]"), " ")
        result = result.replace(Regex("\\s+"), " ").trim()

        // Apply synonym replacements
        val words = result.split(" ")
        val mappedWords = words.map { word ->
            synonymMap[word] ?: word
        }
        return mappedWords.joinToString(" ")
    }

    /**
     * Returns named capture groups present in regex
     */
    private fun getGroupNames(regex: String): List<String> {
        val groups = mutableListOf<String>()
        val matcher = Pattern.compile("\\(\\?<([a-zA-Z0-9]+)>").matcher(regex)
        while (matcher.find()) {
            matcher.group(1)?.let { groups.add(it) }
        }
        return groups
    }

    /**
     * Calculate score based on keyword overlap and Levenshtein similarity
     */
    private fun calculateKeywordFuzzyScore(text: String, rule: IntentRule): Float {
        val textWords = text.split(" ").filter { it.length > 2 }.toSet()
        if (textWords.isEmpty()) return 0.0f

        val ruleKeywords = rule.keywords.toSet()
        val intersection = textWords.intersect(ruleKeywords)

        val overlapScore = intersection.size.toFloat() / ruleKeywords.size.toFloat()

        // Check if any word in text fuzzy matches rule keywords
        var fuzzyBonus = 0.0f
        for (tWord in text.split(" ")) {
            for (kWord in rule.keywords) {
                val sim = getSimilarity(tWord, kWord)
                if (sim > 0.82f) {
                    fuzzyBonus += 0.2f
                    break
                }
            }
        }

        val finalScore = (overlapScore * 0.7f) + (fuzzyBonus.coerceAtMost(0.3f))
        return finalScore.coerceIn(0.0f, 0.9f)
    }

    /**
     * Levenshtein Distance similarity score (0.0 to 1.0)
     */
    private fun getSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        val maxLen = maxOf(len1, len2)
        return (maxLen - dp[len1][len2]).toFloat() / maxLen.toFloat()
    }

    /**
     * Perform custom keyword-based parameters extraction if regex captures fail
     */
    private fun extractHeuristicParams(text: String, type: IntentType): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val words = text.split(" ")

        when (type) {
            IntentType.OPEN_APP -> {
                // E.g. "open instagram" -> extract "instagram"
                val index = words.indexOfFirst { it == "open" }
                if (index != -1 && index < words.size - 1) {
                    params["app"] = words.drop(index + 1).joinToString(" ")
                }
            }
            IntentType.CALL_CONTACT -> {
                val index = words.indexOfFirst { it == "call" }
                if (index != -1 && index < words.size - 1) {
                    params["contact"] = words.drop(index + 1).joinToString(" ")
                }
            }
            IntentType.SET_ALARM -> {
                // Simple search for digital patterns like 8:30 or numbers
                val pattern = Pattern.compile("(\\d{1,2}:?\\d{0,2}\\s*(?:am|pm)?)", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(text)
                if (matcher.find()) {
                    params["time"] = matcher.group(1) ?: ""
                }
            }
            IntentType.SET_TIMER -> {
                val pattern = Pattern.compile("(\\d+)\\s*(second|minute|hour|sec|min|hr|secs|mins|hrs)?", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(text)
                if (matcher.find()) {
                    params["duration"] = matcher.group(1) ?: ""
                    params["unit"] = matcher.group(2) ?: "minute"
                }
            }
            IntentType.MAP_NAVIGATION -> {
                val index = words.indexOfFirst { it == "navigate" }
                if (index != -1 && index < words.size - 1) {
                    params["location"] = words.drop(index + 1).joinToString(" ")
                }
            }
            IntentType.LAUNCH_SEARCH -> {
                val index = words.indexOfFirst { it == "search" }
                if (index != -1 && index < words.size - 1) {
                    params["query"] = words.drop(index + 1).joinToString(" ")
                }
            }
            IntentType.FLASHLIGHT_TOGGLE -> {
                if (text.contains("on")) {
                    params["state"] = "on"
                } else if (text.contains("off")) {
                    params["state"] = "off"
                }
            }
            IntentType.VOLUME_CONTROL -> {
                if (text.contains("increase") || text.contains("up")) {
                    params["action"] = "increase"
                } else if (text.contains("decrease") || text.contains("down")) {
                    params["action"] = "decrease"
                } else if (text.contains("mute")) {
                    params["state"] = "mute"
                } else if (text.contains("unmute")) {
                    params["state"] = "unmute"
                }
            }
            IntentType.MEDIA_CONTROL -> {
                val action = words.firstOrNull { it in listOf("play", "pause", "stop", "next", "previous", "resume") }
                if (action != null) {
                    params["action"] = action
                }
            }
            else -> {}
        }
        return params
    }
}

private data class IntentRule(
    val intentType: IntentType,
    val patterns: List<Pattern>,
    val keywords: List<String>
)
