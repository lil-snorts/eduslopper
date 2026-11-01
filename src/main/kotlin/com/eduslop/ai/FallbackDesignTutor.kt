package com.eduslop.ai

import com.eduslop.models.ClarifyingQuestion
import com.eduslop.models.DesignBeat
import com.eduslop.models.DesignBrief
import com.eduslop.models.DesignBriefResponse
import com.eduslop.models.DesignIterationRequest

/**
 * Deterministic design-review fallback used when no Gemini key is available or
 * when the model call fails. It mirrors the LLM tutor's behaviour: infer what it
 * can from the prompt, ask only for genuinely missing intent, and make every
 * remaining assumption explicit in the shared brief.
 */
internal object FallbackDesignTutor {

    private val questionBank = listOf(
        ClarifyingQuestion(
            id = "audience",
            question = "Who is this lesson for?",
            whyItMatters = "Audience sets the vocabulary, examples, pacing, and difficulty of the quiz.",
            suggestions = listOf(
                "Upper elementary (ages 9-11)",
                "Middle school (ages 11-14)",
                "High school (ages 14-18)",
                "Adult / professional learners"
            )
        ),
        ClarifyingQuestion(
            id = "objective",
            question = "What should learners be able to do after this lesson?",
            whyItMatters = "A concrete objective keeps the animation, cue cards, and quiz pointed at the same outcome.",
            suggestions = listOf(
                "Explain the core idea in their own words",
                "Apply it to a real-world example",
                "Compare it with a common alternative",
                "Spot and correct a common misconception"
            )
        ),
        ClarifyingQuestion(
            id = "prior_knowledge",
            question = "What can we assume learners already know?",
            whyItMatters = "This prevents the lesson from either repeating basics or skipping necessary foundations.",
            suggestions = listOf(
                "No prior knowledge assumed",
                "Basic terminology only",
                "Comfortable with the fundamentals",
                "Advanced - skip the basics"
            )
        ),
        ClarifyingQuestion(
            id = "must_cover",
            question = "Is there a specific example, term, or misconception this lesson must cover?",
            whyItMatters = "Named content prevents a generic lesson that misses the reason you asked for this topic.",
            suggestions = listOf(
                "A concrete worked example",
                "Key vocabulary and definitions",
                "A common mistake to avoid",
                "How it connects to the bigger picture"
            )
        )
    )

    private val audiencePatterns = listOf(
        "upper elementary" to "Upper elementary (ages 9-11)",
        "elementary" to "Elementary learners",
        "primary school" to "Primary school learners",
        "middle school" to "Middle school (ages 11-14)",
        "high school" to "High school (ages 14-18)",
        "secondary school" to "Secondary school learners",
        "university" to "University learners",
        "college" to "College learners",
        "undergrad" to "Undergraduate learners",
        "adult learner" to "Adult learners",
        "professional" to "Professional learners",
        "beginner" to "Beginner learners",
        "advanced" to "Advanced learners",
        "kids" to "Young learners"
    )

