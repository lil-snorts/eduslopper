package com.eduslop.routes

import com.eduslop.models.DesignIterationRequest
import com.eduslop.service.LessonTutorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tutorRoutes(lessonTutorService: LessonTutorService) {
    route("/api/tutor") {
        /**
         * Iterates the initial lesson design with the teacher. The first call
         * returns clarifying questions when the prompt lacks context; later
         * calls fold the teacher's answers into the shared design brief.
         */
        post("design") {
            val request = call.receive<DesignIterationRequest>()
            if (request.prompt.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, "Prompt cannot be empty")
            }
            call.respond(lessonTutorService.iterateDesign(request))
        }
    }
}
