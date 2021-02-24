package com.extollit.gaming.ai.path.vector

import kotlin.math.sqrt

/**
 * Represents a more specific point in 3D space.
 * Not per voxel unit unlike ThreeDimensionalIntVector
 */
class ThreeDimensionalDoubleVector(
    /** Represents the X position in 3d space. */
    var x: Double,
    /** Represents the Y position in 3d space. */
    var y: Double,
    /** Represents the Z position in 3d space. */
    var z: Double
) {

    constructor(copy: ThreeDimensionalDoubleVector) : this(copy.x, copy.y, copy.z)

    constructor(offset: VertexOffset): this(offset.deltaX.toDouble(), offset.deltaY.toDouble(), offset.deltaZ.toDouble())

    constructor(copy: ThreeDimensionalIntVector): this(copy.x.toDouble(), copy.y.toDouble(), copy.z.toDouble())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val vec3d = other as ThreeDimensionalDoubleVector
        if (vec3d.x.compareTo(x) != 0) return false
        return if (vec3d.y.compareTo(y) != 0) false else vec3d.z.compareTo(z) == 0
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long = java.lang.Double.doubleToLongBits(x)
        result = (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(z)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun toString(): String = "<$x, $y, $z>"

    fun proj(other: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector = mulOf(dot(other) / mg2())

    /**
     * Multiplies coordinates by itself and adds the sum of all of them
     * EX if the position is (2, 2, 2) the result would be 2^2 + 2^2 + 2^2 or 4 + 4 + 4 = 12
     *
     * @return The sum of the power of coordinates combined.
     */
    fun mg2(): Double = x * x + y * y + z * z

    fun subOf(other: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            x - other.x,
            y - other.y,
            z - other.z
        )
    }

    fun set(other: ThreeDimensionalDoubleVector) {
        x = other.x
        y = other.y
        z = other.z
    }

    fun subOf(x: Double, y: Double, z: Double): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            this.x - x,
            this.y - y,
            this.z - z
        )
    }

    fun plusOf(other: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            x + other.x,
            y + other.y,
            z + other.z
        )
    }

    fun plusOf(dx: Double, dy: Double, dz: Double): ThreeDimensionalDoubleVector =
        ThreeDimensionalDoubleVector(x + dx, y + dy, z + dz)

    fun dot(other: ThreeDimensionalDoubleVector): Double = x * other.x + y * other.y + z * other.z

    fun mulOf(s: Double): ThreeDimensionalDoubleVector = mulOf(s, s, s)

    fun mulOf(x: Double, y: Double, z: Double): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            this.x * x,
            this.y * y,
            this.z * z
        )
    }

    fun multiply(amount: Double) {
        multiply(amount, amount, amount)
    }

    fun multiply(x: Double, y: Double, z: Double) {
        this.x *= x
        this.y *= y
        this.z *= z
    }

    fun subtract(x: Double, y: Double, z: Double) {
        this.x -= x
        this.y -= y
        this.z -= z
    }

    fun subtract(vector: ThreeDimensionalDoubleVector) {
        x -= vector.x
        y -= vector.y
        z -= vector.z
    }

    fun subtract(vector: ThreeDimensionalIntVector) {
        x -= vector.x
        y -= vector.y
        z -= vector.z
    }

    fun add(vector: ThreeDimensionalDoubleVector) {
        x += vector.x
        y += vector.y
        z += vector.z
    }

    fun add(x: Double, y: Double, z: Double) {
        this.x += x
        this.y += y
        this.z += z
    }

    operator fun times(other: ThreeDimensionalIntVector): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            x * other.x.toDouble(),
            y * other.y.toDouble(),
            z * other.z.toDouble()
        )
    }

    fun mg(): Double = sqrt(mg2())

    fun squared(): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            x * x,
            y * y,
            z * z
        )
    }

    fun negated(): ThreeDimensionalDoubleVector = ThreeDimensionalDoubleVector(-x, -y, -z)

    fun normalized(): ThreeDimensionalDoubleVector {
        val mg = mg()
        return ThreeDimensionalDoubleVector(x / mg, y / mg, z / mg)
    }

    fun contains(x: Double, y: Double, z: Double): Boolean = this.x == x && this.y == y && this.z == z

    fun contains(x: Int, y: Int, z: Int): Boolean =
        this.x == x.toDouble() && this.y == y.toDouble() && this.z == z.toDouble()

    /**
     * Check if this [ThreeDimensionalDoubleVector] is the same as another [ThreeDimensionalDoubleVector]
     *
     * @param coordinates The coordinates to crosscheck over this one
     *
     * @return If [coordinates] has the same x, y, and z as this [ThreeDimensionalDoubleVector]
     */
    fun sameAs(coordinates: ThreeDimensionalIntVector): Boolean =
        contains(coordinates.x, coordinates.y, coordinates.z)
}