    fun iterate(request: DesignIterationRequest): DesignBriefResponse {
        val previous = request.previousBrief
        val answers = buildMap {
            putAll(previous?.answers.orEmpty())
            putAll(request.answers)
        }.filterValues { it.isNotBlank() }
        val teacherContext = request.additionalContext?.takeIf { it.isNotBlank() }
            ?: previous?.additionalContext?.takeIf { it.isNotBlank() }

        val wordCount = request.prompt.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val topic = previous?.topic?.takeIf { it.isNotBlank() } ?: deriveTopic(request.prompt)
        val audience = answers["audience"] ?: inferAudience(request.prompt)
        val objective = answers["objective"] ?: inferObjective(request.prompt)
        val priorKnowledge = answers["prior_knowledge"] ?: inferPriorKnowledge(request.prompt)
        val mustCover = answers["must_cover"]?.takeIf { it.isNotBlank() }
        val tone = "Playful retro arcade"

        val missing = buildList {
            if (audience == null) add("audience")
            if (objective == null) add("objective")
            if (priorKnowledge == null) add("prior_knowledge")
            // A detailed prompt is treated as already scoped by the teacher.
            if (mustCover == null && wordCount < 15) add("must_cover")
        }
        val maxQuestions = if (request.iteration == 0) 3 else 1
        val questions = questionBank.filter { it.id in missing }.take(maxQuestions)

        val resolvedAudience = audience ?: "General classroom learners (inferred)"
        val resolvedObjective = objective ?: "Explain the core idea in their own words"
        val resolvedPriorKnowledge = priorKnowledge ?: "No prior knowledge assumed"
        val objectives = if (objective != null) listOf(objective) else emptyList()

        val assumptions = buildList {
            if (audience == null) add("Audience assumed: $resolvedAudience")
            if (objective == null) add("Objective assumed: $resolvedObjective")
            if (priorKnowledge == null) add("Prior knowledge assumed: $resolvedPriorKnowledge")
            if (mustCover == null && wordCount < 15) add("Scope assumed: focus on the core idea named in the prompt")
            add("Lesson fits a 5-15 minute classroom slot")
            add("Retro pixel visuals are a presentation style, not additional curriculum")
        }

        val proposedOutline = listOf(
            DesignBeat(
                title = "Hook: why it matters",
                purpose = "Show $resolvedAudience why $topic is worth understanding."
            ),
            DesignBeat(
                title = "Core idea",
                purpose = "Visually explain the main concept behind $topic."
            ),
            DesignBeat(
                title = "Concrete example",
                purpose = mustCover ?: "Walk through one clear, concrete example of $topic."
            ),
            DesignBeat(
                title = "Knowledge check",
                purpose = "Let learners apply the idea so the quiz checks understanding, not recall."
            )
        )

        val brief = DesignBrief(
            topic = topic,
            summary = "A PlayStation 1 / GameCube styled micro-lesson on $topic for $resolvedAudience. Learning objective: $resolvedObjective.",
            audience = resolvedAudience,
            learningObjectives = objectives.ifEmpty { listOf(resolvedObjective) },
            tone = tone,
            keyConcepts = mustCover?.let { listOf(it) }.orEmpty(),
            proposedOutline = proposedOutline,
            assumptions = assumptions,
            clarifyingQuestions = questions,
            revisedPrompt = buildRevisedPrompt(
                topic = topic,
                audience = resolvedAudience,
                objectives = objectives.ifEmpty { listOf(resolvedObjective) },
                priorKnowledge = resolvedPriorKnowledge,
                mustCover = mustCover,
                tone = tone,
                teacherContext = teacherContext
            ),
            answers = answers,
            additionalContext = teacherContext
        )

        val tutorMessage = when {
            questions.isEmpty() ->
                "The design is aligned. I have enough context to build a lesson for $resolvedAudience aimed at: ${resolvedObjective.lowercase()}."
            request.iteration == 0 ->
                "I can sketch this lesson, but I need ${questions.size} detail${if (questions.size == 1) "" else "s"} to keep it aligned with your intent. Answer what you can, or generate now and I will make the assumptions explicit."
            else ->
                "Thanks - I have folded your answers into the brief. I only need ${questions.size} more detail${if (questions.size == 1) "" else "s"} before we lock the design."
        }

        return DesignBriefResponse(
            brief = brief,
            tutorMessage = tutorMessage,
            iteration = request.iteration + 1,
            readyToGenerate = questions.isEmpty()
        )
    }

    private fun deriveTopic(prompt: String): String {
        var topic = prompt.trim()
        topic = topic.replace(Regex("^(please\\s+)?(create|make|build|design|generate)\\s+(a|an|the)?\\s*(micro[- ]?lesson|lesson|module)\\s+(about|on|for|covering)\\s+", RegexOption.IGNORE_CASE), "")
        topic = topic.replace(Regex("^(how (does|do|did)|what is|what are|explain|teach me about|tell me about)\\s+", RegexOption.IGNORE_CASE), "")
        topic = topic.removeSuffix("?").removeSuffix(".").removeSuffix("!").trim()
        topic = topic.replace(Regex("\\s+work$", RegexOption.IGNORE_CASE), "").trim()
        if (topic.length > 80) topic = topic.take(77).trimEnd() + "..."
        return topic.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            .ifBlank { "Untitled Lesson" }
    }

