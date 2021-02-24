package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.*
import com.extollit.gaming.ai.path.node.Node
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector
import kotlin.math.roundToInt

internal class GroundNodeCalculator(instanceSpace: IInstanceSpace) : AbstractNodeCalculator(instanceSpace) {

    // TODO hmmmmmm
    override fun passibleNodeNear(coords0: ThreeDimensionalIntVector, origin: ThreeDimensionalIntVector?, flagSampler: FlagSampler): Node {
        val point: Node
        val capabilities = capabilities
        val x0 = coords0.x
        val y0 = coords0.y
        val z0 = coords0.z
        val delta: ThreeDimensionalIntVector = if (origin != null) coords0.subOf(origin) else ThreeDimensionalIntVector.ZERO
        val hasOrigin = delta != ThreeDimensionalIntVector.ZERO
        val climbsLadders = this.capabilities!!.climber()
        var passibility: Passibility = Passibility.Passible
        var minY = Int.MIN_VALUE
        var minPartY = 0f
        val r = discreteSize / 2
        var x = x0 - r
        val xN = x0 + discreteSize - r
        while (x < xN) {
            var z = z0 - r
            val zN = z0 + discreteSize - r
            while (z < zN) {
                var y = y0
                var partY = topOffsetAt(
                    flagSampler,
                    x - delta.x,
                    y - delta.y - 1,
                    z - delta.z
                )
                var flags = flagSampler.flagsAt(x, y, z)
                val impedesMovement: Boolean
                if (PassibilityHelpers.impedesMovement(flags, capabilities).apply { impedesMovement = this }) {
                    val partialDisparity = partY - topOffsetAt(flags, x, y++, z)
                    flags = flagSampler.flagsAt(x, y, z)
                    if (partialDisparity < 0 || PassibilityHelpers.impedesMovement(flags, capabilities)) {
                        if (!hasOrigin) return Node(coords0, Passibility.impassible, flagSampler.volatility > 0)
                        if (delta.x * delta.x + delta.z * delta.z <= 1) {
                            y -= delta.y + 1
                            do flags = flagSampler.flagsAt(
                                x - delta.x,
                                y++,
                                z - delta.z
                            ) while (climbsLadders && Logic.climbable(flags))
                        }
                        if (PassibilityHelpers.impedesMovement(
                                flagSampler.flagsAt(x, --y, z).apply { flags = this },
                                capabilities
                            ) && (PassibilityHelpers.impedesMovement(
                                flagSampler.flagsAt(x, ++y, z).apply { flags = this },
                                capabilities
                            ) || partY < 0)
                        ) return Node(coords0, Passibility.impassible, flagSampler.volatility > 0)
                    }
                }
                partY = topOffsetAt(flagSampler, x, y - 1, z)
                val ys: Int
                passibility =
                    verticalClearanceAt(flagSampler, tall, flags, passibility, delta, x, y.apply { ys = this }, z, partY)
                var swimable = false
                let {
                    var condition = !impedesMovement || unstable(flags)
                    var j = 0
                    while (condition && !it.swimable(flags).apply { swimable = this } && j <= MAX_SURVIVE_FALL_DISTANCE) {
                        flags = flagSampler.flagsAt(x, --y, z)
                        j++
                        condition = unstable(flags)
                    }
                }
                if (swimable) {
                    val cesaLimit = y + CESA_LIMIT
                    val flags00 = flags
                    var flags0: Byte
                    do {
                        flags0 = flags
                        flags = flagSampler.flagsAt(x, ++y, z)
                    } while (swimable(flags) && unstable(flags) && y < cesaLimit)
                    if (y >= cesaLimit) {
                        y -= CESA_LIMIT + 1
                        flags = flags00
                    } else {
                        y--
                        flags = flags0
                    }
                }
                partY = topOffsetAt(flags, x, y++, z)
                passibility = verticalClearanceAt(
                    flagSampler,
                    ys - y,
                    flagSampler.flagsAt(x, y, z),
                    passibility,
                    delta,
                    x,
                    y,
                    z,
                    partY
                )
                if (y > minY) {
                    minY = y
                    minPartY = partY
                } else if (y == minY && partY > minPartY) minPartY = partY
                passibility = passibility.between(
                    PassibilityHelpers.passibilityFrom(
                        flagSampler.flagsAt(x, y, z),
                        capabilities
                    )
                )
                if (passibility.impassible(capabilities)) return Node(
                    coords0,
                    Passibility.impassible,
                    flagSampler.volatility > 0
                )
                ++z
            }
            ++x
        }

        if (hasOrigin && !passibility.impassible(capabilities)) passibility =
            originHeadClearance(flagSampler, passibility, origin!!, minY, minPartY)
        passibility = fallingSafety(passibility, y0, minY)
        if (passibility.impassible(capabilities)) passibility = Passibility.impassible
        point = Node(ThreeDimensionalIntVector(x0, minY + minPartY.roundToInt(), z0))
        point.passibility(passibility)
        point.volatile_(flagSampler.volatility > 0)
        return point
    }

    override fun omnidirectional(): Boolean = false

    private fun fallingSafety(passibility: Passibility, y0: Int, minY: Int): Passibility {
        var mutablePassibility = passibility
        val dy = y0 - minY
        if (dy > 1) mutablePassibility = mutablePassibility.between(
            if (dy > MAX_SAFE_FALL_DISTANCE) Passibility.Dangerous else Passibility.Risky
        )
        return mutablePassibility
    }

    private fun swimable(flags: Byte): Boolean {
        return capabilities!!.swimmer() && swimmingRequiredFor(flags) && (Element.water.flagsIn(
            flags
        ) || capabilities!!.fireResistant())
    }

    companion object {
        private var MAX_SAFE_FALL_DISTANCE = 4
        private var MAX_SURVIVE_FALL_DISTANCE = 20
        private var CESA_LIMIT = 16

        /**
         * Takes the configuration model passed in and uses it to configure this node calculator.
         *
         * @param configModel The configuration model to base this GroundNodeCalculator off of.
         */
        fun configureFrom(configModel: IConfigModel) {
            MAX_SAFE_FALL_DISTANCE = configModel.safeFallDistance().toInt()
            MAX_SURVIVE_FALL_DISTANCE = configModel.surviveFallDistance().toInt()
            CESA_LIMIT = configModel.cesaLimit().toInt()
        }

        private fun unstable(flags: Byte): Boolean = !Element.earth.flagsIn(flags) || Logic.ladder.flagsIn(flags)
    }
}