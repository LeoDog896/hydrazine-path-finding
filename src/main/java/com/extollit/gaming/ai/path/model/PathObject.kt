package com.extollit.gaming.ai.path.model

import com.extollit.gaming.ai.path.model.INode
import com.extollit.gaming.ai.path.model.NodeLinkedList
import kotlin.jvm.JvmOverloads
import com.extollit.gaming.ai.path.model.Passibility
import com.extollit.gaming.ai.path.model.Gravitation
import java.lang.StringBuilder
import java.text.MessageFormat
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
import java.lang.IllegalStateException
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
import com.extollit.linalg.immutable.Vec3d
import com.extollit.linalg.immutable.Vec3i
import java.util.*

class PathObject protected constructor(speed: Float, random: Random?, vararg nodes: Node?) : IPath {
    @kotlin.jvm.JvmField
    val nodes: Array<Node>
    private val speed: Float
    private val random: Random?
    @kotlin.jvm.JvmField
    var i = 0
    private var taxiUntil = 0
    private var adjacentIndex = 0
    private var length: Int
    private var nextDirectLineTimeout: Float
    private var lastMutationTime = -1f

    internal constructor(speed: Float, vararg nodes: Node?) : this(speed, Random(), *nodes)

    override fun truncateTo(length: Int) {
        if (length < 0 || length >= nodes.size) throw ArrayIndexOutOfBoundsException(
            MessageFormat.format("Length is out of bounds 0 <= length < {0} but length = {1}", nodes.size, length)
        )
        this.length = length
    }

    override fun untruncate() {
        length = nodes.size
    }

    override fun iterator(): MutableIterator<INode> {
        return ArrayIterable.Iter<INode>(nodes, length)
    }

    override fun length(): Int {
        return length
    }

    override fun cursor(): Int {
        return i
    }

    override fun at(i: Int): INode {
        return nodes[i]
    }

    override fun current(): INode {
        return nodes[i]
    }

    override fun last(): INode? {
        val nodes = nodes
        val length = length
        return if (length > 0) nodes[length - 1] else null
    }

    override fun done(): Boolean {
        return i >= length
    }

    override fun update(subject: IPathingEntity) {
        var mutated = false
        try {
            if (done()) return
            val unlevelIndex: Int
            val capabilities = subject.capabilities()
            val grounded = !(capabilities!!.avian() || capabilities.aquatic() && capabilities.swimmer())
            val fy: Float
            if (grounded) {
                unlevelIndex = unlevelIndex(i, subject.coordinates())
                fy = 0f
            } else {
                unlevelIndex = length
                fy = 1f
            }
            val adjacentIndex0 = adjacentIndex
            val minDistanceSquared = updateNearestAdjacentIndex(subject, unlevelIndex, fy)
            val adjacentIndex = adjacentIndex
            var targetIndex = i
            if (minDistanceSquared <= PATHPOINT_SNAP_MARGIN_SQ) {
                var advanceTargetIndex: Int? = null
                targetIndex = adjacentIndex
                if (targetIndex >= taxiUntil && directLine(
                        targetIndex,
                        unlevelIndex,
                        grounded
                    ).also { advanceTargetIndex = it } > targetIndex
                ) targetIndex = advanceTargetIndex!! else targetIndex = adjacentIndex + 1
            } else if (minDistanceSquared > 0.5 || targetIndex < taxiUntil) targetIndex = adjacentIndex
            mutated = adjacentIndex > adjacentIndex0
            this.adjacentIndex = adjacentIndex
            i = Math.max(adjacentIndex, targetIndex)
            if (stagnantFor(subject) > nextDirectLineTimeout) {
                if (taxiUntil < adjacentIndex) taxiUntil = adjacentIndex + 1 else taxiUntil++
                nextDirectLineTimeout += DIRECT_LINE_TIME_LIMIT!!.next(random)
            }
            val node = if (done()) last() else current()
            node?.let { moveSubjectTo(subject, it) }
        } finally {
            if (mutated || lastMutationTime < 0) {
                lastMutationTime = subject.age() * speed
                if (nextDirectLineTimeout > DIRECT_LINE_TIME_LIMIT!!.max) nextDirectLineTimeout =
                    DIRECT_LINE_TIME_LIMIT!!.next(
                        random
                    )
            }
        }
    }

