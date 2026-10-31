package com.eduslop.service

import com.eduslop.models.ClarifyingQuestion

import com.eduslop.models.DesignBeat
import com.eduslop.models.DesignBrief
import com.eduslop.models.DesignBriefResponse
import com.eduslop.models.DesignIterationRequest
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

private val TUTOR_INSTRUCTION = """
You are EDUSLOP's Design Tutor, an expert instructional designer working with a teacher BEFORE any lesson content is generated.
Your job is to turn a rough prompt into a shared lesson design brief that both you and the teacher agree on.

A complete brief covers:
- topic
- audience (age, grade, or experience level)
- learning objective(s): what learners should be able to do afterwards
- prior knowledge
- must-cover content: key vocabulary, a concrete example, or a known misconception
- a short proposed outline of 3-5 lesson beats
- tone / visual theme for the retro pixel lesson
- explicit assumptions

Ask clarifying questions ONLY for intent that is genuinely missing and would materially change the lesson.
Priority order: audience, objective, prior knowledge, must-cover content or misconception, tone.
Do not ask for information already present in the prompt or previousBrief answers.
Do not ask generic questions such as "what else?" or "tell me more".
First pass: ask at most 4 questions. Later passes: ask at most 2.
If answers are supplied, incorporate them and prefer an explicit assumption over asking the same thing again.
If the prompt is already detailed enough, return no clarifying questions.
Each clarifying question must have a stable snake_case id (for example audience, objective, prior_knowledge, must_cover, tone), a short whyItMatters, and 2-4 concrete selectable suggestions.

Return pure JSON only, matching this exact structure:
{
  "tutorMessage": "One or two friendly sentences about what you inferred and what you still need.",
  "topic": "Short lesson topic",
  "summary": "One or two sentence design summary",
  "audience": "Who the lesson is for",
  "learningObjectives": ["Concrete objective"],
  "tone": "Retro visual tone",
  "keyConcepts": ["Key concept or must-cover item"],
  "proposedOutline": [
    { "title": "Hook", "purpose": "Why the topic matters to this audience." },
    { "title": "Core idea", "purpose": "The central concept visualised." }
  ],
  "assumptions": ["Explicit assumption you made"],
  "clarifyingQuestions": [
    {
      "id": "audience",
      "question": "Who is this lesson for?",
      "whyItMatters": "Audience changes vocabulary and difficulty.",
      "suggestions": ["Middle school (ages 11-14)", "High school (ages 14-18)"],
      "required": true
    }
  ],
  "revisedPrompt": "A self-contained prompt that includes the agreed topic, audience, objectives, prior knowledge, must-cover content, tone, and constraints."
}
""".trimIndent()

/**
 * Tutor that iterates the initial lesson design with the teacher before content
 * generation. Its output is a [DesignBrief] which becomes the contract for the
 * generator, so questions answered here are honoured by the final lesson.
 */
