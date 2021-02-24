package com.extollit.gaming.ai.path.vector

import com.extollit.linalg.AbstractSpatialRegion
import com.extollit.linalg.ISpatialRegion
import kotlin.math.abs

class ThreeDimensionalIIntBox(@JvmField val min: ThreeDimensionalIntVector, @JvmField val max: ThreeDimensionalIntVector) :
    AbstractSpatialRegion(), ISpatialRegion {
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

    override fun contains(x: Int, y: Int, z: Int): Boolean =
        x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z

    override fun accept(visitor: ISpatialRegion.Visitor?) {
        // nope.
    }

    override fun contains(x: Double, y: Double, z: Double): Boolean =
        x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z

    operator fun contains(other: ThreeDimensionalIIntBox): Boolean =
        contains(other.min.x, other.min.y, other.min.z) && contains(other.max.x, other.max.y, other.max.z)

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
