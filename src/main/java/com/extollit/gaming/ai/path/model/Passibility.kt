package com.extollit.gaming.ai.path.model

/**
 * Expresses ratings for traversal into a particular path-point according to increasing risk.
 *
 * This is used to rate path points visited during A* triage according to the type of block.
 */
enum class Passibility {
    /**
     * Pristine, fully-passible, no risk to the entity
     */
    Passible,

    /**
     * Mild risk pathing into this point, it could be close to lava or through water.
     */
    Risky,

    /**
     * High risk pathing into this point, these points are usually over cliffs or on-fire
     */
    Dangerous,

    /**
     * Impossible (or completely impractical) pathing into this point, usually impeded by collision bounds of
     * the block.  This also applies to lava since the chances of survival pathing through even one block of
     * lava (when not fire-resistant) is effectively zero.
     */
    impassible;

    /**
     * Renders the least passibility between this and the given passibility rating.  For example, if this is
     * [.passible] and the parameter is [.risky] then the result is *risky*.  Also, if this
     * is [.dangerous] and the parameter is [.risky] then the result is *dangerous*.
     *
     * @param other other passibility to compare to
     * @return the lesser of the two passibility ratings
     */
    fun between(other: Passibility): Passibility = values()[ordinal.coerceAtLeast(other.ordinal)]

    /**
     * Determines if the given passibility rating is better than this one.  For example, if this is [.dangerous]
     * and the parameter is [.risky] then the result is *risky*.
     *
     * @param other other rating to compare with as potentially better than this one
     * @return true if the given passibility rating is better than this one, false otherwise
     */
    fun betterThan(other: Passibility): Boolean = ordinal < other.ordinal

    /**
     * Determines if the given passibility rating is worse than this one.  For example, if this is [.dangerous]
     * and the parameter is [.risky] then the result is *dangerous*.
     *
     * @param other other rating to compare with as potentially worse than this one
     * @return true if the given passibility rating is worse than this one, false otherwise
     */
    fun worseThan(other: Passibility): Boolean = ordinal > other.ordinal

    /**
     * Determines if the entity should path to a node rated this way.  This may return false even if the location rated
     * this way does not physically impede or even harm the entity, it depends on the capabilities of the entity.
     *
     * @param capabilities capabilities of the entity, used to determine if the entity is a cautious path-finder or not
     * @return true if the entity can path to this rating, false if it should not
     */
    fun impassible(capabilities: IPathingEntity.Capabilities?): Boolean {
        return (this == impassible
                || capabilities!!.cautious() && worseThan(Passible))
    }
}