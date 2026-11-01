package com.eduslop.ai

import com.eduslop.models.*
import com.google.adk.agents.LlmAgent
import kotlinx.coroutines.runBlocking
import com.google.adk.events.Event
import com.google.adk.runner.Runner
import com.google.adk.sessions.InMemorySessionService
import com.google.genai.types.Content
import com.google.genai.types.Part
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

internal class GoogleAdkService : LessonService {
    private val logger = LoggerFactory.getLogger(GoogleAdkService::class.java)

    override val mode: String = "live"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val sessionService = InMemorySessionService()

    /**
     * Google ADK (Agent Development Kit) Root Agent definition.
     * Uses LlmAgent configured for the EDUSLOP micro-lesson architectural generation.
     */
    val rootAgent: LlmAgent = LlmAgent.builder()
        .name("eduslop_retro_educator_agent")
        .description("An educational AI agent built on Google ADK that designs PlayStation 1 / GameCube styled graphics micro-lessons, cue cards, and stepped quizzes.")
        .model(AiConfig.model())
        .instruction(
            """
            You are the EDUSLOP Educational Agent built with the Google Agent Development Kit (ADK).
            Your mission is to produce a complete, interactive micro-lesson for teachers to use in class.
            
            Each lesson consists of:
            1. A PlayStation 1 / GameCube styled animation (~60 seconds total duration, split into 3-5 scenes of 10-15 seconds each).
               - Each scene contains:
                 - id: integer (1, 2, 3...)
                 - title: short scene title
                 - durationSeconds: integer (e.g. 15)
                 - narration: engaging scripted dialogue text to be typed in the on-screen retro dialogue box
                 - background: a free-form visual theme name inferred from the lesson and user input (PlayStation 1 / GameCube styled graphics); any string is allowed
                 - entities: 2-4 retro pixel sprites with id, name, sprite (one of: ["character", "database", "aggregate", "service", "bounded_context", "event", "box", "user", "cloud", "model"]), x (0-100), y (0-100), color, label
                 - actions: 1-3 simple actions with type ("move", "pulse", "connect", "highlight"), target, toX, toY, duration
            2. Cue Cards (4 to 6 flashcards):
               - id, title, frontPrompt (concept/question), backAnswer (clear takeaway), category
            3. Stepped Multi-Choice Quiz (3 to 5 questions):
               - id, question, options (list of 4 distinct answers), correctOptionIndex (0 to 3), explanation
            
            Output your response as pure JSON matching this exact structure:
            {
               "topic": "Topic Name",
               "summary": "2-sentence lesson summary",
               "animation": {
                  "totalDurationSeconds": 60,
                  "scenes": [ ... ]
               },
               "cueCards": [ ... ],
               "quiz": [ ... ]
            }
            """.trimIndent()
        )
        .build()

    private val runner = Runner(rootAgent, "eduslop_app", null, sessionService)

    override fun generateLesson(
        prompt: String,
        userProvidedApiKey: String?,
        designBrief: DesignBrief?
    ): Lesson {
        val apiKey = AiConfig.requireApiKey(userProvidedApiKey)

        // Ensure both properties are set for client internals.
        System.setProperty("GEMINI_API_KEY", apiKey)
        System.setProperty("GOOGLE_API_KEY", apiKey)

        return try {
            logger.info("Generating lesson with {} for prompt: {}", AiConfig.model(), prompt)
            runBlocking { GeminiService().generateLesson(prompt, apiKey, designBrief) }
        } catch (e: AiGenerationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Lesson generation failed for model '{}': {}", AiConfig.model(), e.message, e)
            throw AiGenerationException(
                "Gemini generation failed using model '${AiConfig.model()}': ${e.message ?: e.javaClass.simpleName}",
                e
            )
        }
    }
}
