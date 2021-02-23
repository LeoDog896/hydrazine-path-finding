package com.extollit.gaming.ai.path

import kotlin.jvm.JvmOverloads
import java.lang.StringBuilder
import java.text.MessageFormat
import java.util.Objects
import com.extollit.collect.SparseSpatialMap
import com.extollit.linalg.immutable.IntAxisAlignedBox
import java.lang.ArrayIndexOutOfBoundsException
import com.extollit.collect.ArrayIterable
import com.extollit.num.FloatRange
import com.extollit.gaming.ai.path.IConfigModel
import java.lang.NullPointerException
import java.lang.UnsupportedOperationException
import com.extollit.collect.CollectionsExt
import com.extollit.linalg.immutable.VertexOffset
import com.extollit.gaming.ai.path.model.OcclusionField.AreaInit
import java.util.LinkedList
import java.util.Collections
import java.lang.IllegalStateException
import java.util.HashSet
import java.util.Deque
import com.extollit.gaming.ai.path.model.TreeTransitional.RotateNodeOp
import com.extollit.gaming.ai.path.SchedulingPriority
import com.extollit.gaming.ai.path.IConfigModel.Schedule
import com.extollit.gaming.ai.path.PassibilityHelpers
import java.lang.IllegalArgumentException
import com.extollit.gaming.ai.path.AreaOcclusionProviderFactory
import com.extollit.gaming.ai.path.HydrazinePathFinder
import com.extollit.gaming.ai.path.FluidicNodeCalculator
import com.extollit.gaming.ai.path.GroundNodeCalculator
import com.extollit.gaming.ai.path.AbstractNodeCalculator
import com.extollit.gaming.ai.path.model.*
import java.lang.Math
import com.extollit.linalg.immutable.Vec3i

internal class GroundNodeCalculator(instanceSpace: IInstanceSpace) : AbstractNodeCalculator(instanceSpace) {
    override fun passibleNodeNear(coordinates0: Vec3i, origin: Vec3i?, flagSampler: FlagSampler): Node {
        val point: Node
        val capabilities = capabilities
        val x0 = coordinates0.x
        val y0 = coordinates0.y
        val z0 = coordinates0.z
        val delta: Vec3i
        delta = if (origin != null) coordinates0.subOf(origin) else Vec3i.ZERO
        val hasOrigin = delta !== Vec3i.ZERO && delta != Vec3i.ZERO
        val climbsLadders = this.capabilities!!.climber()
        var passibility: Passibility? = Passibility.passible
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
                if (PassibilityHelpers.impedesMovement(flags, capabilities).also { impedesMovement = it }) {
                    val partialDisparity = partY - topOffsetAt(flags, x, y++, z)
                    flags = flagSampler.flagsAt(x, y, z)
                    if (partialDisparity < 0 || PassibilityHelpers.impedesMovement(flags, capabilities)) {
                        if (!hasOrigin) return Node(coordinates0, Passibility.impassible, flagSampler.volatility() > 0)
                        if (delta.x * delta.x + delta.z * delta.z <= 1) {
                            y -= delta.y + 1
                            do flags = flagSampler.flagsAt(
                                x - delta.x,
                                y++,
                                z - delta.z
                            ) while (climbsLadders && Logic.Companion.climbable(flags))
                        }
                        if (PassibilityHelpers.impedesMovement(
                                flagSampler.flagsAt(x, --y, z).also { flags = it },
                                capabilities
                            ) && (PassibilityHelpers.impedesMovement(
                                flagSampler.flagsAt(x, ++y, z).also { flags = it },
                                capabilities
                            ) || partY < 0)
                        ) return Node(coordinates0, Passibility.impassible, flagSampler.volatility() > 0)
                    }
                }
                partY = topOffsetAt(flagSampler, x, y - 1, z)
                val ys: Int
                passibility =
                    verticalClearanceAt(flagSampler, tall, flags, passibility, delta, x, y.also { ys = it }, z, partY)
                var swimable = false
                run {
                    var condition = !impedesMovement || unstable(flags)
                    var j = 0
                    while (condition && !swimable(flags).also { swimable = it } && j <= MAX_SURVIVE_FALL_DISTANCE) {
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
                passibility = passibility!!.between(
                    PassibilityHelpers.passibilityFrom(
                        flagSampler.flagsAt(x, y, z),
                        capabilities
                    )
                )
                if (passibility.impassible(capabilities)) return Node(
                    coordinates0,
                    Passibility.impassible,
                    flagSampler.volatility() > 0
                )
                ++z
            }
            ++x
        }
        if (hasOrigin && !passibility!!.impassible(capabilities)) passibility =
            originHeadClearance(flagSampler, passibility, origin!!, minY, minPartY)
        passibility = fallingSafety(passibility, y0, minY)
        if (passibility!!.impassible(capabilities)) passibility = Passibility.impassible
        point = Node(Vec3i(x0, minY + Math.round(minPartY), z0))
        point.passibility(passibility)
        point.volatile_(flagSampler.volatility() > 0)
        return point
    }

    override fun omnidirectional(): Boolean {
        return false
    }

    private fun fallingSafety(passibility: Passibility?, y0: Int, minY: Int): Passibility? {
        var passibility = passibility
        val dy = y0 - minY
        if (dy > 1) passibility = passibility!!.between(
            if (dy > MAX_SAFE_FALL_DISTANCE) Passibility.dangerous else Passibility.risky
        )
        return passibility
    }

    private fun swimable(flags: Byte): Boolean {
        return capabilities!!.swimmer() && AbstractNodeCalculator.Companion.swimmingRequiredFor(flags) && (Element.water.`in`(
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

        private fun unstable(flags: Byte): Boolean {
            return !Element.earth.`in`(flags) || Logic.ladder.`in`(flags)
        }
    }
}