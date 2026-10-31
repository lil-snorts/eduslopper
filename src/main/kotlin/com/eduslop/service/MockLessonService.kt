package com.eduslop.service

import com.eduslop.models.*
import java.util.UUID

object MockLessonService {

    fun createDddLesson(): Lesson {
        val animation = PixelAnimation(
            totalDurationSeconds = 60,
            scenes = listOf(
                AnimationScene(
                    id = 1,
                    title = "The Communication Gap & Ubiquitous Language",
                    durationSeconds = 15,
                    narration = "In complex systems, developers and domain experts speak completely different languages. Domain Driven Design bridges this gap by creating a single Ubiquitous Language shared equally by business minds and written code.",
                    background = "blueprint_blue",
                    entities = listOf(
                        PixelEntity(id = "dev", name = "Developer", sprite = "character", x = 18.0, y = 52.0, color = "#00f0ff", label = "Engineer"),
                        PixelEntity(id = "expert", name = "Domain Expert", sprite = "user", x = 78.0, y = 52.0, color = "#ffaa00", label = "Domain Expert"),
                        PixelEntity(id = "dictionary", name = "Ubiquitous Language", sprite = "model", x = 48.0, y = 28.0, color = "#55ff55", label = "Ubiquitous Language")
                    ),
                    actions = listOf(
                        SceneAction(type = "pulse", target = "dictionary", duration = 3.0, label = "Shared Vocabulary"),
                        SceneAction(type = "connect", target = "dev", toX = 48.0, toY = 28.0, duration = 4.0),
                        SceneAction(type = "connect", target = "expert", toX = 48.0, toY = 28.0, duration = 4.0)
                    )
                ),
                AnimationScene(
                    id = 2,
                    title = "Strategic Design: Bounded Contexts",
                    durationSeconds = 15,
                    narration = "Never attempt one giant model for the whole enterprise. DDD carves the domain into Bounded Contexts. An 'Account' in billing means a ledger; in shipping, it means a physical address. Clear boundaries stop spaghetti code.",
                    background = "retro_dark",
                    entities = listOf(
                        PixelEntity(id = "sales_ctx", name = "Sales Context", sprite = "bounded_context", x = 22.0, y = 35.0, color = "#ff007f", label = "Sales: Lead & Quote"),
                        PixelEntity(id = "shipping_ctx", name = "Shipping Context", sprite = "bounded_context", x = 74.0, y = 35.0, color = "#00d4ff", label = "Shipping: Delivery & Route"),
                        PixelEntity(id = "event_bus", name = "Domain Events", sprite = "event", x = 48.0, y = 72.0, color = "#ffea00", label = "OrderPlaced Event")
                    ),
                    actions = listOf(
                        SceneAction(type = "pulse", target = "sales_ctx", duration = 2.0),
                        SceneAction(type = "pulse", target = "shipping_ctx", duration = 2.0),
                        SceneAction(type = "connect", target = "sales_ctx", toX = 48.0, toY = 72.0, duration = 3.0),
                        SceneAction(type = "connect", target = "event_bus", toX = 74.0, toY = 35.0, duration = 3.0)
                    )
                ),
                AnimationScene(
                    id = 3,
                    title = "Tactical Building Blocks: Entities vs Value Objects",
                    durationSeconds = 15,
                    narration = "Inside a boundary, we model with two vital tools. Entities have a unique identity and lifecycle that persists over time. Value Objects are immutable descriptors defined only by their attributes—like an address or price tag.",
                    background = "cyber_purple",
                    entities = listOf(
                        PixelEntity(id = "entity_user", name = "User Entity", sprite = "aggregate", x = 28.0, y = 45.0, color = "#00ffcc", label = "User [ID #941]"),
                        PixelEntity(id = "vo_money", name = "Money Value Object", sprite = "box", x = 68.0, y = 45.0, color = "#ffd700", label = "Money [$50 USD]")
                    ),
                    actions = listOf(
                        SceneAction(type = "highlight", target = "entity_user", duration = 4.0, label = "Distinct Identity"),
                        SceneAction(type = "pulse", target = "vo_money", duration = 4.0, label = "Immutable Value")
                    )
                ),
                AnimationScene(
                    id = 4,
                    title = "Aggregate Roots: Defending Invariants",
                    durationSeconds = 15,
                    narration = "To protect business consistency, related objects form an Aggregate. External callers must only talk to the Aggregate Root. It acts as the guardian, enforcing business rules and ensuring state is never corrupted.",
                    background = "terminal_green",
                    entities = listOf(
                        PixelEntity(id = "root", name = "Order (Aggregate Root)", sprite = "aggregate", x = 48.0, y = 36.0, color = "#39ff14", label = "Aggregate Root"),
                        PixelEntity(id = "item1", name = "Line Item A", sprite = "model", x = 32.0, y = 68.0, color = "#00e5ff", label = "Item 1"),
                        PixelEntity(id = "item2", name = "Line Item B", sprite = "model", x = 64.0, y = 68.0, color = "#00e5ff", label = "Item 2"),
                        PixelEntity(id = "db", name = "Persistence", sprite = "database", x = 48.0, y = 88.0, color = "#ff0055", label = "Database")
                    ),
                    actions = listOf(
                        SceneAction(type = "pulse", target = "root", duration = 3.0, label = "Enforces Invariants"),
                        SceneAction(type = "connect", target = "root", toX = 32.0, toY = 68.0),
                        SceneAction(type = "connect", target = "root", toX = 64.0, toY = 68.0),
                        SceneAction(type = "connect", target = "root", toX = 48.0, toY = 88.0)
                    )
                )
            )
        )

        val cueCards = listOf(
            CueCard(
                id = 1,
                title = "Ubiquitous Language",
                frontPrompt = "What is the Ubiquitous Language and why is it essential?",
                backAnswer = "A single, rigorous vocabulary mutually agreed upon and spoken by both developers and business experts—used directly in discussions, diagrams, and actual code class names to prevent translation errors.",
                category = "Core Philosophy"
            ),
            CueCard(
                id = 2,
                title = "Bounded Context",
                frontPrompt = "What is a Bounded Context in Strategic Design?",
                backAnswer = "An explicit boundary inside which a domain model strictly applies. It allows terms (e.g. 'Customer' or 'Order') to have crisp, distinct meanings in different sub-systems without conflict.",
                category = "Strategic Design"
            ),
            CueCard(
                id = 3,
                title = "Entity vs. Value Object",
                frontPrompt = "How do you distinguish an Entity from a Value Object?",
                backAnswer = "An Entity has a distinct identity and lifecycle that continues even if its properties change (e.g., Customer #42). A Value Object is immutable and identified solely by its properties (e.g., $10 USD or Zip Code).",
                category = "Tactical Building Blocks"
            ),
            CueCard(
                id = 4,
                title = "Aggregate Root",
                frontPrompt = "What critical rule governs an Aggregate Root?",
                backAnswer = "The Aggregate Root is the single entry point for all operations on the cluster. Outside objects may only reference the Root, guaranteeing all business invariants and data integrity are preserved.",
                category = "Consistency & Guarding"
            ),
            CueCard(
                id = 5,
                title = "Domain Events",
                frontPrompt = "What is a Domain Event and how does it help architecture?",
                backAnswer = "A record of an immutable fact that occurred in the domain (e.g. 'OrderPlaced'). It allows different Bounded Contexts to communicate asynchronously without tight coupling.",
                category = "Integration"
            )
        )

        val quiz = listOf(
            QuizQuestion(
                id = 1,
                question = "Why do developers and business stakeholders establish a 'Ubiquitous Language' in DDD?",
                options = listOf(
                    "To force business teams to write Kotlin code",
                    "To ensure shared vocabulary between domain experts and software developers without translation loss",
                    "To translate legacy relational databases into GraphQL schemas",
                    "To replace all documentation with automated unit tests"
                ),
                correctOptionIndex = 1,
                explanation = "Ubiquitous Language eliminates the risk of misinterpretation by unifying the terms used in business conversations, diagrams, and codebase class names."
            ),
            QuizQuestion(
                id = 2,
                question = "What is the primary characteristic that distinguishes a Value Object from an Entity?",
                options = listOf(
                    "Value Objects have auto-incrementing database IDs",
                    "Entities are immutable, while Value Objects are mutable",
                    "Value Objects are identified solely by their attributes and are immutable, while Entities have a distinct identity",
                    "Value Objects can only be stored in NoSQL databases"
                ),
                correctOptionIndex = 2,
                explanation = "If two $20 bills have the same value, they are interchangeable (Value Objects). A specific customer with an ID is unique regardless of name changes (Entity)."
            ),
            QuizQuestion(
                id = 3,
                question = "In DDD, what is the core responsibility of an Aggregate Root?",
                options = listOf(
                    "To act as the sole entry point and enforce business invariants for a cluster of associated objects",
                    "To compile TypeScript code into WebAssembly",
                    "To manage HTTP connection pools in the server",
                    "To act as an external third-party API gateway"
                ),
                correctOptionIndex = 0,
                explanation = "External objects are only allowed to hold references to the Aggregate Root, ensuring internal business invariants cannot be bypassed or corrupted."
            ),
            QuizQuestion(
                id = 4,
                question = "How does a Bounded Context prevent models from becoming bloated spaghetti?",
                options = listOf(
                    "By deleting unused database tables every weekend",
                    "By defining explicit boundaries where a specific model applies, allowing words to have focused meanings",
                    "By forcing all microservices to use the exact same database schema",
                    "By preventing developers from creating more than 10 classes per module"
                ),
                correctOptionIndex = 1,
                explanation = "A Bounded Context isolates models so that 'Customer' in Sales doesn't need to contain Shipping or Support fields, preventing a single giant monolithic schema."
            )
        )

        return Lesson(
            id = "ddd-intro",
            topic = "Domain Driven Design (DDD)",
            summary = "A comprehensive micro-lesson introducing Domain Driven Design, from Ubiquitous Language and Bounded Contexts to Entities, Value Objects, and Aggregate Roots.",
            animation = animation,
            cueCards = cueCards,
            quiz = quiz
        )
    }

