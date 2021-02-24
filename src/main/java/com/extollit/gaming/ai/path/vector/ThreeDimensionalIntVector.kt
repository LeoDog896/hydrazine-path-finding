package com.extollit.gaming.ai.path.vector

import com.extollit.linalg.immutable.VertexOffset

/**
 * Represents a position in 3D space, not meant to be per-exact but per voxel in a voxel world.
 */
class ThreeDimensionalIntVector(
    /** The X position of this Vector. */
    @JvmField
    var x: Int,
    /** The Y position of this Vector. UP and DOWN are the directions of this plane. */
    @JvmField
    var y: Int,
    /** The Z position of this Vector. */
    @JvmField
    var z: Int
) {

    constructor(copy: ThreeDimensionalIntVector): this(copy.x, copy.y, copy.z)

    constructor(offset: VertexOffset): this(offset.dx.toInt(), offset.dy.toInt(), offset.dz.toInt())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val vec3i = other as ThreeDimensionalIntVector
        if (x != vec3i.x) return false
        return if (y != vec3i.y) false else z == vec3i.z
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun toString(): String =
        "<{0,$x,#}, {1,$y,#}, {2,$z,#}>"

    fun subOf(other: ThreeDimensionalIntVector): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            x - other.x,
            y - other.y,
            z - other.z
        )
    }

    fun dot(other: ThreeDimensionalIntVector): Int =
        x * other.x + y * other.y + z * other.z

    fun contains(x: Double, y: Double, z: Double): Boolean =
        this.x.toDouble() == x && this.y.toDouble() == y && this.z.toDouble() == z

    companion object {
        @JvmField
        /** Represents a [ThreeDimensionalIntVector] with all positions set to 0. */
        val ZERO: ThreeDimensionalIntVector = ThreeDimensionalIntVector(0, 0, 0)
    }
}
