package com.extollit.gaming.ai.path.model

import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector

/**
 * Represents a node in an A* graph which is used by a path to move an entity between points
 *
 * @see PathObject
 */
interface INode {

    /**
     * Coordinates relative to the instance space of the path point
     */
    val coordinates: ThreeDimensionalIntVector

    /**
     * Passibility of this path point, which expresses how likely an entity can survive pathing to this point
     *
     * @see Passibility
     *
     * @return whether this entity can path to this point
     */
    fun passibility(): Passibility

    /**
     * Orientation about gravity for an entity to path to this point, whether the entity must walk, fly or swim to get
     * to this point
     *
     * @return the gravitation of the path point
     */
    fun gravitation(): Gravitation
}