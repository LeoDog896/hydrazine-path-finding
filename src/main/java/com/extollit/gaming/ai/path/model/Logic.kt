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
 * Passibility of blocks is stored in nibbles by an occlusion field.  The higher two bits of this nibble supplement
 * the lower two bits by refining the nature of the [Element] expressed by the lower two bits for passibility.
 *
 * @see Element
 */
enum class Logic {
    /**
     * No special information that refines the nature of the associated element.
     */
    nothing,

    /**
     * This indicates, in general, that the associated element isn't precisely what it seems and additional calculations
     * and queries may be necessary to determine the passibility of a block represented by the associated nibble.
     *
     * Below describes the meaning of this flag applied to each element:
     *
     * - [Element.fire] - The block is flames, without this flag the block is lava
     * - [Element.earth] - The block does not have full collision bounds and possibly has dynamic collision bounds
     * - [Element.air] - The block is openly passible (but not an open door) and there is at least one block in its
     * Von Neumann neighborhood that is dissimilar and not also fuzzy
     * - [Element.water] - Reserved for future use
     *
     * @see IBlockDescription.isFullyBounded
     */
    fuzzy,

    /**
     * This indicates that the block is climbable (i.e. either a ladder or vine) and resides adjacent to a solid block
     * in its Von Neumann neighborhood that supports it (e.g. free-hanging vines cannot be climbed, they must be up
     * against something solid).
     *
     * Presently this flag only applies to the [Element.earth] element.  Pairing this flag with any of the other
     * three elements is reserved for future use.
     *
     * @see IBlockDescription.isClimbable
     */
    ladder,

    /**
     * Indicates that the block is a door that is either open or closed.  This requires performing an additional query on
     * the particular block inside the instance to determine whether the door is open or closed from its dynamic state.
     *
     * Below describes the meaning of this flag applied to each element:
     *
     * - [Element.earth] - The door is closed
     * - [Element.air] - The door is open
     * - [Element.fire] - Reserved for future use
     * - [Element.water] - Reserved for future use
     *
     * @see IBlockDescription.isDoor
     */
    doorway;

    @kotlin.jvm.JvmField
    val mask = (ordinal shl BIT_OFFSET).toByte()

    /**
     * Helper function for determining whether this logic indicator is represented by the specified nibble.
     * The nibble may contain [Element] flags in the low two bits, these will be ignored.
     *
     * @param flags A four-bit nibble containing a higher two-bit logic representation
     *
     * @return true if the higher two bits map to this logic indicator
     */
    fun `in`(flags: Byte): Boolean {
        return flags and (MASK shl BIT_OFFSET) == mask.toInt()
    }

    /**
     * Helper function for setting the higher two bits of the specified nibble to represent this logic indicator.
     * The nibble may contain [Element] flags in the low two bits, these bits will not be affected.
     *
     * @param flags A four-bit nibble that will be modified to represent this logic indicator
     * @return the passed-in flags modified to represent this logic indicator
     */
    fun to(flags: Byte): Byte {
        return (flags and (MASK shl BIT_OFFSET).inv() or mask)
    }

    companion object {
        const val BIT_OFFSET = 2
        const val MASK = 4 - 1

        /**
         * Helper function for determining the logic indicator represented by the specified nibble.  The nibble may contain
         * [Element] flags in the low two bits, these will be ignored.
         *
         * @param flags A four-bit nibble containing a higher two-bit logic representation
         *
         * @return the logic indicator represented by the nibble
         */
        fun of(flags: Byte): Logic {
            return values()[flags and (MASK shl BIT_OFFSET) shr BIT_OFFSET]
        }

        /**
         * Helper function for determining whether the entire nibble (both high and low sets of bit pairs) represents a
         * climbable ladder (i.e. vines or ladder).
         *
         * @param flags A four-bit nibble that contains a high two-bit logic representation and a low two-bit element
         * representation.
         * @return whether the flags map to both [.ladder] and [Element.earth]
         */
        @kotlin.jvm.JvmStatic
        fun climbable(flags: Byte): Boolean {
            return ladder.`in`(flags) && Element.earth.`in`(flags)
        }
    }
}