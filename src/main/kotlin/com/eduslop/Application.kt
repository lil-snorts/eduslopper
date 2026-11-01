package com.eduslop

import com.eduslop.ai.AiGenerationException
import com.eduslop.ai.DesignTutorService
import com.eduslop.ai.DesignTutorServiceFactory
import com.eduslop.ai.LessonService
import com.eduslop.ai.LessonServiceFactory
import com.eduslop.ai.MissingApiKeyException
import com.eduslop.models.ApiError
import com.eduslop.models.ServerConfig
import com.eduslop.repository.LessonRepository
import com.eduslop.repository.QuizRepository
import com.eduslop.routes.lessonRoutes
import com.eduslop.routes.quizRoutes
import com.eduslop.routes.tutorRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    // `.env` and environment configuration is owned by the ai package; it loads
    // lazily the first time a service is wired in module().
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

    install(StatusPages) {
        exception<MissingApiKeyException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(cause.message ?: "No Gemini API key configured.", "missing_api_key")
            )
        }
        exception<AiGenerationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadGateway,
                ApiError(cause.message ?: "Gemini generation failed.", "generation_failed")
            )
        }
    }

    val lessonRepository = LessonRepository()
    val quizRepository = QuizRepository()

    // Pick implementations once, at wiring time, from USE_MOCK; the request
    // flow only ever sees the public ports.
    val lessonService: LessonService = LessonServiceFactory.create()
    val designTutorService: DesignTutorService = DesignTutorServiceFactory.create()
    logger.info("Lesson generation wired in '{}' mode", lessonService.mode)

    // Pre-populate with our showcase Domain Driven Design lesson (deterministic
    // content, seeded regardless of the active generation mode)
    val initialDddLesson = LessonServiceFactory.showcaseLesson()
    lessonRepository.save(initialDddLesson)
    logger.info("Pre-seeded showcase lesson: {} (id: {})", initialDddLesson.topic, initialDddLesson.id)

    routing {
        // API routes
        lessonRoutes(lessonRepository, lessonService)
        tutorRoutes(designTutorService)
        quizRoutes(lessonRepository, quizRepository)

        get("/api/config") {
            call.respond(
                ServerConfig(
                    mode = lessonService.mode,
                    model = lessonService.model,
                    geminiKeyConfigured = lessonService.serverKeyConfigured
                )
            )
        }

        get("/api/health") {
            call.respond(
                mapOf(
                    "status" to "UP",
                    "message" to "EDUSLOP retro education server active",
                    "mode" to lessonService.mode,
                    "model" to lessonService.model
                )
            )
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
