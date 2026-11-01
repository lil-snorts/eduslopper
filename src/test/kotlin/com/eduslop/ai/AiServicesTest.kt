package com.eduslop.ai

import com.eduslop.models.ClarifyingQuestion
import com.eduslop.models.DesignBeat
import com.eduslop.models.DesignBrief
import com.eduslop.models.DesignIterationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AiServicesTest {

    @Test
    fun testMockLessonStructure() {
        val dddLesson = MockLessonService().createDddLesson()
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
        val lesson = MockLessonService().createFallbackLessonForPrompt(topic)
        assertTrue(lesson.topic.contains("Quantum Computing"))
        assertEquals(60, lesson.animation.totalDurationSeconds)
        assertTrue(lesson.cueCards.isNotEmpty())
        assertTrue(lesson.quiz.isNotEmpty())
    }

    @Test
    fun testGoogleAdkRootAgentInitialization() {
        val adkService = GoogleAdkService()
        assertNotNull(adkService.rootAgent)
        assertEquals("eduslop_retro_educator_agent", adkService.rootAgent.name())
    }

    @Test
    fun testMockModeIsExplicitAndReturnsALessonWithoutAnApiKey() {
        val previous = System.getProperty("USE_MOCK")
        System.setProperty("USE_MOCK", "true")
        try {
            assertTrue(AiConfig.useMock())

            val service = LessonServiceFactory.create()
            assertEquals("mock", service.mode)
            val lesson = service.generateLesson("How does Domain Driven Design work")
            assertNotNull(lesson)
            assertEquals(60, lesson.animation.totalDurationSeconds)
        } finally {
            if (previous == null) System.clearProperty("USE_MOCK") else System.setProperty("USE_MOCK", previous)
        }
    }

    @Test
    fun testWiringSelectsLiveServiceWhenMockIsDisabled() {
        val previous = System.getProperty("USE_MOCK")
        System.setProperty("USE_MOCK", "false")
        try {
            val service = LessonServiceFactory.create()
            assertEquals("live", service.mode)
            assertTrue(service is GoogleAdkService)
        } finally {
            if (previous == null) System.clearProperty("USE_MOCK") else System.setProperty("USE_MOCK", previous)
        }
    }

    @Test
    fun testMockModeRequiresAnExplicitEnabledValue() {
        val previous = System.getProperty("USE_MOCK")
        try {
            System.setProperty("USE_MOCK", "false")
            assertFalse(AiConfig.useMock())

            System.setProperty("USE_MOCK", "1")
            assertTrue(AiConfig.useMock())
        } finally {
            if (previous == null) System.clearProperty("USE_MOCK") else System.setProperty("USE_MOCK", previous)
        }
    }

    @Test
    fun testDesignTutorAgentInitialization() {
        val tutorService = GoogleAdkDesignTutor()
        assertNotNull(tutorService.designTutorAgent)
        assertEquals("eduslop_design_tutor_agent", tutorService.designTutorAgent.name())
    }

    @Test
    fun testTutorAsksClarifyingQuestionsForVaguePrompt() {
        val response = FallbackDesignTutor.iterate(
            DesignIterationRequest(prompt = "How does Domain Driven Design work?")
        )

        assertEquals(1, response.iteration)
        assertFalse(response.readyToGenerate)
        assertTrue(response.brief.clarifyingQuestions.isNotEmpty())
        assertTrue(response.brief.clarifyingQuestions.size <= 3)
        assertTrue(response.brief.clarifyingQuestions.any { it.id == "audience" })
        assertTrue(response.brief.clarifyingQuestions.all { it.suggestions.isNotEmpty() })
        assertTrue(response.brief.proposedOutline.isNotEmpty())
        assertTrue(response.brief.revisedPrompt.contains("Domain Driven Design"))
    }

    @Test
    fun testTutorIteratesUntilDesignIsAligned() {
        val prompt = "How does Domain Driven Design work?"
        val first = FallbackDesignTutor.iterate(DesignIterationRequest(prompt = prompt))
        val firstAnswers = first.brief.clarifyingQuestions.associate {
            it.id to (it.suggestions.firstOrNull() ?: "Use a sensible default")
        }

        val second = FallbackDesignTutor.iterate(
            DesignIterationRequest(
                prompt = prompt,
                answers = firstAnswers,
                previousBrief = first.brief,
                iteration = first.iteration
            )
        )
        assertFalse(second.readyToGenerate)

        val secondAnswers = second.brief.clarifyingQuestions.associate {
            it.id to (it.suggestions.firstOrNull() ?: "Use a sensible default")
        }
        val third = FallbackDesignTutor.iterate(
            DesignIterationRequest(
                prompt = prompt,
                answers = secondAnswers,
                previousBrief = second.brief,
                iteration = second.iteration
            )
        )

        assertTrue(third.readyToGenerate)
        assertTrue(third.brief.clarifyingQuestions.isEmpty())
        assertTrue(third.brief.answers.keys.containsAll(firstAnswers.keys))
        assertTrue(third.brief.answers.keys.containsAll(secondAnswers.keys))
    }

    @Test
    fun testDetailedPromptNeedsNoClarifyingQuestions() {
        val prompt = "Create a 10-minute lesson for middle school students with no prior programming knowledge " +
            "so they can explain what a variable is and use one in a simple program."

        val response = FallbackDesignTutor.iterate(DesignIterationRequest(prompt = prompt))

        assertTrue(response.readyToGenerate)
        assertTrue(response.brief.clarifyingQuestions.isEmpty())
        assertTrue(response.brief.audience.contains("Middle school"))
    }

    @Test
    fun testDesignBriefIsHonouredByFallbackGeneration() {
        val brief = DesignBrief(
            topic = "Photosynthesis",
            summary = "How plants turn light into food.",
            audience = "Year 7 learners",
            learningObjectives = listOf("Explain how plants convert light into chemical energy"),
            keyConcepts = listOf("Chlorophyll"),
            tone = "Playful retro arcade"
        )

        val lesson = MockLessonService().createFallbackLessonForPrompt("Photosynthesis", brief)

        assertEquals("Photosynthesis", lesson.topic)
        assertTrue(lesson.summary.contains("Year 7 learners"))
        assertTrue(lesson.summary.contains("Explain how plants convert light into chemical energy"))
        assertTrue(lesson.cueCards.any { it.backAnswer == "Explain how plants convert light into chemical energy" })
        assertTrue(lesson.cueCards.any { it.backAnswer == "Chlorophyll" })
    }

    @Test
    fun testShowcaseLessonIsAdaptedToDesignBrief() {
        val brief = DesignBrief(
            topic = "Domain Driven Design",
            audience = "Year 8 learners",
            learningObjectives = listOf("Explain what a bounded context is"),
            keyConcepts = listOf("Ubiquitous language")
        )

        val lesson = MockLessonService().createFallbackLessonForPrompt("DDD", brief)

        assertEquals("Domain Driven Design", lesson.topic)
        assertTrue(lesson.summary.contains("Year 8 learners"))
        assertTrue(lesson.cueCards.any { it.backAnswer == "Explain what a bounded context is" })
        assertTrue(lesson.cueCards.any { it.backAnswer == "Ubiquitous language" })
        // The rich DDD showcase content is preserved.
        assertTrue(lesson.cueCards.size >= 6)
    }

    @Test
    fun testDesignBriefFormatterIncludesTeacherAnswers() {
        val brief = DesignBrief(
            topic = "Hash collisions",
            audience = "High school learners",
            proposedOutline = listOf(DesignBeat("Hook", "Start with a collision example.")),
            clarifyingQuestions = listOf(
                ClarifyingQuestion(
                    id = "audience",
                    question = "Who is this lesson for?",
                    suggestions = listOf("High school learners")
                )
            ),
            answers = mapOf("audience" to "High school learners"),
            additionalContext = "They already know what a hash function is."
        )

        val context = DesignBriefFormatter.generationContext(brief)

        assertTrue(context.contains("AGREED LESSON DESIGN BRIEF"))
        assertTrue(context.contains("Proposed lesson beats:"))
        assertTrue(context.contains("1. Hook - Start with a collision example."))
        assertTrue(context.contains("A: High school learners"))
        assertTrue(context.contains("They already know what a hash function is."))
    }
}