    private fun updateNearestAdjacentIndex(subject: IPathingEntity, unlevelIndex: Int, fy: Float): Double {
        var minDistanceSquared = Double.MAX_VALUE
        var nextAdjacentIndex: Int
        val currentPosition = subject.coordinates()
        val width = subject.width()
        val offset = pointToPositionOffset(width)
        val d = com.extollit.linalg.mutable.Vec3d(currentPosition)
        val end = unlevelIndex + 1
        run {
            var i = this.adjacentIndex.also { nextAdjacentIndex = it }
            while (i < this.length && i < end) {
                val node = this.nodes[i]
                val pp = node.key
                d.sub(pp)
                d.sub(offset.toDouble(), 0.0, offset.toDouble())
                d.y *= fy.toDouble()
                val distanceSquared = d.mg2()
                if (distanceSquared < minDistanceSquared) {
                    nextAdjacentIndex = i
                    minDistanceSquared = distanceSquared
                }
                d.set(currentPosition)
                ++i
            }
        }
        adjacentIndex = nextAdjacentIndex
        return minDistanceSquared
    }

    private fun moveSubjectTo(subject: IPathingEntity, pathPoint: INode) {
        val d = com.extollit.linalg.mutable.Vec3d(subject.coordinates())
        val position = positionFor(subject, pathPoint.coordinates())
        d.sub(position)
        if (d.mg2() > PATHPOINT_SNAP_MARGIN_SQ) subject.moveTo(
            position,
            pathPoint.passibility(),
            pathPoint.gravitation()
        )
    }

    override fun taxiing(): Boolean {
        return taxiUntil >= adjacentIndex
    }

    override fun taxiUntil(index: Int) {
        taxiUntil = index
    }

    override fun stagnantFor(pathingEntity: IPathingEntity): Float {
        return if (lastMutationTime < 0) 0f else pathingEntity.age() * speed - lastMutationTime
    }

    fun directLine(from: Int, until: Int, grounded: Boolean): Int {
        val xis = IntArray(4)
        val yis = IntArray(4)
        val zis = IntArray(4)
        var ii = 0
        var i = from
        var i0 = i
        var xi00 = 0
        var yi00 = 0
        var zi00 = 0
        var sq: Int
        var bdx = false
        var bdy = false
        var bdz = false
        val n = until - 1
        val nodes = nodes
        val node0 = nodes[i]
        var p0 = node0.key
        while (i++ < n) {
            val node = nodes[i]
            val p = node.key
            val dx = p.x - p0.x
            val dy = p.y - p0.y
            val dz = p.z - p0.z
            if (grounded && dy != 0) return i - 1
            var xi = xis[ii]
            var yi = yis[ii]
            var zi = zis[ii]
            xi += dx
            yi += dy
            zi += dz
            if (xi * zi or zi * yi or yi * xi != 0) {
                val xi0 = xis[ii]
                val yi0 = yis[ii]
                val zi0 = zis[ii]
                xi00 = xi0
                yi00 = yi0
                zi00 = zi0
                xi -= xi0
                yi -= yi0
                zi -= zi0
                val bdx0 = bdx
                val bdy0 = bdy
                val bdz0 = bdz
                if (!(dx != 0).let { bdx = bdx xor it; bdx } && bdx0 ||
                    !(dy != 0).let { bdy = bdy xor it; bdy } && bdy0 ||
                    !(dz != 0).let { bdz = bdz xor it; bdz } && bdz0) break else ++ii
                i0 = i - 1
            }
            xis[ii] = xi
            yis[ii] = yi
            zis[ii] = zi
            sq = (xi * zi00 + xi * yi00
                    + (zi * xi00 + zi * yi00)
                    + (yi * xi00 + yi * zi00))
            sq *= sq
            if (sq > (zi00 + yi00 + xi00) * (zi00 + yi00 + xi00)) break
            p0 = p
        }
        i = i0
        xi00 = xis[0]
        yi00 = yis[0]
        zi00 = zis[0]
        val iiN = ii
        var xi = 0
        var yi = 0
        var zi = 0
        var axi00 = Math.abs(xi00)
        var ayi00 = Math.abs(yi00)
        var azi00 = Math.abs(zi00)
        ii = 0
        p0 = nodes[i].key
        while (i++ < n) {
            val node = nodes[i]
            val p = node.key
            val dx = p.x - p0.x
            val dy = p.y - p0.y
            val dz = p.z - p0.z
            val xi0 = xi
            val yi0 = yi
            val zi0 = zi
            xi += dx
            yi += dy
            zi += dz
            if (Math.abs(xi0) > axi00 || Math.abs(yi0) > ayi00 || Math.abs(zi0) > azi00) {
                --i
                break
            }
            if (xi * zi or zi * yi or yi * xi != 0) {
                if (xi0 != xi00 || yi0 != yi00 || zi0 != zi00) break
                xi -= xi00
                yi -= yi00
                zi -= zi00
                ii = (ii + 1) % iiN
                xi00 = xis[ii]
                yi00 = yis[ii]
                zi00 = zis[ii]
                axi00 = Math.abs(xi00)
                ayi00 = Math.abs(yi00)
                azi00 = Math.abs(zi00)
            }
            if (dx * xi00 < 0 || dy * yi00 < 0 || dz * zi00 < 0) break
            p0 = p
        }
        return --i
    }

