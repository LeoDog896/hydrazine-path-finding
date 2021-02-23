package com.extollit.gaming.ai.path.model

/**
 * For traversing a path to a particular point, this indicates whether an entity should walk, fly or swim to the point
 */
enum class Gravitation {
    /**
     * The associated point is on the ground
     */
    grounded,

    /**
     * The associated point is in fluid and not on the ground, a change in buoyancy is required to traverse to this point
     */
    buoyant,

    /**
     * The associated point is in the air and the entity must fly to it
     */
    airborne;

    /**
     * Determines the greatest gravitation restriction between this and the passed parameter.  For example, if
     * this is [.buoyant] and the parameter is [.grounded] then the result is *grounded*.  Also,
     * if this is [.airborne] and the parameter is [.buoyant] then the result is *buoyant*.
     *
     * @param other the other gravitation rating to compare with this one
     * @return the more restrictive gravitation between this and the parameter
     */
    fun between(other: Gravitation?): Gravitation {
        return values()[ordinal.coerceAtMost(other!!.ordinal)]
    }
}