class LessonTutorService {
    private val logger = LoggerFactory.getLogger(LessonTutorService::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val sessionService = InMemorySessionService()

    /**
     * Google ADK agent responsible for the design-alignment conversation.
     */
    val designTutorAgent: LlmAgent = LlmAgent.builder()
        .name("eduslop_design_tutor_agent")
        .description("An instructional-design tutor that clarifies a teacher's intent and produces an agreed lesson design brief before generation.")
        .model(AiConfig.model())
        .instruction(
            """
            You are EDUSLOP's Design Tutor, an expert instructional designer working with a teacher BEFORE any lesson content is generated.
            Your job is to turn a rough prompt into a shared lesson design brief that both you and the teacher agree on.

            A complete brief covers:
            - topic
            - audience (age, grade, or experience level)
            - learning objective(s): what learners should be able to do afterwards
            - prior knowledge
            - must-cover content: key vocabulary, a concrete example, or a known misconception
            - a short proposed outline of 3-5 lesson beats
            - tone / visual theme for the retro pixel lesson
            - explicit assumptions

            Ask clarifying questions ONLY for intent that is genuinely missing and would materially change the lesson.
            Priority order: audience, objective, prior knowledge, must-cover content or misconception, tone.
            Do not ask for information already present in the prompt or previousBrief answers.
            Do not ask generic questions such as "what else?" or "tell me more".
            First pass: ask at most 4 questions. Later passes: ask at most 2.
            If answers are supplied, incorporate them and prefer an explicit assumption over asking the same thing again.
            If the prompt is already detailed enough, return no clarifying questions.
            Each clarifying question must have a stable snake_case id (for example audience, objective, prior_knowledge, must_cover, tone), a short whyItMatters, and 2-4 concrete selectable suggestions.

            Return pure JSON only, matching this exact structure:
            {
              "tutorMessage": "One or two friendly sentences about what you inferred and what you still need.",
              "topic": "Short lesson topic",
              "summary": "One or two sentence design summary",
              "audience": "Who the lesson is for",
              "learningObjectives": ["Concrete objective"],
              "tone": "Retro visual tone",
              "keyConcepts": ["Key concept or must-cover item"],
              "proposedOutline": [
                { "title": "Hook", "purpose": "Why the topic matters to this audience." },
                { "title": "Core idea", "purpose": "The central concept visualised." }
              ],
              "assumptions": ["Explicit assumption you made"],
              "clarifyingQuestions": [
                {
                  "id": "audience",
                  "question": "Who is this lesson for?",
                  "whyItMatters": "Audience changes vocabulary and difficulty.",
                  "suggestions": ["Middle school (ages 11-14)", "High school (ages 14-18)"],
                  "required": true
                }
              ],
              "revisedPrompt": "A self-contained prompt that includes the agreed topic, audience, objectives, prior knowledge, must-cover content, tone, and constraints."
            }
            """.trimIndent()
        )
        .build()

    private val runner = Runner(designTutorAgent, APP_NAME, null, sessionService)

    suspend fun iterateDesign(request: DesignIterationRequest): DesignBriefResponse {
        if (AiConfig.useMock()) {
            logger.info("USE_MOCK=true: using deterministic design tutor for prompt: {}", request.prompt)
            return FallbackDesignTutor.iterate(request)
        }

        val apiKey = AiConfig.requireApiKey(request.apiKey)

        System.setProperty("GEMINI_API_KEY", apiKey)
        System.setProperty("GOOGLE_API_KEY", apiKey)

        return try {
            logger.info(
                "Executing design tutor ({}) pass {} for prompt: {}",
                AiConfig.model(),
                request.iteration + 1,
                request.prompt
            )
            val lastResponseText = GeminiService().generateText(
                systemPrompt = TUTOR_INSTRUCTION,
                userPrompt = buildTutorInput(request),
                apiKey = apiKey
            )

            val cleanedJson = lastResponseText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val payload = json.decodeFromString<TutorPayload>(cleanedJson)
            val mergedAnswers = buildMap {
                putAll(request.previousBrief?.answers.orEmpty())
                putAll(request.answers)
            }.filterValues { it.isNotBlank() }
            val teacherContext = request.additionalContext?.takeIf { it.isNotBlank() }
                ?: request.previousBrief?.additionalContext?.takeIf { it.isNotBlank() }

            // Never re-ask something the teacher has already answered.
            val questions = payload.clarifyingQuestions
                .filter { mergedAnswers[it.id].isNullOrBlank() }
                .distinctBy { it.id }
                .take(if (request.iteration == 0) 4 else 2)

            val brief = DesignBrief(
                topic = payload.topic.ifBlank { request.previousBrief?.topic ?: request.prompt.take(80) },
                summary = payload.summary,
                audience = payload.audience,
                learningObjectives = payload.learningObjectives,
                tone = payload.tone,
                keyConcepts = payload.keyConcepts,
                proposedOutline = payload.proposedOutline,
                assumptions = payload.assumptions,
                clarifyingQuestions = questions,
                revisedPrompt = payload.revisedPrompt,
                answers = mergedAnswers,
                additionalContext = teacherContext
            )

            val ready = questions.isEmpty()
            val tutorMessage = payload.tutorMessage.ifBlank {
                if (ready) {
                    "The design is aligned. Review the brief, then generate the lesson."
                } else {
                    "I need ${questions.size} more detail${if (questions.size == 1) "" else "s"} before the design is aligned."
                }
            }

            DesignBriefResponse(
                brief = brief,
                tutorMessage = tutorMessage,
                iteration = request.iteration + 1,
                readyToGenerate = ready
            )
        } catch (e: AiGenerationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Design tutor execution failed: {}", e.message, e)
            throw AiGenerationException(
                "Tutor review failed using model '${AiConfig.model()}': ${e.message ?: e.javaClass.simpleName}",
                e
            )
        }
    }

    private fun buildTutorInput(request: DesignIterationRequest): String {
        // Never send the API key to the model as part of the conversation payload.
        val sanitized = request.copy(apiKey = null)
        return buildString {
            appendLine("Iterate the lesson design for this teacher request.")
            appendLine()
            appendLine("Request payload (JSON):")
            appendLine(json.encodeToString(DesignIterationRequest.serializer(), sanitized))
            appendLine()
            if (request.previousBrief == null) {
                appendLine("This is the first design pass. Ask focused clarifying questions if the prompt lacks audience, objective, prior knowledge, or must-cover content.")
            } else {
                appendLine("This is a later design pass. Incorporate the supplied answers and previousBrief, then ask only strictly necessary follow-ups.")
            }
        }
    }

    @Serializable
    private data class TutorPayload(
        val tutorMessage: String = "",
        val topic: String = "",
        val summary: String = "",
        val audience: String = "",
        val learningObjectives: List<String> = emptyList(),
        val tone: String = "",
        val keyConcepts: List<String> = emptyList(),
        val proposedOutline: List<DesignBeat> = emptyList(),
        val assumptions: List<String> = emptyList(),
        val clarifyingQuestions: List<ClarifyingQuestion> = emptyList(),
        val revisedPrompt: String = ""
    )

    private companion object {
        const val APP_NAME = "eduslop_tutor_app"
    }
}