    private fun unlevelIndex(from: Int, position: Vec3d?): Int {
        val y0 = Math.floor(position!!.y).toInt()
        val nodes = nodes
        var levelIndex = length()
        for (i in from until length()) {
            val node = nodes[i]
            if (node.key.y - y0 != 0) {
                levelIndex = i
                break
            }
        }
        return levelIndex
    }

    override fun sameAs(other: IPath): Boolean {
        val length = length
        var c = 0
        val i: Iterator<INode?> = other.iterator()
        while (c < length && i.hasNext()) {
            if (nodes[c].key != i.next()!!.coordinates()) return false
            c++
        }
        return c >= length && !i.hasNext()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as PathObject
        return i == that.i && sameAs(that)
    }

    override fun hashCode(): Int {
        val last = last()
        return last?.hashCode() ?: 0
    }

    override fun toString(): String {
        val sb = StringBuilder()
        var index = 0
        sb.append("Last Mutation: ")
        sb.append(lastMutationTime)
        sb.append(System.lineSeparator())
        for (pp in nodes) {
            if (index++ == i) sb.append('*')
            sb.append(pp.key)
            sb.append(System.lineSeparator())
        }
        return sb.toString()
    }

    fun adjustPathPosition(formerPath: IPath, pathingEntity: IPathingEntity) {
        val pointOffset = pointToPositionOffset(pathingEntity.width())
        val length = length
        val nodes = nodes
        val lastPointVisited = formerPath.current()
        val coordinates = pathingEntity.coordinates()
        val x = coordinates!!.x
        val y = coordinates.y
        val z = coordinates.z
        var minSquareDistFromSource = Double.MAX_VALUE
        var c = -1
        while (++c < formerPath.cursor() && c < length && nodes[c].key == formerPath.at(c).coordinates());
        c--
        while (++c < length) {
            val node: INode = nodes[c]
            val p = node.coordinates()
            if (p == lastPointVisited.coordinates()) {
                i = c
                break
            }
            val dx = p.x - x + pointOffset
            val dy = p.y - y
            val dz = p.z - z + pointOffset
            val squareDelta = dx * dx + dy * dy + dz * dz
            if (squareDelta < minSquareDistFromSource) {
                minSquareDistFromSource = squareDelta
                i = c
            }
        }
    }

    fun reachableFrom(otherPath: PathObject): Boolean {
        val pivot = otherPath.current()
        for (node in nodes) if (node.key == pivot.coordinates()) return true
        return false
    }

    companion object {
        private const val PATHPOINT_SNAP_MARGIN_SQ = 0.25
        private var DIRECT_LINE_TIME_LIMIT: FloatRange? = FloatRange(1f, 2f)
        fun configureFrom(configModel: IConfigModel) {
            DIRECT_LINE_TIME_LIMIT = configModel.directLineTimeLimit()
        }

        fun fromHead(speed: Float, random: Random?, head: Node?): IPath {
            var i = 1
            run {
                var p = head
                while (p!!.parent() != null) {
                    ++i
                    p = p!!.parent()
                }
            }
            val result = arrayOfNulls<Node>(i)
            result[--i] = head
            var p = head
            while (p!!.parent() != null) {
                p = p!!.parent()
                result[--i] = p
            }
            return if (result.size <= 1) IncompletePath(result[0]) else PathObject(speed, random, *result)
        }

        @kotlin.jvm.JvmStatic
        fun active(path: IPath?): Boolean {
            return path != null && !path.done()
        }

        @kotlin.jvm.JvmStatic
        fun positionFor(subject: IPathingEntity, point: Vec3i?): Vec3d {
            val offset = pointToPositionOffset(subject.width())
            return Vec3d(
                (point!!.x + offset).toDouble(),
                point.y.toDouble(),
                (point.z + offset).toDouble()
            )
        }

        private fun pointToPositionOffset(subjectWidth: Float): Float {
            val dw = Math.ceil(subjectWidth.toDouble()).toFloat() / 2
            return dw - Math.floor(dw.toDouble()).toInt()
        }
    }

    init {
        // TODO NULL AAAAA
        this.nodes = nodes as Array<Node>
        length = nodes.size
        this.speed = speed
        this.random = random
        nextDirectLineTimeout = DIRECT_LINE_TIME_LIMIT!!.next(random)
    }
}