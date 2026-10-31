package com.eduslop.service

import com.eduslop.util.EnvLoader

/**
 * Central place for deciding whether a request should call Gemini or use the
 * deterministic mock lesson.
 *
 * Requirements:
 * - A live Gemini call always needs an API key (from the request, `.env`, or env vars).
 * - The mock is only used when `USE_MOCK=true` is set explicitly.
 * - API failures never silently degrade to mock output; the caller gets an error.
 */
object AiConfig {
    const val DEFAULT_MODEL = "gemini-3.8-flash"

    fun useMock(): Boolean {
        val value = EnvLoader.get("USE_MOCK")?.trim()?.lowercase() ?: return false
        return value == "true" || value == "1" || value == "yes" || value == "on"
    }

    fun model(): String =
        EnvLoader.get("GEMINI_MODEL")?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_MODEL

    /**
     * Ordered fallback models used when the primary model is unavailable or
     * overloaded. The primary model is never duplicated here.
     */
    fun fallbackModels(): List<String> =
        listOf("gemini-3.6-flash", "gemini-3.5-flash-lite")
            .filter { it != model() }

    fun apiKey(userProvided: String?): String? =
        userProvided?.trim()?.takeIf { it.isNotEmpty() }
            ?: EnvLoader.get("GEMINI_API_KEY")
            ?: EnvLoader.get("GOOGLE_API_KEY")

    /**
     * Returns the key for a live call, or throws a clear error telling the
     * operator how to configure the application.
     */
    fun requireApiKey(userProvided: String?): String =
        apiKey(userProvided) ?: throw MissingApiKeyException()
}

class MissingApiKeyException : RuntimeException(
    "No Gemini API key configured. Add a key in the UI, set GEMINI_API_KEY in .env, " +
        "or start the server with USE_MOCK=true for offline demo mode."
)

class AiGenerationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
