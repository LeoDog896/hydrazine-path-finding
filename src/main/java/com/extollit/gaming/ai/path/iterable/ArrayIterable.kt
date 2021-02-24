package com.extollit.gaming.ai.path.iterable

import java.text.MessageFormat

class ArrayIterable<out T>(elements: Array<out T>) : AbstractArrayIterable<T, T>(elements) {
    override operator fun iterator(): Iterator<T> = Iter(delegate)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ArrayIterable<*>

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return delegate.contentEquals(that.delegate)
    }

    override fun hashCode(): Int = delegate.contentHashCode()

    class Iter<T> : AbstractIter<T, T> {
        constructor(array: Array<out T>) : super(array)
        constructor(array: Array<out T>, len: Int) : super(array, len)

        override fun map(index: Int, input: T): T = input
    }
}

abstract class AbstractArrayIterable<out A, out B>(val delegate: Array<out B>) : Iterable<A> {
    abstract class AbstractIter<A, B> @JvmOverloads constructor(val array: Array<out B>, val length: Int = array.size) : MutableIterator<A> {
        private var c = 0
        override fun hasNext(): Boolean = c < length

        override fun next(): A = map(c, array[c++])

        protected abstract fun map(index: Int, input: B): A
        override fun remove() {
            throw UnsupportedOperationException()
        }

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