    private fun inferAudience(prompt: String): String? {
        val lower = prompt.lowercase()
        audiencePatterns.firstOrNull { lower.contains(it.first) }?.let { return it.second }
        val grade = Regex("\\b(?:grade|year)\\s+(\\d{1,2})\\b", RegexOption.IGNORE_CASE).find(prompt)
        if (grade != null) return "Grade ${grade.groupValues[1]} learners"
        val ages = Regex("\\bages?\\s+(\\d{1,2}\\s*(?:-|to)\\s*\\d{1,2})\\b", RegexOption.IGNORE_CASE).find(prompt)
        if (ages != null) return "Learners aged ${ages.groupValues[1]}"
        return null
    }

    private fun inferObjective(prompt: String): String? {
        val patterns = listOf(
            Regex("\\b(?:so\\s+(?:that\\s+)?(?:they|students?|learners?)\\s+can|students?\\s+(?:will|should)\\s+be\\s+able\\s+to|learners?\\s+(?:will|should)\\s+be\\s+able\\s+to|goal\\s+is\\s+to|objective\\s*(?:is|:)\\s*)\\s*([^.;]+)", RegexOption.IGNORE_CASE),
            Regex("\\b(?:help|teach)\\s+(?:students?|learners?|pupils?)\\s+(?:to\\s+)?(understand|explain|apply|compare|identify|recognise|recognize|describe|use|build|calculate|solve)\\s+([^.;]+)", RegexOption.IGNORE_CASE),
            Regex("\\b(?:learn|understand|explain|apply|compare|describe|use|build|calculate|solve)\\s+([^.;]+)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(prompt) ?: continue
            val verb = if (match.groupValues.size > 2 && match.groupValues[1].lowercase() in setOf(
                    "understand", "explain", "apply", "compare", "identify", "recognise", "recognize",
                    "describe", "use", "build", "calculate", "solve"
                )) match.groupValues[1] else ""
            val remainder = match.groupValues.last().trim()
            if (remainder.isBlank()) continue
            return listOf(verb, remainder).filter { it.isNotBlank() }.joinToString(" ").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
        return null
    }

    private fun inferPriorKnowledge(prompt: String): String? {
        val lower = prompt.lowercase()
        return when {
            Regex("\\b(no|without)\\s+(prior|previous|any)\\s+(?:\\w+\\s+){0,2}(knowledge|experience|background)", RegexOption.IGNORE_CASE).containsMatchIn(prompt) ->
                "No prior knowledge assumed"
            lower.contains("prior knowledge") || lower.contains("already know") || lower.contains("familiar with") ->
                "Prior knowledge described in the prompt"
            lower.contains("advanced") -> "Comfortable with the fundamentals"
            else -> null
        }
    }

    private fun buildRevisedPrompt(
        topic: String,
        audience: String,
        objectives: List<String>,
        priorKnowledge: String,
        mustCover: String?,
        tone: String,
        teacherContext: String?
    ): String = buildString {
        appendLine("Create a 5-15 minute PlayStation 1 / GameCube styled micro-lesson about \"$topic\".")
        appendLine("Audience: $audience.")
        appendLine("Learning objectives: ${objectives.joinToString("; ")}.")
        if (!mustCover.isNullOrBlank()) appendLine("Must cover: $mustCover.")
        appendLine("Prior knowledge: $priorKnowledge.")
        appendLine("Tone: $tone.")
        if (!teacherContext.isNullOrBlank()) appendLine("Teacher context: $teacherContext.")
        appendLine("Include a ~60-second animation (3-5 scenes), 4-6 cue cards, and 3-5 multiple-choice quiz questions.")
    }.trim()
}
