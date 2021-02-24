package com.extollit.gaming.ai.path.vector

class ThreeDimensionalIIntBox(@JvmField val min: ThreeDimensionalIntVector, @JvmField val max: ThreeDimensionalIntVector) {
    constructor(x0: Int, y0: Int, z0: Int, xN: Int, yN: Int, zN: Int) : this(ThreeDimensionalIntVector(x0, y0, z0), ThreeDimensionalIntVector(xN, yN, zN))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ThreeDimensionalIIntBox
        return if (min != that.min) false else max == that.max
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }

    override fun toString(): String = "$min to $max"

    fun contains(x: Int, y: Int, z: Int): Boolean =
        x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z

    fun contains(x: Double, y: Double, z: Double): Boolean =
        x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z

    operator fun contains(other: ThreeDimensionalIIntBox): Boolean =
        contains(other.min.x, other.min.y, other.min.z) && contains(other.max.x, other.max.y, other.max.z)
}
