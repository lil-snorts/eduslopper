package com.eduslop.ai

/**
 * Public error contract of the AI module, thrown by [LessonService] and
 * [DesignTutorService] implementations and mapped to HTTP responses by the
 * server's StatusPages.
 */
class MissingApiKeyException : RuntimeException(
    "No Gemini API key configured. Add a key in the UI, set GEMINI_API_KEY in .env, " +
        "or start the server with USE_MOCK=true for offline demo mode."
)

class AiGenerationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
