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
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Passibility of blocks is stored in nibbles by an occlusion field.  The lower two bits of this nibble indicates
 * the material and basic passibility of the block.  This is determined from the flags on [IBlockDescription].
 *
 * @see IBlockDescription
 *
 * @see Logic
 */
enum class Element {
    /**
     * Represents a passible block having no collision bounds.
     * A block description that is not impeding, a liquid or incinerating produces an 'air' element.
     *
     * @see IBlockDescription.isImpeding
     * @see IBlockDescription.isLiquid
     * @see IBlockDescription.isIncinerating
     */
    air,

    /**
     * Represents an impassible block having at least some collision bounds.
     * A block description that is impeding produces an 'earth' element with the following additional conditions:
     * - If the block is a door, [IBlockObject.isImpeding] is called to determine dynamic collision bounds
     * from the specific block's state inside the instance.  If the door is open then the element 'air' results
     * instead.
     * - Climable blocks with no collision bounds (i.e. vines and ladders) also produce an 'earth' element.  This is
     * a special provision due to bit restrictions.  This is acceptable because something that has collision bounds
     * cannot also be a climbable ladder or vine due to how these work in the Notchian implementation.
     *
     * @see Logic.climbable
     * @see IBlockObject.isImpeding
     * @see IBlockDescription.isDoor
     * @see .air
     */
    earth,

    /**
     * Represents a fluid block having no collision bounds but can potentially drown an entity or requires swimming.
     * This is used only for non-incinerating fluids such as water, quicksand or mud.  Lava or magma is expressed
     * differently.
     *
     * A block description that is liquid and not incinerating produces a 'water' element.
     *
     * @see IBlockDescription.isLiquid
     * @see IBlockDescription.isIncinerating
     */
    water,

    /**
     * Represents a block that can burn entities that pass through it (if they are not fire resistant).  This is also
     * used to represent lava or magma blocks with the additional [Logic.fuzzy] flag.
     *
     * A block description that is incinerating produces a 'fire' element.
     *
     * @see IBlockDescription.isIncinerating
     * @see Logic.fuzzy
     */
    fire;

    @kotlin.jvm.JvmField
    val mask = ordinal.toByte()

    /**
     * Helper function for determining whether this element is represented by the specified nibble.  The nibble may
     * contain [Logic] flags in the high two bits, these will be ignored.
     *
     * @param flags A four-bit nibble containing a lower two bit element representation
     *
     * @return true if the lower two bits map to this element
     */
    fun `in`(flags: Byte): Boolean {
        return flags and MASK.toByte() == mask
    }

    /**
     * Helper function for setting the lower two bits of the specified nibble to represent this element.  The nibble
     * may contain [Logic] flags in the high two bits, these bits will not be affected.
     *
     * @param flags A four-bit nibble that will be modified to represent this element
     * @return the passed-in flags modified to represent this element
     */
    fun to(flags: Byte): Byte {
        return (flags and MASK.inv().toByte() or mask)
    }

    companion object {
        const val MASK = 4 - 1

        /**
         * Helper function for determining the element represented by the specified nibble.  The nibble may contain
         * [Logic] flags in the high two bits, these will be ignored.
         *
         * @param flags A four-bit nibble containing a lower two bit element representation
         *
         * @return the element represented by the nibble
         */
        fun of(flags: Byte): Element {
            return values()[(flags and MASK.toByte()).toInt()]
        }
    }
}