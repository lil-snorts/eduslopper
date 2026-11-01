package com.eduslop

import com.eduslop.ai.LessonServiceFactory
import com.eduslop.models.QuizSubmissionResult
import com.eduslop.repository.LessonRepository
import com.eduslop.repository.QuizRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RepositoryTest {

    @Test
    fun testLessonRepository() {
        val repo = LessonRepository()
        val lesson = LessonServiceFactory.showcaseLesson()
        repo.save(lesson)

        val retrieved = repo.findById("ddd-intro")
        assertNotNull(retrieved)
        assertEquals(lesson.topic, retrieved.topic)
        assertEquals(1, repo.findAll().size)
    }

    @Test
    fun testQuizRepository() {
        val repo = QuizRepository()
        val submission = QuizSubmissionResult(
            submissionId = "sub-1",
            lessonId = "ddd-intro",
            studentName = "Alice",
            score = 4,
            total = 4,
            percentage = 100,
            questionResults = emptyList()
        )
        repo.saveSubmission(submission)

        val results = repo.getSubmissionsForLesson("ddd-intro")
        assertEquals(1, results.size)
        assertEquals("Alice", results.first().studentName)
        assertEquals(100, results.first().percentage)
    }
}
