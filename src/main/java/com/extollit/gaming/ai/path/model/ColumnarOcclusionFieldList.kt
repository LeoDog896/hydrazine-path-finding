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
 * A one-dimensional store for occlusion fields aligned along the y-axis distributed according to chunk coordinates.
 * Define an object of this class as a final field member of your concrete implementation of [IColumnarSpace] then
 * implement [IColumnarSpace.occlusionFields] to return this object.
 *
 * Additionally, some members of this type must be called upon certain server events.  Methods that the implementor should
 * be aware of are documented whereas methods below that remain undocumented are for internal use.
 *
 * @see IColumnarSpace
 *
 * @see OcclusionField
 */
open class ColumnarOcclusionFieldList
/**
 * Construct a new object bound to the specified columnar space container, this is what [.container] will be
 * bound to in a one-to-one relationship.
 *
 * @param container columnar space that owns this object
 */(
    /**
     * Containing columnar space that owns this object.  This object will typically have a final field member that points
     * to this object and returns it via [IColumnarSpace.occlusionFields]
     */
    val container: IColumnarSpace
) {
    private var fields: Array<OcclusionField?>? = null

    /**
     * Completely erases all data in this object, this must be called by the implementor prior to loading a chunk or
     * unloading a chunk to prevent stale state creep.  This effectively forces lazy-reinitialization of the occlusion
     * field cache.
     */
    fun reset() {
        fields = null
    }

    /**
     * Notifies the occlusion field cache that a block in the containing columnar space has changed (i.e has been added,
     * removed, changed type, or has had its meta-data changed).  The implementor must call this method whenever this
     * change occurs in the server, it causes the associated occlusion field data to update accordingly.
     *
     * This method's coordinate parameters are exceptional because they are absolute (relative to the instance) rather
     * than relative (to the parent columnar space)
     *
     * @param x absolute (relative to the instance space, not the columnar space) x coordinate of the block that changed
     * @param y absolute (relative to the instance space, not the columnar space) y coordinate of the block that changed
     * @param z absolute (relative to the instance space, not the columnar space) z coordinate of the block that changed
     * @param description description of the new block replacing what existed previously
     * @param metaData meta-data for the new block replacing what existed previously
     */
    fun onBlockChanged(x: Int, y: Int, z: Int, description: IBlockDescription?, metaData: Int) {
        if (fields == null) return
        val field = fields!![y shr 4 and 0xF] ?: return
        field[container, x, y, z] = description
    }

    fun occlusionFieldAt(cx: Int, cy: Int, cz: Int): OcclusionField {
        if (fields == null) fields = arrayOfNulls(OcclusionField.DIMENSION_SIZE)
        val result = fields!![cy]
        return if (result != null) {
            result
        } else {
            val occlusionField = createOcclusionField(cx, cy, cz)
            occlusionField.also { fields!![cy] = it }
        }
    }

    fun optionalOcclusionFieldAt(cy: Int): OcclusionField? {
        return if (fields == null) null else fields!![cy]
    }

    protected open fun createOcclusionField(cx: Int, cy: Int, cz: Int): OcclusionField {
        val occlusionField = OcclusionField()
        occlusionField.loadFrom(container, cx, cy, cz)
        return occlusionField
    }

    companion object {
        fun optionalOcclusionFieldAt(instance: IInstanceSpace?, cx: Int, cy: Int, cz: Int): OcclusionField? {
            val columnarSpace = instance!!.columnarSpaceAt(cx, cz) ?: return null
            return columnarSpace.occlusionFields().optionalOcclusionFieldAt(cy)
        }
    }
}