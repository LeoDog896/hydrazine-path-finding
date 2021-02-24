package com.extollit.gaming.ai.path.vector

import kotlin.math.max
import kotlin.math.min

// TODO isn't this the same as ThreeDimensionalIntAxisAlignedBox?
class ThreeDimensionalAxisAlignedBoundaryBox {
    val min: ThreeDimensionalDoubleVector
    val max: ThreeDimensionalDoubleVector

    constructor(x0: Double, y0: Double, z0: Double, xN: Double, yN: Double, zN: Double) : this(
        ThreeDimensionalDoubleVector(
            min(x0, xN),
            min(y0, yN),
            min(z0, zN)
        ),
        ThreeDimensionalDoubleVector(
            max(xN, x0),
            max(yN, y0),
            max(zN, z0)
        )
    )

    constructor(min: ThreeDimensionalDoubleVector, max: ThreeDimensionalDoubleVector) {
        if (min.x > max.x || min.y > max.y || min.z > max.z) {
            this.min = ThreeDimensionalDoubleVector(
                min(min.x, max.x),
                min(min.y, max.y),
                min(min.z, max.z)
            )
            this.max = ThreeDimensionalDoubleVector(
                max(max.x, min.x),
                max(max.y, min.y),
                max(max.z, min.z)
            )
        } else {
            this.min = min
            this.max = max
        }
    }

    constructor(mutable: ThreeDimensionalAxisAlignedBoundaryBox) {
        min = ThreeDimensionalDoubleVector(mutable.min)
        max = ThreeDimensionalDoubleVector(mutable.max)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ThreeDimensionalAxisAlignedBoundaryBox
        return if (min != that.min) false else max == that.max
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }

    override fun toString(): String = "$min to $max"

    fun contains(x: Double, y: Double, z: Double): Boolean =
        x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z

    fun center(): ThreeDimensionalDoubleVector {
        val min = min
        val max = max
        return ThreeDimensionalDoubleVector(
            (max.x + min.x) / 2,
            (max.y + min.y) / 2,
            (max.z + min.z) / 2
        )
    }

    fun mg2(other: ThreeDimensionalAxisAlignedBoundaryBox): Double {
        val left0 = min
        val right0 = other.min
        val leftN = max
        val rightN = other.max
        var dx = max(left0.x - rightN.x, right0.x - leftN.x)
        var dy = max(left0.y - rightN.y, right0.y - leftN.y)
        var dz = max(left0.z - rightN.z, right0.z - leftN.z)
        if (dx < 0) dx = 0.0
        if (dy < 0) dy = 0.0
        if (dz < 0) dz = 0.0
        return dx * dx + dy * dy + dz * dz
    }

    private fun intersection(d1: Double, d2: Double, p1: ThreeDimensionalDoubleVector, p2: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector? {
        if (d1 * d2 >= 0 || d1 == d2) return null
        val f = -d1 / (d2 - d1)
        return ThreeDimensionalDoubleVector(
            p1.x + (p2.x - p1.x) * f,
            p1.y + (p2.y - p1.y) * f,
            p1.x + (p2.z - p1.z) * f
        )
    }

    fun add(dx: Double, dy: Double, dz: Double) {
        min.add(dx, dy, dz)
        max.add(dx, dy, dz)
    }


    fun intersection(p0: ThreeDimensionalDoubleVector, pN: ThreeDimensionalDoubleVector): ThreeDimensionalDoubleVector? {
        val min = min
        val max = max
        var i: ThreeDimensionalDoubleVector? = intersection(p0.x - min.x, pN.x - min.x, p0, pN)
        if (i != null && i.z > min.z && i.z < max.z && i.y > min.y && i.y < max.y) return i
        i = intersection(p0.y - min.y, pN.y - min.y, p0, pN)
        if (i != null && i.z > min.z && i.z < max.z && i.x > min.x && i.x < max.x) return i
        i = intersection(p0.z - min.z, pN.z - min.z, p0, pN)
        if (i != null && i.x > min.x && i.x < max.x && i.y > min.y && i.y < max.y) return i
        i = intersection(p0.x - max.x, pN.x - max.x, p0, pN)
        if (i != null && i.z > min.z && i.z < max.z && i.y > min.y && i.y < max.y) return i
        i = intersection(p0.y - max.y, pN.y - max.y, p0, pN)
        if (i != null && i.z > min.z && i.z < max.z && i.x > min.x && i.x < max.x) return i
        i = intersection(p0.z - max.z, pN.z - max.z, p0, pN)
        return if (i != null && i.x > min.x && i.x < max.x && i.y > min.y && i.y < max.y) i else null
    }
}