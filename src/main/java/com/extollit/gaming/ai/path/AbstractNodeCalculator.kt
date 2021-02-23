package com.extollit.gaming.ai.path


import com.extollit.gaming.ai.path.model.*
import java.lang.Math
import com.extollit.linalg.immutable.Vec3i

internal abstract class AbstractNodeCalculator(protected val instanceSpace: IInstanceSpace) : INodeCalculator {
    protected var capabilities: IPathingEntity.Capabilities? = null
    protected var discreteSize = 0
    protected var tall = 0
    protected var actualSize = 0f
    override fun applySubject(subject: IPathingEntity) {
        actualSize = subject.width()
        discreteSize = Math.floor((subject.width() + 1).toDouble()).toInt()
        tall = Math.floor((subject.height() + 1).toDouble()).toInt()
        capabilities = subject.capabilities()
    }

    protected fun verticalClearanceAt(
        sampler: FlagSampler,
        max: Int, flags: Byte,
        passibility: Passibility?,
        delta: Vec3i,
        x: Int, y: Int, z: Int,
        partY: Float
    ): Passibility? {
        var passibility = passibility
        var clearanceFlags = flags
        val yMax = y + max
        val yN = Math.max(y, y - delta.y) + tall
        var yt = y
        val yNa = yN + Math.floor(partY.toDouble()).toInt()
        while (yt < yNa && yt < yMax) {
            passibility = passibility!!.between(clearance(clearanceFlags))
            clearanceFlags = sampler.flagsAt(x, ++yt, z)
        }
        if (yt < yN && yt < yMax && insufficientHeadClearance(clearanceFlags, partY, x, yt, z)) passibility =
            passibility!!.between(clearance(clearanceFlags))
        return passibility
    }

    protected fun insufficientHeadClearance(flags: Byte, partialY0: Float, x: Int, yN: Int, z: Int): Boolean {
        return bottomOffsetAt(flags, x, yN, z) + partialY0 > 0
    }

    private fun bottomOffsetAt(flags: Byte, x: Int, y: Int, z: Int): Float {
        if (Element.air.flagsIn(flags)
            || Logic.climbable(flags)
            || Element.earth.flagsIn(flags) && Logic.nothing.flagsIn(flags) || swimmingRequiredFor(flags)
        ) return 0f
        val block = instanceSpace.blockObjectAt(x, y, z)
        return if (!block.isImpeding) 0f else block.bounds().min.y.toFloat()
    }

    private fun clearance(flags: Byte): Passibility {
        return PassibilityHelpers.clearance(flags, capabilities)
    }

    protected fun topOffsetAt(sampler: FlagSampler, x: Int, y: Int, z: Int): Float {
        return topOffsetAt(sampler.flagsAt(x, y, z), x, y, z)
    }

    protected fun topOffsetAt(flags: Byte, x: Int, y: Int, z: Int): Float {
        if (Element.air.flagsIn(flags)
            || Logic.climbable(flags)
            || Element.earth.flagsIn(flags) && Logic.nothing.flagsIn(flags) || Element.water.flagsIn(flags) && (capabilities!!.aquatic() || !capabilities!!.swimmer())
        ) return 0f
        if (swimmingRequiredFor(flags)) return -0.5f
        val block = instanceSpace.blockObjectAt(x, y, z)
        if (!block.isImpeding) {
            if (Element.earth.flagsIn(flags)) {
                val blockBelow = instanceSpace.blockObjectAt(x, y - 1, z)
                if (!blockBelow.isFullyBounded) {
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
        passibility: Passibility?,
        origin: Vec3i,
        minY: Int,
        minPartY: Float
    ): Passibility? {
        var passibility = passibility
        val yN = minY + tall
        val yNa = yN + Math.floor(minPartY.toDouble()).toInt()
        var x = origin.x
        val xN = origin.x + discreteSize
        while (x < xN) {
            var z = origin.z
            val zN = origin.z + discreteSize
            while (z < zN) {
                for (y in origin.y + tall until yNa) passibility =
                    passibility?.between(clearance(sampler.flagsAt(x, y, z)))
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
                    if (insufficientHeadClearance(flags, minPartY, x, yNa, z)) passibility =
                        passibility?.between(clearance(flags))
                    ++z
                }
                ++x
            }
        }
        return passibility
    }

    companion object {
        @JvmStatic
        protected fun swimmingRequiredFor(flags: Byte): Boolean {
            return Element.water.flagsIn(flags) || Element.fire.flagsIn(flags) && !Logic.fuzzy.flagsIn(flags)
        }
    }
}