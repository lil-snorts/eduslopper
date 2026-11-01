package com.eduslop.ai

import com.eduslop.models.DesignBriefResponse
import com.eduslop.models.DesignIterationRequest

/**
 * The public port for tutor-led design iteration. The Gemini/ADK
 * implementation and its deterministic fallback are internal to the ai package.
 */
interface DesignTutorService {
    suspend fun iterateDesign(request: DesignIterationRequest): DesignBriefResponse
}

/**
 * Wiring point for the design tutor. The implementation reads USE_MOCK once at
 * construction time to decide between the live tutor and the deterministic
 * fallback; request flows never inspect the environment.
 */
object DesignTutorServiceFactory {
    fun create(): DesignTutorService = GoogleAdkDesignTutor()
}
