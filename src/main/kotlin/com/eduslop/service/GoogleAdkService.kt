package com.eduslop.service

import com.eduslop.models.*
import com.eduslop.util.EnvLoader
import com.google.adk.agents.LlmAgent
import com.google.adk.events.Event
import com.google.adk.runner.Runner
import com.google.adk.sessions.InMemorySessionService
import com.google.genai.types.Content
import com.google.genai.types.Part
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class GoogleAdkService {
    private val logger = LoggerFactory.getLogger(GoogleAdkService::class.java)

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
        .description("An educational AI agent built on Google ADK that designs 2D pixel art micro-lessons, cue cards, and stepped quizzes.")
        .model("gemini-2.0-flash")
        .instruction(
            """
            You are the EDUSLOP Educational Agent built with the Google Agent Development Kit (ADK).
            Your mission is to produce a complete, interactive micro-lesson for teachers to use in class.
            
            Each lesson consists of:
            1. A 2D pixel animation (~60 seconds total duration, split into 3-5 scenes of 10-15 seconds each).
               - Each scene contains:
                 - id: integer (1, 2, 3...)
                 - title: short scene title
                 - durationSeconds: integer (e.g. 15)
                 - narration: engaging scripted dialogue text to be typed in the on-screen retro dialogue box
                 - background: one of ["retro_dark", "blueprint_blue", "terminal_green", "cyber_purple"]
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

    fun generateLesson(
        prompt: String,
        userProvidedApiKey: String? = null,
        designBrief: DesignBrief? = null
    ): Lesson {
        val apiKey = userProvidedApiKey?.takeIf { it.isNotBlank() }
            ?: EnvLoader.get("GEMINI_API_KEY")
            ?: EnvLoader.get("GOOGLE_API_KEY")

        if (apiKey == null) {
            logger.info("Google ADK: No API key supplied. Falling back to built-in lesson generator for: {}", prompt)
            return MockLessonService.createFallbackLessonForPrompt(prompt, designBrief)
        }

        // Ensure both properties are set for ADK and GenAI client internals
        System.setProperty("GEMINI_API_KEY", apiKey)
        System.setProperty("GOOGLE_API_KEY", apiKey)

        try {
            logger.info("Executing Google ADK Agent for prompt: {}", prompt)
            val userId = "teacher_" + UUID.randomUUID().toString().substring(0, 6)
            val sessionId = "session_" + UUID.randomUUID().toString().substring(0, 6)

            // Create in-memory session in ADK
            val session = sessionService.createSession("eduslop_app", userId, null, sessionId).blockingGet()

            val generationInstruction = buildString {
                append("Generate a complete 2D retro pixel educational micro-lesson for the following topic: \"$prompt\"")
                if (designBrief != null) {
                    append("\n\n")
                    append(DesignBriefFormatter.generationContext(designBrief))
                    append("\n\nThe animation, cue cards, and quiz must all serve the agreed audience, objectives, and must-cover content above.")
                }
            }

            val content = Content.fromParts(
                Part.fromText(generationInstruction)
            )

            val flowable = runner.runAsync(session, content, null)

            var lastResponseText = ""
            flowable.blockingForEach { event: Event ->
                val text = event.stringifyContent()
                if (!text.isNullOrBlank()) {
                    lastResponseText = text
                }
            }

            if (lastResponseText.isBlank()) {
                logger.warn("Google ADK returned empty response. Utilizing fallback.")
                return MockLessonService.createFallbackLessonForPrompt(prompt, designBrief)
            }

            val cleanedJson = lastResponseText
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
            logger.error("Google ADK execution encountered error: {}. Utilizing resilient fallback.", e.message, e)
            return MockLessonService.createFallbackLessonForPrompt(prompt, designBrief)
        }
    }
}
