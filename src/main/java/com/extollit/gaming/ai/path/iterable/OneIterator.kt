package com.extollit.gaming.ai.path.iterable

/**
 * Represents an [Iterator] with only one iteration. Useful for special object cases.
 */
class OneIterator<out T>(private var element: T?) : MutableIterator<T> {
    override fun hasNext(): Boolean = element != null

    override fun next(): T {
        val current: T = element ?: throw NoSuchElementException()
        element = null
        return current
    }

    override fun remove() {
        if (element == null) throw NoSuchElementException()
        element = null
    }
}