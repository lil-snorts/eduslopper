package com.eduslop.util

import org.slf4j.LoggerFactory
import java.io.File

object EnvLoader {
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

                    // Populate System properties so libraries like Google ADK can detect them
                    System.setProperty(key, value)
                    if (key == "GEMINI_API_KEY" && System.getProperty("GOOGLE_API_KEY") == null) {
                        System.setProperty("GOOGLE_API_KEY", value)
                    }
                }
            }
            logger.info("Successfully loaded environment variables from .env file (keys: {})", envMap.keys)
        } catch (e: Exception) {
            logger.warn("Failed to parse .env file: {}", e.message)
        }
    }

    fun get(key: String): String? {
        return envMap[key] ?: System.getenv(key) ?: System.getProperty(key)
    }
}
