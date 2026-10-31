package com.eduslop

import com.eduslop.models.QuizSubmissionRequest
import com.eduslop.models.QuizSubmissionResult
import com.eduslop.repository.LessonRepository
import com.eduslop.repository.QuizRepository
import com.eduslop.service.MockLessonService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EduslopTest {

    @Test
    fun testMockLessonStructure() {
        val dddLesson = MockLessonService.createDddLesson()
        assertEquals("ddd-intro", dddLesson.id)
        assertTrue(dddLesson.topic.contains("Domain Driven Design"))
        assertEquals(60, dddLesson.animation.totalDurationSeconds)
        assertTrue(dddLesson.animation.scenes.size >= 4)
        assertTrue(dddLesson.cueCards.size >= 4)
        assertTrue(dddLesson.quiz.size >= 4)

        // Validate each quiz question has a valid correct option index
        dddLesson.quiz.forEach { q ->
            assertTrue(q.correctOptionIndex in 0 until q.options.size)
            assertTrue(q.explanation.isNotBlank())
        }
    }

    @Test
    fun testFallbackGenerationForArbitraryPrompt() {
        val topic = "Quantum Computing"
        val lesson = MockLessonService.createFallbackLessonForPrompt(topic)
        assertTrue(lesson.topic.contains("Quantum Computing"))
        assertEquals(60, lesson.animation.totalDurationSeconds)
        assertTrue(lesson.cueCards.isNotEmpty())
        assertTrue(lesson.quiz.isNotEmpty())
    }

    @Test
    fun testLessonRepository() {
        val repo = LessonRepository()
        val lesson = MockLessonService.createDddLesson()
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

    @Test
    fun testGoogleAdkRootAgentInitialization() {
        val adkService = com.eduslop.service.GoogleAdkService()
        assertNotNull(adkService.rootAgent)
        assertEquals("eduslop_retro_educator_agent", adkService.rootAgent.name())

        // Test generation with fallback
        val lesson = adkService.generateLesson("How does Domain Driven Design work")
        assertNotNull(lesson)
        assertEquals(60, lesson.animation.totalDurationSeconds)
    }
}
