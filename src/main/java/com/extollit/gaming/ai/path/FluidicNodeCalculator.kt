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

internal class FluidicNodeCalculator(instanceSpace: IInstanceSpace) : AbstractNodeCalculator(instanceSpace),
    INodeCalculator {
    override fun passibleNodeNear(coords0: Vec3i, origin: Vec3i?, flagSampler: FlagSampler): Node {
        val point: Node
        val capabilities = capabilities
        val x0 = coords0.x
        val y0 = coords0.y
        val z0 = coords0.z
        val d: Vec3i
        d = if (origin != null) coords0.subOf(origin) else Vec3i.ZERO
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
                        flagSampler.volatility() > 0,
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
                    flagSampler.volatility() > 0,
                    gravitation
                )
                ++z
            }
            ++x
        }
        if (passibility!!.impassible(capabilities)) passibility =
            Passibility.impassible else if (hasOrigin) passibility =
            originHeadClearance(flagSampler, passibility, origin!!, minY, minPartY)
        point = Node(Vec3i(x0, minY + Math.round(minPartY), z0))
        point.passibility(passibility)
        point.gravitation(gravitation)
        point.volatile_(flagSampler.volatility() > 0)
        return point
    }

    override fun omnidirectional(): Boolean {
        return true
    }
}