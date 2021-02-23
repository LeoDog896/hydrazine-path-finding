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
import com.extollit.linalg.immutable.Vec3i

class NodeMap(
    private val instanceSpace: IInstanceSpace,
    private var calculator: INodeCalculator?,
    private val occlusionProviderFactory: IOcclusionProviderFactory
) {
    private val internalMap = SparseSpatialMap<Node?>(3)
    private var filter: IGraphNodeFilter? = null
    private var occlusionProvider: IOcclusionProvider? = null
    private var centerX0 = 0
    private var centerXN = 0
    private var centerZ0 = 0
    private var centerZN = 0

    constructor(
        instanceSpace: IInstanceSpace,
        occlusionProviderFactory: IOcclusionProviderFactory
    ) : this(instanceSpace, null, occlusionProviderFactory)

    fun filter(filter: IGraphNodeFilter?) {
        this.filter = filter
        clear()
    }

    fun filter(): IGraphNodeFilter? {
        return filter
    }

    fun calculator(calculator: INodeCalculator?) {
        this.calculator = calculator
        clear()
    }

    fun reset(queue: SortedNodeQueue) {
        for (p in internalMap.values) p!!.rollback()
        queue.clear()
    }

    fun cullBranchAt(coordinates: Vec3i?, queue: SortedNodeQueue) {
        val node = internalMap[coordinates] ?: return
        val parent = node.parent()
        queue.cullBranch(node)
        if (parent != null && !parent.assigned()) {
            parent.visited(false)
            queue.add(parent)
        }
        internalMap.remove(coordinates)
    }

    fun reset() {
        clear()
        occlusionProvider = null
    }

    fun clear() {
        internalMap.clear()
    }

    fun needsOcclusionProvider(): Boolean {
        return occlusionProvider == null
    }

    fun flagsAt(x: Int, y: Int, z: Int): Byte {
        return occlusionProvider!!.elementAt(x, y, z)
    }

    fun updateFieldWindow(x0: Int, z0: Int, xN: Int, zN: Int, cull: Boolean) {
        val cx0 = x0 shr 4
        val cz0 = z0 shr 4
        val cxN = xN shr 4
        val czN = zN shr 4
        val aop = occlusionProvider
        val windowTest: Boolean
        windowTest =
            if (cull) cx0 != centerX0 || cz0 != centerZ0 || cxN != centerXN || czN != centerZN else cx0 < centerX0 || cz0 < centerZ0 || cxN > centerXN || czN > centerZN
        if (aop == null || windowTest) {
            occlusionProvider = occlusionProviderFactory.fromInstanceSpace(instanceSpace, cx0, cz0, cxN, czN)
            centerX0 = cx0
            centerZ0 = cz0
            centerXN = cxN
            centerZN = czN
            if (cull) cullOutside(x0, z0, xN, zN)
        }
    }

    fun all(): Collection<Node?> {
        return internalMap.values
    }

    fun cullOutside(x0: Int, z0: Int, xN: Int, zN: Int) {
        for (p in internalMap.cullOutside(
            IntAxisAlignedBox(
                x0,
                Int.MIN_VALUE,
                z0,
                xN,
                Int.MAX_VALUE,
                zN
            )
        )) p!!.rollback()
    }

    fun cachedPointAt(x: Int, y: Int, z: Int): Node {
        val coords = Vec3i(x, y, z)
        return cachedPointAt(coords)
    }

    fun cachedPointAt(coords: Vec3i): Node {
        var point = internalMap[coords]
        if (point == null) {
            point = passibleNodeNear(coords, null)
            if (point.key != coords) point = Node(coords, Passibility.impassible, false)
            internalMap[coords] = point
        }
        return point
    }

    fun cachedPassiblePointNear(x: Int, y: Int, z: Int): Node? {
        return cachedPassiblePointNear(x, y, z, null)
    }

    fun cachedPassiblePointNear(x0: Int, y0: Int, z0: Int, origin: Vec3i?): Node {
        val coords0 = Vec3i(x0, y0, z0)
        return cachedPassiblePointNear(coords0, origin)
    }

    fun cachedPassiblePointNear(coords: Vec3i, origin: Vec3i?): Node {
        val nodeMap = internalMap
        val point0 = nodeMap[coords]
        var point = point0
        if (point == null) point = passibleNodeNear(coords, origin) else if (point.volatile_()) {
            point = passibleNodeNear(coords, origin)
            if (point.key == point0!!.key) {
                point0.passibility(point.passibility())
                point0.volatile_(point.volatile_())
                point = point0
            } else point0.isolate()
        }
        if (coords != point.key) {
            val existing = nodeMap[point.key]
            if (existing == null) nodeMap[point.key] = point else point = existing
        }
        if (point !== point0) nodeMap[coords] = point
        return point
    }

    private fun passibleNodeNear(coordinates: Vec3i, origin: Vec3i?): Node {
        val node = calculator!!.passibleNodeNear(coordinates, origin, FlagSampler(occlusionProvider))
        val filter = filter
        if (filter != null) {
            val newPassibility = filter.mapPassibility(node)
            if (newPassibility != null) node.passibility(newPassibility)
        }
        return node
    }

    fun remove(x: Int, y: Int, z: Int): Boolean {
        return remove(Vec3i(x, y, z))
    }

    fun remove(coordinates: Vec3i?): Boolean {
        val existing = internalMap.remove(coordinates)
        if (existing != null) {
            existing.rollback()
            return true
        }
        return false
    }

    override fun toString(): String {
        return internalMap.toString()
    }

    override fun equals(`object`: Any?): Boolean {
        if (this === `object`) return true
        if (`object` == null || javaClass != `object`.javaClass) return false
        val nodeMap = `object` as NodeMap
        return internalMap == nodeMap.internalMap
    }

    override fun hashCode(): Int {
        return internalMap.hashCode()
    }
}