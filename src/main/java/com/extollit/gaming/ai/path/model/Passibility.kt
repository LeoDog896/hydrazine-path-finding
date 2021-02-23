package com.extollit.gaming.ai.path.model

import com.extollit.gaming.ai.path.model.INode
import com.extollit.gaming.ai.path.model.NodeLinkedList
import kotlin.jvm.JvmOverloads
import com.extollit.gaming.ai.path.model.Passibility
import com.extollit.gaming.ai.path.model.Gravitation
import java.lang.StringBuilder
import java.text.MessageFormat
import java.util.Objects
import com.extollit.gaming.ai.path.model.IPath
import com.extollit.gaming.ai.path.model.IPathingEntity
import com.extollit.gaming.ai.path.model.Logic
import com.extollit.gaming.ai.path.model.IInstanceSpace
import com.extollit.gaming.ai.path.model.INodeCalculator
import com.extollit.gaming.ai.path.model.IOcclusionProviderFactory
import com.extollit.collect.SparseSpatialMap
import com.extollit.gaming.ai.path.model.IGraphNodeFilter
import com.extollit.gaming.ai.path.model.IOcclusionProvider
import com.extollit.gaming.ai.path.model.SortedNodeQueue
import com.extollit.linalg.immutable.IntAxisAlignedBox
import com.extollit.gaming.ai.path.model.FlagSampler
import java.lang.ArrayIndexOutOfBoundsException
import com.extollit.collect.ArrayIterable
import com.extollit.gaming.ai.path.model.PathObject
import com.extollit.num.FloatRange
import com.extollit.gaming.ai.path.IConfigModel
import com.extollit.gaming.ai.path.model.IncompletePath
import com.extollit.gaming.ai.path.model.IBlockDescription
import com.extollit.gaming.ai.path.model.ColumnarOcclusionFieldList
import com.extollit.gaming.ai.path.model.IBlockObject
import com.extollit.gaming.ai.path.model.IColumnarSpace
import com.extollit.gaming.ai.path.model.IDynamicMovableObject
import java.lang.NullPointerException
import java.lang.UnsupportedOperationException
import com.extollit.collect.CollectionsExt
import com.extollit.linalg.immutable.VertexOffset
import com.extollit.gaming.ai.path.model.OcclusionField.AreaInit
import com.extollit.gaming.ai.path.model.OcclusionField
import com.extollit.gaming.ai.path.model.TreeTransitional
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
import com.extollit.gaming.ai.path.model.IPathProcessor
import com.extollit.gaming.ai.path.AreaOcclusionProviderFactory
import com.extollit.gaming.ai.path.HydrazinePathFinder
import com.extollit.gaming.ai.path.FluidicNodeCalculator
import com.extollit.gaming.ai.path.GroundNodeCalculator
import com.extollit.gaming.ai.path.AbstractNodeCalculator
import java.lang.Math
import com.extollit.gaming.ai.path.model.AreaOcclusionProvider

/**
 * Expresses ratings for traversal into a particular path-point according to increasing risk.
 *
 * This is used to rate path points visited during A* triage according to the type of block.
 */
enum class Passibility {
    /**
     * Pristine, fully-passible, no risk to the entity
     */
    passible,

    /**
     * Mild risk pathing into this point, it could be close to lava or through water.
     */
    risky,

    /**
     * High risk pathing into this point, these points are usually over cliffs or on-fire
     */
    dangerous,

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
    fun between(other: Passibility?): Passibility {
        return values()[Math.max(ordinal, other!!.ordinal)]
    }

    /**
     * Determines if the given passibility rating is better than this one.  For example, if this is [.dangerous]
     * and the parameter is [.risky] then the result is *risky*.
     *
     * @param other other rating to compare with as potentially better than this one
     * @return true if the given passibility rating is better than this one, false otherwise
     */
    fun betterThan(other: Passibility?): Boolean {
        return ordinal < other!!.ordinal
    }

    /**
     * Determines if the given passibility rating is worse than this one.  For example, if this is [.dangerous]
     * and the parameter is [.risky] then the result is *dangerous*.
     *
     * @param other other rating to compare with as potentially worse than this one
     * @return true if the given passibility rating is worse than this one, false otherwise
     */
    fun worseThan(other: Passibility?): Boolean {
        return ordinal > other!!.ordinal
    }

    /**
     * Determines if the entity should path to a node rated this way.  This may return false even if the location rated
     * this way does not physically impede or even harm the entity, it depends on the capabilities of the entity.
     *
     * @param capabilities capabilities of the entity, used to determine if the entity is a cautious path-finder or not
     * @return true if the entity can path to this rating, false if it should not
     */
    fun impassible(capabilities: IPathingEntity.Capabilities?): Boolean {
        return (this == impassible
                || capabilities!!.cautious() && worseThan(passible))
    }
}