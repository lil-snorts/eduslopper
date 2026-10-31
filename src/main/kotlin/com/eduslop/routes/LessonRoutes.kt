package com.eduslop.routes

import com.eduslop.models.GenerateLessonRequest
import com.eduslop.repository.LessonRepository
import com.eduslop.service.GoogleAdkService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.lessonRoutes(
    lessonRepository: LessonRepository,
    googleAdkService: GoogleAdkService
) {
    route("/api/lessons") {
        get {
            call.respond(lessonRepository.findAll())
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
            val lesson = lessonRepository.findById(id)
            if (lesson != null) {
                call.respond(lesson)
            } else {
                call.respond(HttpStatusCode.NotFound, "Lesson not found")
            }
        }

        post("generate") {
            val request = call.receive<GenerateLessonRequest>()
            if (request.prompt.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, "Prompt cannot be empty")
            }
            val lesson = googleAdkService.generateLesson(request.prompt, request.apiKey, request.designBrief)
            lessonRepository.save(lesson)
            call.respond(HttpStatusCode.Created, lesson)
        }
    }
}

