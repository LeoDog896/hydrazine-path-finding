package com.extollit.gaming.ai.path.vector

import java.text.MessageFormat
import kotlin.math.sqrt


class ThreeDimensionalDoubleVector {
    var x: Double
    var y: Double
    var z: Double

    constructor(copy: ThreeDimensionalDoubleVector) {
        x = copy.x
        y = copy.y
        z = copy.z
    }

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(offset: VertexOffset) {
        x = offset.dx.toDouble()
        y = offset.dy.toDouble()
        z = offset.dz.toDouble()
    }

    constructor(copy: ThreeDimensionalIntVector) {
        x = copy.x.toDouble()
        y = copy.y.toDouble()
        z = copy.z.toDouble()
    }

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

    override fun toString(): String = MessageFormat.format("<{0,number,#.#}, {1,number,#.#}, {2,number,#.#}>", x, y, z)

    fun proj(other: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector = mulOf(dot(other) / mg2())

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

    fun mul(amount: Double) {
        mul(amount, amount, amount)
    }

    fun mul(x: Double, y: Double, z: Double) {
        this.x *= x
        this.y *= y
        this.z *= z
    }

    fun sub(x: Double, y: Double, z: Double) {
        this.x -= x
        this.y -= y
        this.z -= z
    }

    fun sub(vector: ThreeDimensionalDoubleVector) {
        x -= vector.x
        y -= vector.y
        z -= vector.z
    }

    fun sub(vector: ThreeDimensionalIntVector) {
        x -= vector.x
        y -= vector.y
        z -= vector.z
    }

    fun add(vector: ThreeDimensionalDoubleVector) {
        x += vector.x
        y += vector.y
        z += vector.z
    }

    fun mulOf(other: ThreeDimensionalIntVector): ThreeDimensionalDoubleVector {
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

    operator fun contains(coordinates: ThreeDimensionalIntVector): Boolean =
        contains(coordinates.x, coordinates.y, coordinates.z)
}