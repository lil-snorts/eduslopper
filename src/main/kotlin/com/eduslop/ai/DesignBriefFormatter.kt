package com.eduslop.ai

import com.eduslop.models.DesignBrief

/**
 * Formats the teacher-approved [DesignBrief] into authoritative context for the
 * lesson generator. Keeping this in one place ensures the ADK agent, the direct
 * Gemini client, and the offline fallback all honour the same design contract.
 */
internal object DesignBriefFormatter {

    fun generationContext(brief: DesignBrief): String = buildString {
        appendLine("AGREED LESSON DESIGN BRIEF (this is authoritative - do not contradict it):")
        appendLine("Topic: ${brief.topic}")
        if (brief.summary.isNotBlank()) appendLine("Design summary: ${brief.summary}")
        if (brief.audience.isNotBlank()) appendLine("Audience: ${brief.audience}")
        if (brief.learningObjectives.isNotEmpty()) {
            appendLine("Learning objectives:")
            brief.learningObjectives.forEach { appendLine("- $it") }
        }
        if (brief.keyConcepts.isNotEmpty()) {
            appendLine("Must-cover key concepts:")
            brief.keyConcepts.forEach { appendLine("- $it") }
        }
        if (brief.proposedOutline.isNotEmpty()) {
            appendLine("Proposed lesson beats:")
            brief.proposedOutline.forEachIndexed { index, beat ->
                val purpose = beat.purpose.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty()
                appendLine("${index + 1}. ${beat.title}$purpose")
            }
        }
        if (brief.tone.isNotBlank()) appendLine("Tone / visual theme: ${brief.tone}")
        if (brief.assumptions.isNotEmpty()) {
            appendLine("Explicit assumptions:")
            brief.assumptions.forEach { appendLine("- $it") }
        }

        val askedQuestions = brief.clarifyingQuestions
        if (askedQuestions.isNotEmpty()) {
            appendLine("Clarifying Q&A:")
            askedQuestions.forEach { question ->
                val answer = brief.answers[question.id]?.takeIf { it.isNotBlank() }
                    ?: "Not answered - use the assumptions above"
                appendLine("- Q: ${question.question}")
                appendLine("  A: $answer")
            }
        }

        val knownQuestionIds = askedQuestions.map { it.id }.toSet()
        val extraAnswers = brief.answers.filterKeys { it !in knownQuestionIds }
        if (extraAnswers.isNotEmpty()) {
            appendLine("Additional teacher decisions:")
            extraAnswers.forEach { (id, answer) ->
                appendLine("- ${id.replace('_', ' ')}: $answer")
            }
        }

        if (!brief.additionalContext.isNullOrBlank()) {
            appendLine("Teacher's extra context: ${brief.additionalContext}")
        }
        if (brief.revisedPrompt.isNotBlank()) {
            appendLine("Tutor's revised prompt:")
            appendLine(brief.revisedPrompt)
        }
    }.trim()
}
