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

class AreaOcclusionProvider(
    private val columnarSpaces: Array<Array<IColumnarSpace?>>,
    private val cx0: Int,
    private val cz0: Int
) : IOcclusionProvider {
    private val cxN: Int
    private val czN: Int
    override fun elementAt(x: Int, y: Int, z: Int): Byte {
        val columnarSpaces = columnarSpaces
        val cx = x shr 4
        val cz = z shr 4
        val cy = y shr 4
        if (cx >= cx0 && cx <= cxN && cz >= cz0 && cz <= czN && cy >= 0 && cy < OcclusionField.Companion.DIMENSION_SIZE) {
            val czz = cz - cz0
            val cxx = cx - cx0
            val columnarSpace = columnarSpaces[czz][cxx]
            if (columnarSpace != null) {
                val field = columnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz)
                if (!field!!.areaInitFull()) areaInit(field, x, y, z)
                return field.elementAt(
                    x and OcclusionField.Companion.DIMENSION_MASK,
                    y and OcclusionField.Companion.DIMENSION_MASK,
                    z and OcclusionField.Companion.DIMENSION_MASK
                )
            }
        }
        return 0
    }

    private fun areaInit(field: OcclusionField?, x: Int, y: Int, z: Int) {
        val columnarSpaces = columnarSpaces
        val cx = x shr 4
        val cy = y shr 4
        val cz = z shr 4
        val cxN: Int = columnarSpaces[0].length - 1
        val czN = columnarSpaces.size - 1
        val czz = cz - cz0
        val cxx = cx - cx0
        val xx = x and OcclusionField.Companion.DIMENSION_MASK
        val yy = y and OcclusionField.Companion.DIMENSION_MASK
        val zz = z and OcclusionField.Companion.DIMENSION_MASK
        val centerColumnarSpace = columnarSpaces[czz][cxx]
        if (xx == 0 && zz == 0 && !field!!.areaInitAt(AreaInit.northWest) && cxx > 0 && czz > 0) {
            val westColumnarSpace = columnarSpaces[czz - 1][cxx]
            val northColumnarSpace = columnarSpaces[czz][cxx - 1]
            if (northColumnarSpace != null && westColumnarSpace != null) field.areaInitNorthWest(
                northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz),
                westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
            )
        } else if (xx == OcclusionField.Companion.DIMENSION_EXTENT && zz == 0 && !field!!.areaInitAt(AreaInit.northEast) && cxx < cxN && czz > 0) {
            val westColumnarSpace = columnarSpaces[czz - 1][cxx]
            val eastColumnarSpace = columnarSpaces[czz][cxx + 1]
            if (eastColumnarSpace != null && westColumnarSpace != null) field.areaInitNorthEast(
                eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz),
                westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
            )
        } else if (xx == 0 && zz == OcclusionField.Companion.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.southWest) && cxx > 0 && czz < czN) {
            val northColumnarSpace = columnarSpaces[czz][cxx - 1]
            val southColumnarSpace = columnarSpaces[czz + 1][cxx]
            if (northColumnarSpace != null && southColumnarSpace != null) field.areaInitSouthWest(
                northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz),
                southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
            )
        } else if (xx == OcclusionField.Companion.DIMENSION_EXTENT && zz == OcclusionField.Companion.DIMENSION_EXTENT && !field!!.areaInitAt(
                AreaInit.southEast
            ) && cxx < cxN && czz < czN
        ) {
            val eastColumnarSpace = columnarSpaces[czz][cxx + 1]
            val southColumnarSpace = columnarSpaces[czz + 1][cxx]
            if (eastColumnarSpace != null && southColumnarSpace != null) field.areaInitSouthEast(
                eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz),
                southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
            )
        } else if (xx == 0 && !field!!.areaInitAt(AreaInit.west) && cxx > 0) {
            val northColumnarSpace = columnarSpaces[czz][cxx - 1]
            if (northColumnarSpace != null) field.areaInitWest(
                northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz)
            )
        } else if (xx == OcclusionField.Companion.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.east) && cxx < cxN) {
            val eastColumnarSpace = columnarSpaces[czz][cxx + 1]
            if (eastColumnarSpace != null) field.areaInitEast(
                eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz)
            )
        } else if (zz == 0 && !field!!.areaInitAt(AreaInit.north) && czz > 0) {
            val westColumnarSpace = columnarSpaces[czz - 1][cxx]
            if (westColumnarSpace != null) field.areaInitNorth(
                westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
            )
        } else if (zz == OcclusionField.Companion.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.south) && czz < czN) {
            val southColumnarSpace = columnarSpaces[czz + 1][cxx]
            if (southColumnarSpace != null) field.areaInitSouth(
                southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
            )
        }
        if (yy == OcclusionField.Companion.DIMENSION_EXTENT && !field!!.areaInitAt(AreaInit.up)) {
            field.areaInitUp(
                centerColumnarSpace,
                cy,
                if (cy < OcclusionField.Companion.DIMENSION_EXTENT) centerColumnarSpace!!.occlusionFields()
                    .occlusionFieldAt(cx, cy + 1, cz) else null
            )
        } else if (yy == 0 && !field!!.areaInitAt(AreaInit.down)) {
            field.areaInitDown(
                centerColumnarSpace,
                cy,
                if (cy > 0) centerColumnarSpace!!.occlusionFields().occlusionFieldAt(cx, cy - 1, cz) else null
            )
        }
    }

    override fun visualizeAt(y: Int): String {
        return OcclusionField.Companion.visualizeAt(this, y, cx0 shl 4, cz0 shl 4, cxN + 1 shl 4, czN + 1 shl 4)
    }

    init {
        cxN = columnarSpaces[0].length + cx0 - 1
        czN = columnarSpaces.size + cz0 - 1
    }
}