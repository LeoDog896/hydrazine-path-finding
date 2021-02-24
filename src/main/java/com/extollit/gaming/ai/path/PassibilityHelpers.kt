package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.*

internal object PassibilityHelpers {

    fun impedesMovement(flags: Byte, capabilities: IPathingEntity.Capabilities?): Boolean {
        return (Element.earth.flagsIn(flags) && !passibleDoorway(flags, capabilities) && !Logic.ladder.flagsIn(flags)
                || Element.air.flagsIn(flags) && impassibleDoorway(flags, capabilities))
    }

    @kotlin.jvm.JvmStatic
    fun clearance(flags: Byte, capabilities: IPathingEntity.Capabilities?): Passibility {
        return if (Element.earth.flagsIn(flags)) if (Logic.ladder.flagsIn(flags) || passibleDoorway(
                flags,
                capabilities
            )
        ) Passibility.Passible else if (Logic.fuzzy.flagsIn(flags)) Passibility.Risky else Passibility.impassible else if (Element.water.flagsIn(
                flags
            )
        ) {
            if (capabilities!!.fireResistant()) Passibility.Dangerous else if (capabilities.aquatic() && capabilities.swimmer()) Passibility.Passible else Passibility.Risky
        } else if (Element.fire.flagsIn(flags)) if (capabilities!!.fireResistant()) Passibility.Risky else Passibility.Dangerous else if (impassibleDoorway(
                flags,
                capabilities
            )
        ) Passibility.impassible else if (capabilities!!.aquatic()) Passibility.Risky else Passibility.Passible
    }

    @JvmStatic
    fun passibilityFrom(flags: Byte, capabilities: IPathingEntity.Capabilities?): Passibility {
        if (impassibleDoorway(flags, capabilities)) return Passibility.impassible
        return when (Element.of(flags)) {
            Element.earth -> if (Logic.ladder.flagsIn(flags) || passibleDoorway(
                    flags,
                    capabilities
                )
            ) Passibility.Passible else Passibility.impassible
            Element.air -> if (capabilities!!.aquatic()) Passibility.Dangerous else Passibility.Passible
            Element.water -> {
                val gilled = capabilities!!.aquatic()
                if (capabilities.aquaphobic()) Passibility.Dangerous else if (gilled && capabilities.swimmer()) Passibility.Passible else Passibility.Risky
            }
            Element.fire -> if (!capabilities!!.fireResistant()) Passibility.Dangerous else Passibility.Risky
        }
    }

    fun gravitationFrom(flags: Byte): Gravitation {
        if (Element.earth.flagsIn(flags)) return Gravitation.grounded
        if (Element.water.flagsIn(flags)) return Gravitation.buoyant
        return if (Element.air.flagsIn(flags)) Gravitation.airborne else Gravitation.grounded
    }

    private fun passibleDoorway(flags: Byte, capabilities: IPathingEntity.Capabilities?): Boolean = Logic.doorway.flagsIn(flags) && capabilities!!.opensDoors() && !capabilities.avoidsDoorways()

    private fun impassibleDoorway(flags: Byte, capabilities: IPathingEntity.Capabilities?): Boolean = Logic.doorway.flagsIn(flags) && capabilities!!.avoidsDoorways()
}