    fun createFallbackLessonForPrompt(prompt: String): Lesson {
        val cleanTopic = prompt.trim().removePrefix("how does ").removePrefix("what is ").removeSuffix(" work").removeSuffix("?").capitalizeWords()
        val ddd = createDddLesson()
        if (cleanTopic.contains("Domain", ignoreCase = true) || cleanTopic.contains("DDD", ignoreCase = true)) {
            return ddd
        }

        // Generic fallback for any other prompt if Gemini is offline
        val animation = PixelAnimation(
            totalDurationSeconds = 60,
            scenes = listOf(
                AnimationScene(
                    id = 1,
                    title = "Understanding $cleanTopic: The Foundation",
                    durationSeconds = 15,
                    narration = "Welcome to this interactive micro-lesson on $cleanTopic. Every great concept begins with a foundational principle: decomposing a complex real-world problem into manageable, observable parts.",
                    background = "blueprint_blue",
                    entities = listOf(
                        PixelEntity(id = "actor1", name = "Concept Core", sprite = "model", x = 48.0, y = 35.0, color = "#00f0ff", label = cleanTopic),
                        PixelEntity(id = "actor2", name = "Observer", sprite = "character", x = 20.0, y = 60.0, color = "#ffaa00", label = "Learner")
                    ),
                    actions = listOf(
                        SceneAction(type = "pulse", target = "actor1", duration = 3.0),
                        SceneAction(type = "connect", target = "actor2", toX = 48.0, toY = 35.0)
                    )
                ),
                AnimationScene(
                    id = 2,
                    title = "Core Mechanics & Flow",
                    durationSeconds = 15,
                    narration = "As $cleanTopic operates, inputs transition through predictable stages. State is evaluated, decisions are made, and side effects are produced cleanly to maintain system harmony.",
                    background = "cyber_purple",
                    entities = listOf(
                        PixelEntity(id = "input", name = "Input Signal", sprite = "event", x = 18.0, y = 45.0, color = "#ffd700", label = "Input"),
                        PixelEntity(id = "engine", name = "Processing Engine", sprite = "service", x = 48.0, y = 45.0, color = "#00ffcc", label = "Engine"),
                        PixelEntity(id = "output", name = "Output State", sprite = "database", x = 78.0, y = 45.0, color = "#ff007f", label = "Result")
                    ),
                    actions = listOf(
                        SceneAction(type = "connect", target = "input", toX = 48.0, toY = 45.0),
                        SceneAction(type = "pulse", target = "engine", duration = 2.0),
                        SceneAction(type = "connect", target = "engine", toX = 78.0, toY = 45.0)
                    )
                ),
                AnimationScene(
                    id = 3,
                    title = "Key Rules & Boundaries",
                    durationSeconds = 15,
                    narration = "Mastery comes from understanding constraints. In $cleanTopic, boundaries protect integrity. When components respect their designated scopes, failures remain isolated and predictable.",
                    background = "terminal_green",
                    entities = listOf(
                        PixelEntity(id = "boundary", name = "Boundary Wall", sprite = "bounded_context", x = 48.0, y = 45.0, color = "#39ff14", label = "Safe Boundary"),
                        PixelEntity(id = "internal", name = "Protected State", sprite = "aggregate", x = 48.0, y = 45.0, color = "#00f0ff", label = "Core State")
                    ),
                    actions = listOf(
                        SceneAction(type = "highlight", target = "boundary", duration = 3.0),
                        SceneAction(type = "pulse", target = "internal", duration = 3.0)
                    )
                ),
                AnimationScene(
                    id = 4,
                    title = "Practical Takeaway",
                    durationSeconds = 15,
                    narration = "Applying $cleanTopic in real scenarios enables scalable design, clearer mental models, and robust outcomes. Now, scroll down to test your knowledge with the cue cards and quiz!",
                    background = "retro_dark",
                    entities = listOf(
                        PixelEntity(id = "star", name = "Mastery", sprite = "character", x = 48.0, y = 40.0, color = "#ffd700", label = "Mastery Achieved")
                    ),
                    actions = listOf(
                        SceneAction(type = "pulse", target = "star", duration = 4.0)
                    )
                )
            )
        )

        val cueCards = listOf(
            CueCard(1, "Primary Purpose", "What is the primary objective of $cleanTopic?", "To simplify complex problem spaces by creating focused abstractions and well-defined boundaries.", "Fundamentals"),
            CueCard(2, "Key Invariant", "What must always be maintained when using $cleanTopic?", "Consistency and predictable state transitions within its designated boundary.", "Rules"),
            CueCard(3, "Best Practice", "How should one start applying $cleanTopic?", "Start with a clear, shared vocabulary and break down the problem into smaller, isolated components.", "Application")
        )

        val quiz = listOf(
            QuizQuestion(
                1,
                "What is the main goal of $cleanTopic?",
                listOf(
                    "To break down complex systems into well-scoped, manageable components",
                    "To eliminate all written documentation",
                    "To replace compiled languages with dynamic scripting",
                    "To increase memory consumption unnecessarily"
                ),
                0,
                "$cleanTopic focuses on simplifying complexity through modular, well-bounded structures."
            ),
            QuizQuestion(
                2,
                "Why are boundaries crucial when designing with $cleanTopic?",
                listOf(
                    "They prevent unauthorized system shutdowns",
                    "They isolate changes and prevent cascading failures across the system",
                    "They force all developers to use the exact same IDE",
                    "They make the codebase impossible to refactor"
                ),
                1,
                "Explicit boundaries ensure components remain decoupled, making systems resilient and easier to reason about."
            )
        )

        return Lesson(
            id = UUID.randomUUID().toString().substring(0, 8),
            topic = cleanTopic,
            summary = "A concise 2D interactive pixel micro-lesson on $cleanTopic.",
            animation = animation,
            cueCards = cueCards,
            quiz = quiz
        )
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
}
