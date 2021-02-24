package com.extollit.gaming.ai.path.vector

import com.extollit.linalg.immutable.Vec3d
import com.extollit.linalg.immutable.VertexOffset
import java.text.MessageFormat
import kotlin.math.abs
import kotlin.math.sqrt

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
        MessageFormat.format("<{0,number,#}, {1,number,#}, {2,number,#}>", x, y, z)

    fun plusOf(other: VertexOffset): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            x + other.dx,
            y + other.dy,
            z + other.dz
        )
    }

    fun subOf(other: VertexOffset): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            x - other.dx,
            y - other.dy,
            z - other.dz
        )
    }

    fun plusOf(other: ThreeDimensionalIntVector): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            x + other.x,
            y + other.y,
            z + other.z
        )
    }

    fun plusOf(delta: Int): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            x + delta,
            y + delta,
            z + delta
        )
    }

    fun subOf(other: ThreeDimensionalIntVector): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            x - other.x,
            y - other.y,
            z - other.z
        )
    }

    fun subOf(delta: Int): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            x - delta,
            y - delta,
            z - delta
        )
    }

    fun dot(other: ThreeDimensionalIntVector): Int =
        x * other.x + y * other.y + z * other.z

    fun contains(x: Double, y: Double, z: Double): Boolean =
        this.x.toDouble() == x && this.y.toDouble() == y && this.z.toDouble() == z

    fun contains(coordinates: Vec3d): Boolean =
        contains(coordinates.x, coordinates.y, coordinates.z)

    fun magnitude(): Double =
        sqrt((x * x + y * y + z * z).toDouble())

    fun taxiTo(other: ThreeDimensionalIntVector): Int =
        abs(other.x - x) + abs(other.y - y) + abs(other.z - z)

    fun truncatedEquals(x: Double, y: Double, z: Double): Boolean =
        this.x == x.toInt() && this.y == y.toInt() && this.z == z.toInt()

    companion object {
        @JvmField
        val ZERO = ThreeDimensionalIntVector(0, 0, 0)

        fun truncated(other: Vec3d): ThreeDimensionalIntVector =
            ThreeDimensionalIntVector(other.x.toInt(), other.y.toInt(), other.z.toInt())
    }
}
