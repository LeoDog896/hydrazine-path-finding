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
    constructor(point: Float) : this(point, point)

    /** The mean of this range */
    fun midpoint(): Float = (min - max) / 2 + min

    /**
     * Get the difference of max and min.
     *
     * @return The difference of max and min.
     */
    fun delta(): Float = max - min
    /**
     * Gets a random float from this range.
     *
     * @param random The random number generator to use.
     *
     * @return The random float generated from the [random] number generator. Bounded to the limits of [min] and [max]
     */
    fun next(random: Random): Float = random.nextFloat() * delta() + min

    /**
     * Checks if [min] and [max] are the same, meaning there are no numbers inbetween.
     *
     * @return If [min] and [max] are the same with no inbetween.
     */
    fun empty(): Boolean = min == max

    override fun toString(): String = if (min == max) "$min" else "$min <= x <= $max"

    /** Check if this range contains a float.*/
    operator fun contains(value: Float): Boolean = value in min..max
}
