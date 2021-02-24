package com.extollit.gaming.ai.path.vector

import java.text.MessageFormat


class ThreeDimensionalDoubleVector {
    var x: Double
    var y: Double
    var z: Double

    constructor(copy: ThreeDimensionalDoubleVector) {
        x = copy.x
        y = copy.y
        z = copy.z
    }

    constructor(copy: com.extollit.linalg.mutable.Vec3d) {
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
        var temp: Long
        temp = java.lang.Double.doubleToLongBits(x)
        result = (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(z)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun toString(): String {
        return MessageFormat.format("<{0,number,#.#}, {1,number,#.#}, {2,number,#.#}>", x, y, z)
    }

    fun proj(other: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector {
        return mulOf(dot(other) / mg2())
    }

    fun mg2(): Double {
        return x * x + y * y + z * z
    }

    fun subOf(other: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            x - other.x,
            y - other.y,
            z - other.z
        )
    }

    fun subOf(other: com.extollit.linalg.mutable.Vec3d): ThreeDimensionalDoubleVector {
        return subOf(other.x, other.y, other.z)
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

    fun plusOf(other: com.extollit.linalg.mutable.Vec3d): ThreeDimensionalDoubleVector {
        return plusOf(other.x, other.y, other.z)
    }

    fun plusOf(dx: Double, dy: Double, dz: Double): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(x + dx, y + dy, z + dz)
    }

    fun dot(other: ThreeDimensionalDoubleVector): Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun dot(other: com.extollit.linalg.mutable.Vec3d): Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun mulOf(s: Double): ThreeDimensionalDoubleVector {
        return mulOf(s, s, s)
    }

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
        this.x -= vector.x
        this.y -= vector.y
        this.z -= vector.z
    }

    fun sub(vector: ThreeDimensionalIntVector) {
        this.x -= vector.x
        this.y -= vector.y
        this.z -= vector.z
    }

    fun add(vector: ThreeDimensionalDoubleVector) {
        this.x += vector.x
        this.y += vector.y
        this.z += vector.z
    }

    fun mulOf(other: ThreeDimensionalIntVector): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            x * other.x.toDouble(),
            y * other.y.toDouble(),
            z * other.z.toDouble()
        )
    }

    fun mutableCrossOf(other: ThreeDimensionalDoubleVector): com.extollit.linalg.mutable.Vec3d {
        return com.extollit.linalg.mutable.Vec3d(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    fun cross(other: com.extollit.linalg.mutable.Vec3d) {
        val x = y * other.z - z * other.y
        val y = z * other.x - this.x * other.z
        val z = this.x * other.y - this.y * other.x
        other.x = x
        other.y = y
        other.z = z
    }

    fun mg(): Double {
        return Math.sqrt(mg2())
    }

    fun squared(): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(
            x * x,
            y * y,
            z * z
        )
    }

    fun negated(): ThreeDimensionalDoubleVector {
        return ThreeDimensionalDoubleVector(-x, -y, -z)
    }

    fun normalized(): ThreeDimensionalDoubleVector {
        val mg = mg()
        return ThreeDimensionalDoubleVector(x / mg, y / mg, z / mg)
    }

    fun contains(x: Double, y: Double, z: Double): Boolean {
        return this.x == x && this.y == y && this.z == z
    }

    fun contains(coordinates: com.extollit.linalg.immutable.Vec3d): Boolean {
        return contains(coordinates.x, coordinates.y, coordinates.z)
    }

    fun contains(coordinates: com.extollit.linalg.mutable.Vec3d): Boolean {
        return contains(coordinates.x, coordinates.y, coordinates.z)
    }

    fun contains(x: Int, y: Int, z: Int): Boolean {
        return this.x == x.toDouble() && this.y == y.toDouble() && this.z == z.toDouble()
    }

    fun contains(coordinates: ThreeDimensionalIntVector): Boolean {
        return contains(coordinates.x, coordinates.y, coordinates.z)
    }
}