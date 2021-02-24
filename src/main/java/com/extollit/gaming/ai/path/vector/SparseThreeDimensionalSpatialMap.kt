package com.extollit.gaming.ai.path.vector

import java.util.*
import java.util.AbstractMap.SimpleEntry

class SparseThreeDimensionalSpatialMap<T>(private val order: Int) : MutableMap<ThreeDimensionalIntVector?, T?> {
    private class GreaterCoarseKey(x: Int, y: Int, z: Int) {
        val x: Byte = (x and 0xFF).toByte()
        val y: Byte = (y and 0xFF).toByte()
        val z: Byte = (z and 0xFF).toByte()
        private val hashCode: Int
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val greaterCoarseKey = other as GreaterCoarseKey
            return x == greaterCoarseKey.x && y == greaterCoarseKey.y && z == greaterCoarseKey.z
        }

        override fun hashCode(): Int = hashCode

        init {
            var result = 1
            result = 31 * result + z
            result = 31 * result + y
            result = 31 * result + x
            hashCode = result
        }
    }

    private class LesserCoarseKey(val x: Int, val y: Int, val z: Int) {
        private val hashCode: Int
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val lesserCoarseKey = other as LesserCoarseKey
            return x == lesserCoarseKey.x && y == lesserCoarseKey.y && z == lesserCoarseKey.z
        }

        override fun hashCode(): Int = hashCode

        init {
            var result = 1
            result = 31 * result + z
            result = 31 * result + y
            result = 31 * result + x
            hashCode = result
        }
    }

    private val mask: Int = (2 shl order) - 1
    override var size: Int = 0
    private var key0: ThreeDimensionalIntVector? = null
    private var inner0: MutableMap<GreaterCoarseKey, T>? = null
    private val space: MutableMap<LesserCoarseKey, MutableMap<GreaterCoarseKey, T>>

    override fun isEmpty(): Boolean = size == 0

    override fun containsKey(key: ThreeDimensionalIntVector?): Boolean {

        if (key == null) return false

        val inner = acquireInner(key)
        if (inner != null) {
            val greaterKey = greaterKey(key)
            return inner.containsKey(greaterKey)
        }
        return false
    }

    override fun containsValue(value: T?): Boolean {
        if (value == null) throw NullPointerException()
        return space.values.any { it.containsValue(value) }
    }

    override operator fun get(key: ThreeDimensionalIntVector?): T? {

        if (key == null) return null

        val inner = acquireInner(key)
        if (inner != null) {
            val greaterKey = greaterKey(key)
            return inner[greaterKey]
        }
        return null
    }

    override fun put(key: ThreeDimensionalIntVector?, value: T?): T? {
        if (value == null || key == null) throw NullPointerException()
        val lesserKey = lesserKey(key.also { key0 = it })
        inner0 = space[lesserKey]
        var inner = inner0
        if (inner == null) space[lesserKey] = HashMap<GreaterCoarseKey, T>(
            order shl 2,
            INNER_LOAD_FACTOR
        ).also { inner0 = it }.also { inner = it }
        val value0 = inner!!.put(greaterKey(key), value)
        if (value0 == null) size++
        return value0
    }

    override fun remove(key: ThreeDimensionalIntVector?): T? {
        if (key == null) throw NullPointerException()
        val lesserKey = lesserKey(key)
        val inner: MutableMap<GreaterCoarseKey, T>?
        if (key == key0) inner = inner0 else {
            key0 = key
            inner0 = space[lesserKey]
            inner = inner0
        }
        return if (inner != null) try {
            val greaterKey = greaterKey(key)
            val value0 = inner.remove(greaterKey)
            if (value0 != null) size--
            value0
        } finally {
            if (inner.isEmpty()) {
                space.remove(lesserKey)
                key0 = null
                inner0 = null
            }
        } else null
    }

    override fun putAll(from: Map<out ThreeDimensionalIntVector?, T?>) {
        for ((key, value) in from) put(key, value)
    }

    override fun clear() {
        space.clear()
        size = 0
        inner0 = null
        key0 = null
    }

    fun cullOutside(bounds: ThreeDimensionalIIntBox): Iterable<T> {
        val min = lesserKey(bounds.min)
        val max = lesserKey(bounds.max)
        val size0 = size
        val cullees: MutableList<Collection<T>> = LinkedList()
        val i: MutableIterator<Map.Entry<LesserCoarseKey, Map<GreaterCoarseKey, T>>> = space.entries.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            val key = entry.key
            val subMap = entry.value
            if (key.x < min.x || key.y < min.y || key.z < min.z || key.x > max.x || key.y > max.y || key.z > max.z) {
                i.remove()
                cullees.add(subMap.values)
                size -= subMap.size
            }
        }
        if (size0 != size) {
            inner0 = null
            key0 = null
        }
        return cullees.flatten().asIterable()
    }

    private abstract inner class AbstractIterator<out V> : MutableIterator<V> {

        private var current: V? = null
        private var dead = false

        override fun hasNext(): Boolean {
            if (dead) return false
            if (current == null) current = findNext()
            dead = current == null
            return !dead
        }

        override fun next(): V {
            if (!hasNext()) throw NoSuchElementException(
                "No more elements exist in this iterator $this"
            )
            val result: V = current!!
            current = findNext()
            dead = current == null
            return result
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }

        private val oi: Iterator<Map.Entry<LesserCoarseKey, Map<GreaterCoarseKey, T>>>
        private var ii: Iterator<Map.Entry<GreaterCoarseKey, T>>? = null
        private var lesserKey: LesserCoarseKey? = null
        fun findNext(): V? {
            val oi = oi
            var ii = ii
            while (ii == null || !ii.hasNext()) {
                if (oi.hasNext()) {
                    val entry = oi.next()
                    lesserKey = entry.key
                    this.ii = entry.value.entries.iterator()
                    ii = this.ii
                } else return null
            }
            val entry = ii.next()
            val coords = coords(lesserKey, entry.key)
            return map(coords, entry.value)
        }

        protected abstract fun map(key: ThreeDimensionalIntVector, value: T): V

        init {
            oi = space.entries.iterator()
        }
    }

    private inner class KeySet : AbstractSet<ThreeDimensionalIntVector>() {
        private inner class Iter : AbstractIterator<ThreeDimensionalIntVector?>() {
            override fun map(key: ThreeDimensionalIntVector, value: T): ThreeDimensionalIntVector = key
        }

        override fun iterator(): Iter = Iter()

        override val size: Int
            get() = this@SparseThreeDimensionalSpatialMap.size
    }

    override val keys: MutableSet<ThreeDimensionalIntVector?>
        get() = KeySet()

    private inner class ValueCollection : AbstractCollection<T>() {
        private inner class Iter : AbstractIterator<T>() {
            override fun map(key: ThreeDimensionalIntVector, value: T): T = value
        }

        override fun iterator(): MutableIterator<T> = Iter()

        override val size: Int
            get() = this@SparseThreeDimensionalSpatialMap.size
    }

    override val values: MutableCollection<T?>
        get() = ValueCollection()

    private inner class EntrySet : AbstractSet<MutableMap.MutableEntry<ThreeDimensionalIntVector?, T?>>() {
        private inner class Iter : AbstractIterator<MutableMap.MutableEntry<ThreeDimensionalIntVector?, T?>>() {
            override fun map(
                key: ThreeDimensionalIntVector,
                value: T
            ): MutableMap.MutableEntry<ThreeDimensionalIntVector?, T?> = SimpleEntry(key, value)
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<ThreeDimensionalIntVector?, T?>> = Iter()

        override val size: Int
            get() = this@SparseThreeDimensionalSpatialMap.size
    }

    override val entries: MutableSet<MutableMap.MutableEntry<ThreeDimensionalIntVector?, T?>>
        get() = EntrySet()

    private fun lesserKey(coords: ThreeDimensionalIntVector): LesserCoarseKey =
        LesserCoarseKey(coords.x shr order, coords.y shr order, coords.z shr order)

    private fun greaterKey(coords: ThreeDimensionalIntVector): GreaterCoarseKey =
        GreaterCoarseKey(coords.x and mask, coords.y and mask, coords.z and mask)

    private fun coords(lesserKey: LesserCoarseKey?, greaterKey: GreaterCoarseKey): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            lesserKey!!.x shl order and mask.inv() or greaterKey.x.toInt(),
            lesserKey.y shl order and mask.inv() or greaterKey.y.toInt(),
            lesserKey.z shl order and mask.inv() or greaterKey.z.toInt()
        )
    }

    private fun acquireInner(coords: ThreeDimensionalIntVector): Map<GreaterCoarseKey, T>? {
        if (coords == key0) return inner0
        val lesserKey = lesserKey(coords.also { key0 = it })
        return space[lesserKey].also { inner0 = it }
    }

    companion object {
        private const val INNER_LOAD_FACTOR = 0.75f
        private const val OUTER_LOAD_FACTOR = 0.9f
    }

    init {
        space = HashMap(
            1 shl order shr 2, OUTER_LOAD_FACTOR
        )
    }
}