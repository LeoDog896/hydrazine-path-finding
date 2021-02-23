package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.*
import com.extollit.linalg.immutable.Vec3i
import kotlin.math.roundToInt

internal class FluidicNodeCalculator(instanceSpace: IInstanceSpace) : AbstractNodeCalculator(instanceSpace),
    INodeCalculator {
    override fun passibleNodeNear(coords0: Vec3i, origin: Vec3i?, flagSampler: FlagSampler): Node {
        val point: Node
        val capabilities = capabilities
        val x0 = coords0.x
        val y0 = coords0.y
        val z0 = coords0.z
        val d: Vec3i = if (origin != null) coords0.subOf(origin) else Vec3i.ZERO
        val hasOrigin = d !== Vec3i.ZERO && d != Vec3i.ZERO
        var passibility: Passibility? = Passibility.passible
        var gravitation: Gravitation? = Gravitation.airborne
        var minY = Int.MIN_VALUE
        var minPartY = 0f
        val r = discreteSize / 2
        var x = x0 - r
        val xN = x0 + discreteSize - r
        while (x < xN) {
            var z = z0 - r
            val zN = z0 + discreteSize - r
            while (z < zN) {
                val flags = flagSampler.flagsAt(x, y0, z)
                val yb = y0 - 1
                val flagsBeneath = flagSampler.flagsAt(x, yb, z)
                gravitation = gravitation!!.between(PassibilityHelpers.gravitationFrom(flags))
                gravitation = gravitation.between(PassibilityHelpers.gravitationFrom(flagsBeneath))
                passibility =
                    if (PassibilityHelpers.impedesMovement(flags, capabilities)) return Node(
                        coords0,
                        Passibility.impassible,
                        flagSampler.volatility > 0,
                        gravitation
                    ) else passibility!!.between(PassibilityHelpers.passibilityFrom(flags, capabilities))
                val partY = topOffsetAt(flagsBeneath, x, yb, z)
                passibility = verticalClearanceAt(flagSampler, tall, flags, passibility, d, x, y0, z, partY)
                if (y0 > minY) {
                    minY = y0
                    minPartY = partY
                } else if (partY > minPartY) minPartY = partY
                if (passibility!!.impassible(capabilities)) return Node(
                    coords0,
                    Passibility.impassible,
                    flagSampler.volatility > 0,
                    gravitation
                )
                ++z
            }
            ++x
        }
        if (passibility!!.impassible(capabilities)) passibility =
            Passibility.impassible else if (hasOrigin) passibility =
            originHeadClearance(flagSampler, passibility, origin!!, minY, minPartY)
        point = Node(Vec3i(x0, minY + minPartY.roundToInt(), z0))
        point.passibility(passibility)
        point.gravitation(gravitation)
        point.volatile_(flagSampler.volatility > 0)
        return point
    }

    override fun omnidirectional(): Boolean {
        return true
    }
}