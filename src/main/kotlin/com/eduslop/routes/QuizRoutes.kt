package com.eduslop.routes

import com.eduslop.models.QuestionEvaluation
import com.eduslop.models.QuizSubmissionRequest
import com.eduslop.models.QuizSubmissionResult
import com.eduslop.repository.LessonRepository
import com.eduslop.repository.QuizRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.quizRoutes(
    lessonRepository: LessonRepository,
    quizRepository: QuizRepository
) {
    route("/api/lessons/{id}/quiz") {
        post("submit") {
            val lessonId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing lesson id")
            val lesson = lessonRepository.findById(lessonId) ?: return@post call.respond(HttpStatusCode.NotFound, "Lesson not found")

            val submission = call.receive<QuizSubmissionRequest>()
            var correctCount = 0

            val evaluations = lesson.quiz.map { q ->
                val selected = submission.answers[q.id] ?: -1
                val isCorrect = selected == q.correctOptionIndex
                if (isCorrect) correctCount++

                QuestionEvaluation(
                    questionId = q.id,
                    question = q.question,
                    selectedOptionIndex = selected,
                    correctOptionIndex = q.correctOptionIndex,
                    isCorrect = isCorrect,
                    explanation = q.explanation
                )
            }

            val total = lesson.quiz.size
            val percentage = if (total > 0) ((correctCount.toDouble() / total) * 100).toInt() else 0

            val result = QuizSubmissionResult(
                submissionId = UUID.randomUUID().toString().substring(0, 8),
                lessonId = lessonId,
                studentName = submission.studentName.ifBlank { "Anonymous Student" },
                score = correctCount,
                total = total,
                percentage = percentage,
                questionResults = evaluations
            )

            quizRepository.saveSubmission(result)
            call.respond(HttpStatusCode.OK, result)
        }

        get("results") {
            val lessonId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing lesson id")
            val results = quizRepository.getSubmissionsForLesson(lessonId)
            call.respond(results)
        }
    }
}
