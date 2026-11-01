package com.eduslop.ai

import org.slf4j.LoggerFactory
import java.io.File

internal object EnvLoader {
    private val logger = LoggerFactory.getLogger(EnvLoader::class.java)
    private val envMap = mutableMapOf<String, String>()

    init {
        loadDotEnv()
    }

    fun loadDotEnv() {
        val envFile = File(".env")
        if (!envFile.exists()) {
            logger.debug(".env file not found in working directory.")
            return
        }

        try {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    val key = parts[0].trim()
                    var value = parts[1].trim()
                    // Strip enclosing quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length - 1)
                    }
                    envMap[key] = value

                    // Mirror into System properties so libraries like Google ADK can detect
                    // them, but never clobber an explicit -D flag or a real environment variable.
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value)
                    }
                    if (key == "GEMINI_API_KEY" && System.getProperty("GOOGLE_API_KEY") == null && System.getenv("GOOGLE_API_KEY") == null) {
                        System.setProperty("GOOGLE_API_KEY", value)
                    }
                }
            }
            logger.info("Successfully loaded environment variables from .env file (keys: {})", envMap.keys)
        } catch (e: Exception) {
            logger.warn("Failed to parse .env file: {}", e.message)
        }
    }

    /**
     * Resolution order: explicit System properties (set via `-D` or at runtime),
     * then real environment variables, then values loaded from `.env`. This keeps
     * `.env` a fallback, so `USE_MOCK=true ./gradlew run` (and test overrides) win
     * over a stale `.env` entry.
     */
    fun get(key: String): String? {
        return System.getProperty(key) ?: System.getenv(key) ?: envMap[key]
    }
}
