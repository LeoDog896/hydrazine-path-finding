package com.extollit.gaming.ai.path.model

import com.extollit.collect.ArrayIterable
import com.extollit.gaming.ai.path.IConfigModel
import com.extollit.linalg.immutable.Vec3d
import com.extollit.linalg.immutable.Vec3i
import com.extollit.num.FloatRange
import java.text.MessageFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class PathObject @JvmOverloads protected constructor(
    val speed: Float,
    val random: Random = Random(),
    @kotlin.jvm.JvmField vararg val nodes: Node
) : IPath {
    @kotlin.jvm.JvmField
    var index: Int = 0
    private var taxiUntil = 0
    private var adjacentIndex = 0
    private var length: Int = nodes.size
    private var nextDirectLineTimeout: Float = DIRECT_LINE_TIME_LIMIT!!.next(random)
    private var lastMutationTime = -1f

    override fun truncateTo(length: Int) {
        if (length < 0 || length >= nodes.size) throw ArrayIndexOutOfBoundsException(
            MessageFormat.format("Length is out of bounds 0 <= length < {0} but length = {1}", nodes.size, length)
        )
        this.length = length
    }

    override fun untruncate() {
        length = nodes.size
    }

    override fun iterator(): MutableIterator<INode> = ArrayIterable.Iter<INode>(nodes, length)

    override fun length(): Int = length

    override fun cursor(): Int = index

    override fun at(i: Int): Node = nodes[i]

    override fun current(): Node = nodes[index]

    override fun last(): INode? {
        val nodes = nodes
        val length = length
        return if (length > 0) nodes[length - 1] else null
    }

    override fun done(): Boolean = index >= length

    override fun update(pathingEntity: IPathingEntity) {
        var mutated = false
        try {
            if (done()) return
            val unlevelIndex: Int
            val capabilities = pathingEntity.capabilities()
            val grounded = !(capabilities!!.avian() || capabilities.aquatic() && capabilities.swimmer())
            val fy: Float
            if (grounded) {
                unlevelIndex = unlevelIndex(index, pathingEntity.coordinates())
                fy = 0f
            } else {
                unlevelIndex = length
                fy = 1f
            }
            val adjacentIndex0 = adjacentIndex
            val minDistanceSquared = updateNearestAdjacentIndex(pathingEntity, unlevelIndex, fy)
            val adjacentIndex = adjacentIndex
            var targetIndex = index
            if (minDistanceSquared <= PATHPOINT_SNAP_MARGIN_SQ) {
                var advanceTargetIndex: Int? = null
                targetIndex = adjacentIndex
                if (targetIndex >= taxiUntil && directLine(
                        targetIndex,
                        unlevelIndex,
                        grounded
                    ).apply { advanceTargetIndex = this } > targetIndex
                ) targetIndex = advanceTargetIndex!! else targetIndex = adjacentIndex + 1
            } else if (minDistanceSquared > 0.5 || targetIndex < taxiUntil) targetIndex = adjacentIndex
            mutated = adjacentIndex > adjacentIndex0
            this.adjacentIndex = adjacentIndex
            index = max(adjacentIndex, targetIndex)
            if (stagnantFor(pathingEntity) > nextDirectLineTimeout) {
                if (taxiUntil < adjacentIndex) taxiUntil = adjacentIndex + 1 else taxiUntil++
                nextDirectLineTimeout += DIRECT_LINE_TIME_LIMIT!!.next(random)
            }
            val node = if (done()) last() else current()
            node?.run { moveSubjectTo(pathingEntity, this) }
        } finally {
            if (mutated || lastMutationTime < 0) {
                lastMutationTime = pathingEntity.age() * speed
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
        let {
            var i = it.adjacentIndex.apply { nextAdjacentIndex = this }
            while (i < it.length && i < end) {
                val node = it.nodes[i]
                val pp = node.coordinates
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
        val position = positionFor(subject, pathPoint.coordinates)
        d.sub(position)
        if (d.mg2() > PATHPOINT_SNAP_MARGIN_SQ) subject.moveTo(
            position,
            pathPoint.passibility(),
            pathPoint.gravitation()
        )
    }

    override fun taxiing(): Boolean = taxiUntil >= adjacentIndex

    override fun taxiUntil(index: Int) {
        taxiUntil = index
    }

    override fun stagnantFor(subject: IPathingEntity): Float = if (lastMutationTime < 0) 0f else subject.age() * speed - lastMutationTime

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
        var p0 = node0.coordinates
        while (i++ < n) {
            val node = nodes[i]
            val p = node.coordinates
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
                if (!(dx != 0).run { bdx = bdx xor this; bdx } && bdx0 ||
                    !(dy != 0).run { bdy = bdy xor this; bdy } && bdy0 ||
                    !(dz != 0).run { bdz = bdz xor this; bdz } && bdz0) break else ++ii
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
        var axi00 = abs(xi00)
        var ayi00 = abs(yi00)
        var azi00 = abs(zi00)
        ii = 0
        p0 = nodes[i].coordinates
        while (i++ < n) {
            val node = nodes[i]
            val p = node.coordinates
            val dx = p.x - p0.x
            val dy = p.y - p0.y
            val dz = p.z - p0.z
            val xi0 = xi
            val yi0 = yi
            val zi0 = zi
            xi += dx
            yi += dy
            zi += dz
            if (abs(xi0) > axi00 || abs(yi0) > ayi00 || abs(zi0) > azi00) {
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
                axi00 = abs(xi00)
                ayi00 = abs(yi00)
                azi00 = abs(zi00)
            }
            if (dx * xi00 < 0 || dy * yi00 < 0 || dz * zi00 < 0) break
            p0 = p
        }
        return --i
    }

    private fun unlevelIndex(from: Int, position: Vec3d?): Int {
        val y0 = floor(position!!.y).toInt()
        val nodes = nodes
        var levelIndex = length()
        for (i in from until length()) {
            val node = nodes[i]
            if (node.coordinates.y - y0 != 0) {
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
            if (nodes[c].coordinates != i.next()!!.coordinates) return false
            c++
        }
        return c >= length && !i.hasNext()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as PathObject
        return index == that.index && sameAs(that)
    }

    override fun hashCode(): Int {
        val last = last()
        return last?.hashCode() ?: 0
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Last Mutation: ")
        sb.append(lastMutationTime)
        sb.append(System.lineSeparator())
        for ((index, pp) in nodes.withIndex()) {
            if (index == this.index) sb.append('*')
            sb.append(pp.coordinates)
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
        while (++c < formerPath.cursor() && c < length && nodes[c].coordinates == formerPath.at(c).coordinates);
        c--
        while (++c < length) {
            val node: INode = nodes[c]
            val p = node.coordinates
            if (p == lastPointVisited.coordinates) {
                index = c
                break
            }
            val dx = p.x - x + pointOffset
            val dy = p.y - y
            val dz = p.z - z + pointOffset
            val squareDelta = dx * dx + dy * dy + dz * dz
            if (squareDelta < minSquareDistFromSource) {
                minSquareDistFromSource = squareDelta
                index = c
            }
        }
    }

    fun reachableFrom(otherPath: PathObject): Boolean {
        val pivot = otherPath.current()
        return nodes.any { it.coordinates == pivot.coordinates }
    }

    companion object {
        private const val PATHPOINT_SNAP_MARGIN_SQ = 0.25
        private var DIRECT_LINE_TIME_LIMIT: FloatRange? = FloatRange(1f, 2f)
        fun configureFrom(configModel: IConfigModel) {
            DIRECT_LINE_TIME_LIMIT = configModel.directLineTimeLimit()
        }

        fun fromHead(speed: Float, random: Random = Random(), head: Node): IPath {
            var i = 1
            let {
                var p: Node? = head
                while (p!!.parent != null) {
                    ++i
                    p = p!!.parent
                }
            }
            val result = arrayOfNulls<Node>(i)
            result[--i] = head
            var p: Node? = head
            while (p!!.parent != null) {
                p = p!!.parent
                result[--i] = p
            }
            return if (result.size <= 1)
                IncompletePath(result[0]!!)
            else
                PathObject(speed, random, *result.map { it!! }.toTypedArray())
        }

        @JvmStatic
        fun active(path: IPath?): Boolean = path != null && !path.done()

        @JvmStatic
        fun positionFor(subject: IPathingEntity, point: Vec3i?): Vec3d {
            val offset = pointToPositionOffset(subject.width())
            return Vec3d(
                (point!!.x + offset).toDouble(),
                point.y.toDouble(),
                (point.z + offset).toDouble()
            )
        }

        private fun pointToPositionOffset(subjectWidth: Float): Float {
            val dw = ceil(subjectWidth.toDouble()).toFloat() / 2
            return dw - floor(dw.toDouble()).toInt()
        }
    }
}