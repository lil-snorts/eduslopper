package com.eduslop.repository

import com.eduslop.models.Lesson
import java.util.concurrent.ConcurrentHashMap

class LessonRepository {
    private val lessons = ConcurrentHashMap<String, Lesson>()

    fun save(lesson: Lesson): Lesson {
        lessons[lesson.id] = lesson
        return lesson
    }

    fun findById(id: String): Lesson? = lessons[id]

    fun findAll(): List<Lesson> = lessons.values.toList().sortedByDescending { it.createdAt }

    fun delete(id: String): Boolean = lessons.remove(id) != null
}
