package com.extollit.gaming.ai.path.num

import java.util.*

/** Convience method for creating float ranges. */
infix fun Float.range(max: Float): FloatingRange = FloatingRange(this, max)

/**
 * Type specific ranges for floats.
 */
// TODO use Kotlin's float range.
class FloatingRange(
    /** The minimum number this range goes to. */
    val min: Float,
    /** The maximum number this range goes to. */
    val max: Float
) {
    constructor(point: Float) : this(point, point) {}

    /** The mean of this range */
    fun midpoint(): Float = (min - max) / 2 + min

    fun ratio(value: Float): Float = (value - min) / delta()

    fun delta(): Float = max - min

    fun clamp(value: Float): Float {
        if (value < min) return min
        return if (value > max) max else value
    }

    /** Gets a random float from this range. */
    fun next(random: Random): Float = random.nextFloat() * delta() + min

    fun empty(): Boolean = min == max

    override fun toString(): String = if (min == max) "$min" else "$min <= x <= $max"

    /** Check if this range contains a float.*/
    operator fun contains(value: Float): Boolean = value in min..max
}
