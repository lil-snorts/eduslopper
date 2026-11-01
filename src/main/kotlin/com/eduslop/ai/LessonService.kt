package com.eduslop.ai

import com.eduslop.models.DesignBrief
import com.eduslop.models.Lesson

/**
 * The public port for micro-lesson generation. Implementations (live Google
 * ADK/Gemini and deterministic mock) are internal to the ai package and chosen once at
 * wiring time via [LessonServiceFactory], so request handlers never inspect
 * environment configuration.
 */
interface LessonService {
    /** "mock" or "live"; reported by /api/config and /api/health. */
    val mode: String

    /** Configured generation model name; reported for observability even in mock mode. */
    val model: String
        get() = AiConfig.model()

    /** Whether a server-level Gemini API key (.env or environment) is present. */
    val serverKeyConfigured: Boolean
        get() = AiConfig.apiKey(null) != null

    fun generateLesson(
        prompt: String,
        userProvidedApiKey: String? = null,
        designBrief: DesignBrief? = null
    ): Lesson
}

/**
 * Wiring point for the AI module. Mock output is used only when USE_MOCK is
 * explicitly enabled (see [AiConfig]); live generation never silently
 * degrades to mock content.
 */
object LessonServiceFactory {
    fun create(): LessonService =
        if (AiConfig.useMock()) MockLessonService() else GoogleAdkService()

    /** The deterministic DDD showcase lesson, seeded at startup in either mode. */
    fun showcaseLesson(): Lesson = MockLessonService().createDddLesson()
}
