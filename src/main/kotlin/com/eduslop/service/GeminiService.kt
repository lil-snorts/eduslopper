package com.eduslop.service

import com.eduslop.models.*
import com.eduslop.util.EnvLoader
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
private data class GenerationConfig(
    val response_mime_type: String = "application/json",
    val temperature: Double = 0.7
)

@Serializable
private data class GeminiRequestBody(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig = GenerationConfig()
)

@Serializable
private data class GeminiCandidate(val content: GeminiContent)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

class GeminiService {
    private val logger = LoggerFactory.getLogger(GeminiService::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@GeminiService.json)
        }
    }

    suspend fun generateLesson(prompt: String, userProvidedApiKey: String? = null): Lesson {
        val apiKey = userProvidedApiKey?.takeIf { it.isNotBlank() }
            ?: EnvLoader.get("GEMINI_API_KEY")
            ?: EnvLoader.get("GOOGLE_API_KEY")

        if (apiKey == null) {
            logger.info("No Gemini API key supplied. Utilizing intelligent mock generation for: {}", prompt)
            return MockLessonService.createFallbackLessonForPrompt(prompt)
        }

        try {
            logger.info("Calling Gemini API for prompt: {}", prompt)
            val systemPrompt = """
                You are an expert educational content creator designing an interactive 2D pixel art micro-lesson for students.
                The lesson consists of:
                1. A 2D pixel animation (~60 seconds total, divided into 3 to 5 scenes, each 10-15s).
                   - Each scene must have:
                     - id (integer 1, 2, ...)
                     - title (string)
                     - durationSeconds (integer, e.g. 12 to 15)
                     - narration (engaging script text to be displayed in a dialogue box matching the visuals)
                     - background: one of ["retro_dark", "blueprint_blue", "terminal_green", "cyber_purple"]
                     - entities: list of 2-4 pixel entities with id, name, sprite (one of: "character", "database", "aggregate", "service", "bounded_context", "event", "box", "user", "cloud", "model"), x (10 to 90), y (20 to 80), color (hex e.g. "#00f0ff"), label.
                     - actions: list of 1-3 simple actions with type ("move", "pulse", "connect", "highlight"), target (entity id), toX, toY, duration (seconds), label.
                2. Cue cards (4 to 6 flashcards):
                   - id, title, frontPrompt (question/concept), backAnswer (clear concise answer), category.
                3. Multiple-choice Quiz (3 to 5 questions):
                   - id, question, options (list of 4 distinct answers), correctOptionIndex (0 to 3), explanation (why the answer is right).
                
                Format your entire response as a valid JSON object strictly matching this schema:
                {
                   "topic": "The exact topic name",
                   "summary": "Short 2-sentence summary of the micro-lesson",
                   "animation": {
                      "totalDurationSeconds": 60,
                      "scenes": [ ... ]
                   },
                   "cueCards": [ ... ],
                   "quiz": [ ... ]
                }
            """.trimIndent()

            val requestBody = GeminiRequestBody(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart("$systemPrompt\n\nGenerate the complete educational micro-lesson for the following topic: \"$prompt\"")
                        )
                    )
                )
            )

            // Primary model endpoint with fallback
            val modelName = "gemini-1.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val httpResponse: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                logger.error("Gemini API error ({}): {}", httpResponse.status, errorBody)
                return MockLessonService.createFallbackLessonForPrompt(prompt)
            }

            val geminiResponse = httpResponse.body<GeminiResponse>()
            val rawText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (rawText.isNullOrBlank()) {
                logger.warn("Gemini returned empty candidate. Falling back.")
                return MockLessonService.createFallbackLessonForPrompt(prompt)
            }

            // Clean json in case Markdown code block fences are present
            val cleanedJson = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            @Serializable
            data class GeneratedPayload(
                val topic: String,
                val summary: String,
                val animation: PixelAnimation,
                val cueCards: List<CueCard>,
                val quiz: List<QuizQuestion>
            )

            val payload = json.decodeFromString<GeneratedPayload>(cleanedJson)
            return Lesson(
                id = UUID.randomUUID().toString().substring(0, 8),
                topic = payload.topic,
                summary = payload.summary,
                animation = payload.animation,
                cueCards = payload.cueCards,
                quiz = payload.quiz
            )
        } catch (e: Exception) {
            logger.error("Failed to generate lesson via Gemini API: {}", e.message, e)
            return MockLessonService.createFallbackLessonForPrompt(prompt)
        }
    }
}
