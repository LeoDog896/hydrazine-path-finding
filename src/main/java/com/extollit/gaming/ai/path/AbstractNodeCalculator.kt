package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.*
import com.extollit.gaming.ai.path.node.INodeCalculator
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector
import kotlin.math.floor

internal abstract class AbstractNodeCalculator(protected val instanceSpace: IInstanceSpace) : INodeCalculator {
    protected var capabilities: IPathingEntity.Capabilities? = null
    protected var discreteSize = 0
    protected var tall = 0
    protected var actualSize = 0f

    override fun applySubject(subject: IPathingEntity) {
        actualSize = subject.width()
        discreteSize = floor((subject.width() + 1).toDouble()).toInt()
        tall = floor((subject.height() + 1).toDouble()).toInt()
        capabilities = subject.capabilities()
    }

    protected fun verticalClearanceAt(
        sampler: FlagSampler,
        max: Int, flags: Byte,
        passibility: Passibility,
        delta: ThreeDimensionalIntVector,
        x: Int, y: Int, z: Int,
        partY: Float
    ): Passibility {
        var mutablePassibility = passibility
        var clearanceFlags = flags
        val yMax = y + max
        val yN = y.coerceAtLeast(y - delta.y) + tall
        var yt = y
        val yNa = yN + floor(partY.toDouble()).toInt()
        while (yt < yNa && yt < yMax) {
            mutablePassibility = mutablePassibility.between(clearance(clearanceFlags))
            clearanceFlags = sampler.flagsAt(x, ++yt, z)
        }
        if (yt < yN && yt < yMax && insufficientHeadClearance(clearanceFlags, partY, x, yt, z)) mutablePassibility =
            mutablePassibility.between(clearance(clearanceFlags))
        return mutablePassibility
    }

    protected fun insufficientHeadClearance(flags: Byte, partialY0: Float, x: Int, yN: Int, z: Int): Boolean =
        bottomOffsetAt(flags, x, yN, z) + partialY0 > 0

    private fun bottomOffsetAt(flags: Byte, x: Int, y: Int, z: Int): Float {
        if (Element.air.flagsIn(flags)
            || Logic.climbable(flags)
            || Element.earth.flagsIn(flags) && Logic.nothing.flagsIn(flags) || swimmingRequiredFor(flags)
        ) return 0f
        val block = instanceSpace.blockObjectAt(x, y, z)
        return if (!block.impeding) 0f else block.bounds().min.y.toFloat()
    }

    private fun clearance(flags: Byte): Passibility =
        PassibilityHelpers.clearance(flags, capabilities)

    protected fun topOffsetAt(sampler: FlagSampler, x: Int, y: Int, z: Int): Float =
        topOffsetAt(sampler.flagsAt(x, y, z), x, y, z)

    protected fun topOffsetAt(flags: Byte, x: Int, y: Int, z: Int): Float {
        if (Element.air.flagsIn(flags)
            || Logic.climbable(flags)
            || Element.earth.flagsIn(flags) && Logic.nothing.flagsIn(flags) || Element.water.flagsIn(flags) && (capabilities!!.aquatic() || !capabilities!!.swimmer())
        ) return 0f
        if (swimmingRequiredFor(flags)) return -0.5f
        val block = instanceSpace.blockObjectAt(x, y, z)
        if (!block.impeding) {
            if (Element.earth.flagsIn(flags)) {
                val blockBelow = instanceSpace.blockObjectAt(x, y - 1, z)
                if (!blockBelow.fullyBounded) {
                    var offset = blockBelow.bounds().max.y.toFloat() - 2
                    if (offset < -1) offset = 0f
                    return offset
                }
            }
            return 0f
        }
        return block.bounds().max.y.toFloat() - 1
    }

    protected fun originHeadClearance(
        sampler: FlagSampler,
        passibility: Passibility,
        origin: ThreeDimensionalIntVector,
        minY: Int,
        minPartY: Float
    ): Passibility {
        var mutablePassibility = passibility
        val yN = minY + tall
        val yNa = yN + floor(minPartY.toDouble()).toInt()
        var x = origin.x
        val xN = origin.x + discreteSize
        while (x < xN) {
            var z = origin.z
            val zN = origin.z + discreteSize
            while (z < zN) {
                for (y in origin.y + tall until yNa) mutablePassibility =
                    mutablePassibility.between(clearance(sampler.flagsAt(x, y, z)))
                ++z
            }
            ++x
        }
        if (yNa < yN) {
            var x = origin.x
            val xN = origin.x + discreteSize
            while (x < xN) {
                var z = origin.z
                val zN = origin.z + discreteSize
                while (z < zN) {
                    val flags = sampler.flagsAt(x, yNa, z)
                    if (insufficientHeadClearance(flags, minPartY, x, yNa, z)) mutablePassibility =
                        mutablePassibility.between(clearance(flags))
                    ++z
                }
                ++x
            }
        }
        return mutablePassibility
    }

    companion object {
        @JvmStatic
        protected fun swimmingRequiredFor(flags: Byte): Boolean =
            Element.water.flagsIn(flags) || Element.fire.flagsIn(flags) && !Logic.fuzzy.flagsIn(flags)
    }
}