package com.extollit.gaming.ai.path.iterable

import java.text.MessageFormat

class ArrayIterable<out T>(val delegate: Array<out T>) : Iterable<T> {
    override operator fun iterator(): Iterator<T> = Iter(delegate)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ArrayIterable<*>

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return delegate.contentEquals(that.delegate)
    }

    override fun hashCode(): Int = delegate.contentHashCode()

    class Iter<T> @JvmOverloads constructor(val array: Array<out T>, val length: Int = array.size) : MutableIterator<T> {

        private var c = 0

        override fun hasNext(): Boolean = c < length

        override fun next(): T = map(c, array[c++])

        override fun remove() {
            throw UnsupportedOperationException()
        }

        fun map(index: Int, input: T): T = input

        init {
            if (length > array.size || length < 0) throw ArrayIndexOutOfBoundsException(
                MessageFormat.format(
                    "Iteration limit is not within bounds! 0 <= len < {0} but len = {1}",
                    array.size,
                    length
                )
            )
        }
    }
}