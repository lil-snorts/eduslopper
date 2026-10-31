package com.eduslop.models

import kotlinx.serialization.Serializable

@Serializable
data class SceneAction(
    val type: String, // "move", "pulse", "spawn", "connect", "highlight"
    val target: String,
    val toX: Double? = null,
    val toY: Double? = null,
    val duration: Double? = null,
    val label: String? = null
)

@Serializable
data class PixelEntity(
    val id: String,
    val name: String,
    val sprite: String, // "character", "database", "aggregate", "service", "bounded_context", "event", "box", "user", "cloud", "model"
    val x: Double,      // 0 - 100 (% of canvas width)
    val y: Double,      // 0 - 100 (% of canvas height)
    val color: String? = null,
    val label: String? = null
)

@Serializable
data class AnimationScene(
    val id: Int,
    val title: String,
    val durationSeconds: Int,
    val narration: String,
    val background: String = "retro_dark", // "retro_dark", "blueprint_blue", "terminal_green", "cyber_purple"
    val entities: List<PixelEntity> = emptyList(),
    val actions: List<SceneAction> = emptyList()
)

@Serializable
data class PixelAnimation(
    val totalDurationSeconds: Int,
    val scenes: List<AnimationScene>
)

@Serializable
data class CueCard(
    val id: Int,
    val title: String,
    val frontPrompt: String,
    val backAnswer: String,
    val category: String = "Core Concept"
)

@Serializable
data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

@Serializable
data class Lesson(
    val id: String,
    val topic: String,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis(),
    val animation: PixelAnimation,
    val cueCards: List<CueCard>,
    val quiz: List<QuizQuestion>
)

@Serializable
data class GenerateLessonRequest(
    val prompt: String,
    val apiKey: String? = null,
    val designBrief: DesignBrief? = null
)

@Serializable
data class ServerConfig(
    val mode: String,
    val model: String,
    val geminiKeyConfigured: Boolean,
    val mockRequiresUseMock: Boolean = true
)

@Serializable
data class ApiError(
    val error: String,
    val code: String = "error"
)

@Serializable
data class QuizSubmissionRequest(
    val studentName: String = "Anonymous Student",
    val answers: Map<Int, Int> // questionId to selectedOptionIndex
)

@Serializable
data class QuestionEvaluation(
    val questionId: Int,
    val question: String,
    val selectedOptionIndex: Int,
    val correctOptionIndex: Int,
    val isCorrect: Boolean,
    val explanation: String
)

@Serializable
data class QuizSubmissionResult(
    val submissionId: String,
    val lessonId: String,
    val studentName: String,
    val score: Int,
    val total: Int,
    val percentage: Int,
    val submittedAt: Long = System.currentTimeMillis(),
    val questionResults: List<QuestionEvaluation>
)
