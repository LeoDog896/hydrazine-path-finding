package com.extollit.gaming.ai.path.vector

import com.extollit.linalg.AbstractSpatialRegion
import com.extollit.linalg.ISpatialRegion
import kotlin.math.abs

class ThreeDimensionalIntAxisAlignedBox(@JvmField val min: ThreeDimensionalIntVector, @JvmField val max: ThreeDimensionalIntVector) :
    AbstractSpatialRegion(), ISpatialRegion {
    constructor(x0: Int, y0: Int, z0: Int, xN: Int, yN: Int, zN: Int) : this(ThreeDimensionalIntVector(x0, y0, z0), ThreeDimensionalIntVector(xN, yN, zN)) {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ThreeDimensionalIntAxisAlignedBox
        return if (min != that.min) false else max == that.max
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }

    override fun toString(): String = "$min to $max"

    override fun contains(x: Int, y: Int, z: Int): Boolean =
        x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z

    override fun accept(visitor: ISpatialRegion.Visitor?) {
        // nope.
    }

    fun center(): ThreeDimensionalDoubleVector {
        val min = min
        val max = max
        return ThreeDimensionalDoubleVector(
            ((max.x + min.x).toFloat() / 2).toDouble(),
            ((max.y + min.y).toFloat() / 2).toDouble(),
            ((max.z + min.z).toFloat() / 2).toDouble()
        )
    }

    fun valid(): Boolean = min.x <= max.x && min.y <= max.y && min.z <= max.z

    override fun contains(x: Double, y: Double, z: Double): Boolean =
        x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z

    operator fun contains(other: ThreeDimensionalIntAxisAlignedBox): Boolean =
        contains(other.min.x, other.min.y, other.min.z) && contains(other.max.x, other.max.y, other.max.z)

    fun intersects(other: ThreeDimensionalIntAxisAlignedBox): Boolean {
        return lineDeltaFactor(min.x, max.x, other.min.x, other.max.x) or
                lineDeltaFactor(min.y, max.y, other.min.y, other.max.y) or
                lineDeltaFactor(min.z, max.z, other.min.z, other.max.z) == 0
    }

    fun intersection(other: ThreeDimensionalIntAxisAlignedBox): ThreeDimensionalIntAxisAlignedBox {
        return ThreeDimensionalIntAxisAlignedBox(
            Math.max(min.x, other.min.x),
            Math.max(min.y, other.min.y),
            Math.max(min.z, other.min.z),
            Math.min(max.x, other.max.x),
            Math.min(max.y, other.max.y),
            Math.min(max.z, other.max.z)
        )
    }

    fun midpoint(): ThreeDimensionalIntVector {
        return ThreeDimensionalIntVector(
            (max.x + min.x) / 2,
            (max.y + min.y) / 2,
            (max.z + min.z) / 2
        )
    }

    fun union(other: ThreeDimensionalIntAxisAlignedBox): ThreeDimensionalIntAxisAlignedBox {
        return ThreeDimensionalIntAxisAlignedBox(
            min.x.coerceAtMost(other.min.x),
            min.y.coerceAtMost(other.min.y),
            min.z.coerceAtMost(other.min.z),
            max.x.coerceAtLeast(other.max.x),
            max.y.coerceAtLeast(other.max.y),
            max.z.coerceAtLeast(other.max.z)
        )
    }

    fun union(x: Int, y: Int, z: Int): ThreeDimensionalIntAxisAlignedBox {
        return ThreeDimensionalIntAxisAlignedBox(
            min.x.coerceAtMost(x),
            min.y.coerceAtMost(y),
            min.z.coerceAtMost(z),
            max.x.coerceAtLeast(x),
            max.y.coerceAtLeast(y),
            max.z.coerceAtLeast(z)
        )
    }

    companion object {
        private fun lineDeltaFactor(leftMin: Int, leftMax: Int, rightMin: Int, rightMax: Int): Int {
            assert(leftMin >= Int.MIN_VALUE shr 1 && leftMax <= Int.MAX_VALUE shr 1)
            assert(rightMin >= Int.MIN_VALUE shr 1 && rightMax <= Int.MAX_VALUE shr 1)
            val leftWidth = leftMax - leftMin
            val rightWidth = rightMax - rightMin
            val leftMid = leftMin + (leftMax - leftMin shr 1)
            val rightMid = rightMin + (rightMax - rightMin shr 1)
            return (abs(leftMid - rightMid) shl 1) / (leftWidth + rightWidth + 1)
        }
    }
}
