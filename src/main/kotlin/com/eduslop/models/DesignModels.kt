@file:OptIn(ExperimentalSerializationApi::class)

package com.eduslop.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A single question the tutor needs answered before the lesson design is aligned
 * with the teacher's intent.
 */
@Serializable
data class ClarifyingQuestion(
    val id: String,
    val question: String,
    @EncodeDefault val whyItMatters: String = "",
    @EncodeDefault val suggestions: List<String> = emptyList(),
    @EncodeDefault val required: Boolean = true
)

/**
 * One proposed beat of the lesson design, visible to the teacher before
 * generation so the intended content can be reviewed and corrected.
 */
@Serializable
data class DesignBeat(
    val title: String,
    @EncodeDefault val purpose: String = ""
)

/**
 * The shared design contract between the tutor and the teacher. Generation uses
 * this brief (including answers and extra context) as the source of truth.
 */
@Serializable
data class DesignBrief(
    val topic: String,
    @EncodeDefault val summary: String = "",
    @EncodeDefault val audience: String = "",
    @EncodeDefault val learningObjectives: List<String> = emptyList(),
    @EncodeDefault val tone: String = "",
    @EncodeDefault val keyConcepts: List<String> = emptyList(),
    @EncodeDefault val proposedOutline: List<DesignBeat> = emptyList(),
    @EncodeDefault val assumptions: List<String> = emptyList(),
    @EncodeDefault val clarifyingQuestions: List<ClarifyingQuestion> = emptyList(),
    @EncodeDefault val revisedPrompt: String = "",
    @EncodeDefault val answers: Map<String, String> = emptyMap(),
    @EncodeDefault val additionalContext: String? = null
) {
    fun unansweredQuestions(): List<ClarifyingQuestion> =
        clarifyingQuestions.filter { answers[it.id].isNullOrBlank() }

    fun isAligned(): Boolean = unansweredQuestions().isEmpty()
}

@Serializable
data class DesignIterationRequest(
    val prompt: String,
    val apiKey: String? = null,
    val answers: Map<String, String> = emptyMap(),
    val additionalContext: String? = null,
    val previousBrief: DesignBrief? = null,
    val iteration: Int = 0
)

@Serializable
data class DesignBriefResponse(
    val brief: DesignBrief,
    val tutorMessage: String,
    val iteration: Int,
    val readyToGenerate: Boolean
)
