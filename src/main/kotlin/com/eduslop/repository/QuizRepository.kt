package com.eduslop.repository

import com.eduslop.models.QuizSubmissionResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class QuizRepository {
    // lessonId -> List of submissions
    private val submissionsByLesson = ConcurrentHashMap<String, CopyOnWriteArrayList<QuizSubmissionResult>>()

    fun saveSubmission(result: QuizSubmissionResult): QuizSubmissionResult {
        submissionsByLesson.computeIfAbsent(result.lessonId) { CopyOnWriteArrayList() }
            .add(result)
        return result
    }

    fun getSubmissionsForLesson(lessonId: String): List<QuizSubmissionResult> {
        return submissionsByLesson[lessonId]?.toList() ?: emptyList()
    }

    fun getAllSubmissions(): Map<String, List<QuizSubmissionResult>> {
        return submissionsByLesson.mapValues { it.value.toList() }
    }
}
