package com.eduslop

import com.eduslop.repository.LessonRepository
import com.eduslop.repository.QuizRepository
import com.eduslop.routes.lessonRoutes
import com.eduslop.routes.quizRoutes
import com.eduslop.service.GoogleAdkService
import com.eduslop.service.MockLessonService
import com.eduslop.util.EnvLoader
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    EnvLoader.loadDotEnv()
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("com.eduslop.Application")
    logger.info("Initializing EDUSLOP application module...")

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    val lessonRepository = LessonRepository()
    val quizRepository = QuizRepository()
    val googleAdkService = GoogleAdkService()

    // Pre-populate with our showcase Domain Driven Design lesson
    val initialDddLesson = MockLessonService.createDddLesson()
    lessonRepository.save(initialDddLesson)
    logger.info("Pre-seeded showcase lesson: {} (id: {})", initialDddLesson.topic, initialDddLesson.id)

    routing {
        // API routes
        lessonRoutes(lessonRepository, googleAdkService)
        quizRoutes(lessonRepository, quizRepository)

        get("/api/health") {
            call.respond(mapOf("status" to "UP", "message" to "EDUSLOP retro education server active"))
        }

        // HTML pages and static assets
        staticResources("/static", "static")

        get("/") {
            val stream = this::class.java.classLoader.getResourceAsStream("static/index.html")
            if (stream != null) {
                call.respondText(stream.bufferedReader().use { it.readText() }, ContentType.Text.Html)
            } else {
                call.respond(HttpStatusCode.NotFound, "index.html missing")
            }
        }

        get("/lesson/{id}") {
            val stream = this::class.java.classLoader.getResourceAsStream("static/lesson.html")
            if (stream != null) {
                call.respondText(stream.bufferedReader().use { it.readText() }, ContentType.Text.Html)
            } else {
                call.respond(HttpStatusCode.NotFound, "lesson.html missing")
            }
        }
    }
}
