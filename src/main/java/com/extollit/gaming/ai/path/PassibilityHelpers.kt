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

internal object PassibilityHelpers {
    fun impedesMovement(flags: Byte, capabilities: IPathingEntity.Capabilities?): Boolean {
        return (Element.earth.`in`(flags) && !passibleDoorway(flags, capabilities) && !Logic.ladder.`in`(flags)
                || Element.air.`in`(flags) && impassibleDoorway(flags, capabilities))
    }

    @kotlin.jvm.JvmStatic
    fun clearance(flags: Byte, capabilities: IPathingEntity.Capabilities?): Passibility {
        return if (Element.earth.`in`(flags)) if (Logic.ladder.`in`(flags) || passibleDoorway(
                flags,
                capabilities
            )
        ) Passibility.passible else if (Logic.fuzzy.`in`(flags)) Passibility.risky else Passibility.impassible else if (Element.water.`in`(
                flags
            )
        ) {
            if (capabilities!!.fireResistant()) Passibility.dangerous else if (capabilities.aquatic() && capabilities.swimmer()) Passibility.passible else Passibility.risky
        } else if (Element.fire.`in`(flags)) if (capabilities!!.fireResistant()) Passibility.risky else Passibility.dangerous else if (impassibleDoorway(
                flags,
                capabilities
            )
        ) Passibility.impassible else if (capabilities!!.aquatic()) Passibility.risky else Passibility.passible
    }

    @kotlin.jvm.JvmStatic
    fun passibilityFrom(flags: Byte, capabilities: IPathingEntity.Capabilities?): Passibility {
        if (impassibleDoorway(flags, capabilities)) return Passibility.impassible
        val kind: Element = Element.Companion.of(flags)
        return when (kind) {
            Element.earth -> if (Logic.ladder.`in`(flags) || passibleDoorway(
                    flags,
                    capabilities
                )
            ) Passibility.passible else Passibility.impassible
            Element.air -> if (capabilities!!.aquatic()) Passibility.dangerous else Passibility.passible
            Element.water -> {
                val gilled = capabilities!!.aquatic()
                if (capabilities.aquaphobic()) Passibility.dangerous else if (gilled && capabilities.swimmer()) Passibility.passible else Passibility.risky
            }
            Element.fire -> if (!capabilities!!.fireResistant()) Passibility.dangerous else Passibility.risky
        }
        throw IllegalArgumentException(MessageFormat.format("Unhandled element type ''{0}''", kind))
    }

    fun gravitationFrom(flags: Byte): Gravitation {
        if (Element.earth.`in`(flags)) return Gravitation.grounded
        if (Element.water.`in`(flags)) return Gravitation.buoyant
        return if (Element.air.`in`(flags)) Gravitation.airborne else Gravitation.grounded
    }

    private fun passibleDoorway(flags: Byte, capabilities: IPathingEntity.Capabilities?): Boolean {
        return Logic.doorway.`in`(flags) && capabilities!!.opensDoors() && !capabilities.avoidsDoorways()
    }

    private fun impassibleDoorway(flags: Byte, capabilities: IPathingEntity.Capabilities?): Boolean {
        return Logic.doorway.`in`(flags) && capabilities!!.avoidsDoorways()
    }
}