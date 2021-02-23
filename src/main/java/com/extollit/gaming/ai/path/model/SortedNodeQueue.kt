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
import com.extollit.linalg.immutable.Vec3i
import java.util.*
import kotlin.math.sqrt

/**
 * A queue of Points, or Nodes
 */
class SortedNodeQueue {
    private val list = ArrayList<Node?>(8)
    fun fastAdd(node: Node?): Boolean {
        if (!node!!.index(list.size)) return false
        list.add(node)
        sortBack(node.index().toInt())
        return true
    }

    /**
     * Removes all elements from this queue
     */
    fun clear() {
        for (node in list) node!!.unassign()
        list.clear()
    }

    val isEmpty: Boolean
        get() = list.isEmpty()

    fun trimFrom(source: Node): Node {
        if (source.orphaned()) return source
        val root0 = source.root()
        val dd = source.key.subOf(root0.key)
        val list: MutableList<Node?> = list
        val length0 = source.length()
        val path = Stack<Node?>()
        val treeTransitional = TreeTransitional(source)
        val i = list.listIterator()
        while (i.hasNext()) {
            val head = i.next()
            var point = head
            while (!point!!.orphaned()) {
                point = point.parent()
                path.push(point)
            }
            if (point === source) {
                while (!path.isEmpty()) {
                    point = path.pop()
                    val length = point!!.length() - length0
                    point.length(length)
                }
                val length = head!!.length() - length0
                head.length(length)
                head.index(i.previousIndex())
            } else {
                val root: Node?
                root = if (path.isEmpty()) head else path.pop()
                if (head === point || head!!.key.subOf(point.key).dot(dd) <= 0) {
                    head.dirty(true)
                    while (!path.isEmpty()) path.pop()!!.dirty(true)
                    treeTransitional.queue(head, root)
                    head.index(i.previousIndex())
                } else {
                    if (!path.isEmpty()) {
                        val branch = path.pop()
                        branch!!.dirty(true)
                        treeTransitional.queue(branch, root)
                    }
                    i.remove()
                    path.clear()
                    head.unassign()
                    head.visited(false)
                }
            }
        }
        treeTransitional.finish(this)
        return root0
    }

    fun cullBranch(ancestor: Node) {
        val list: MutableList<Node?> = list
        val stack = Stack<Node?>()
        val i = list.listIterator()
        val culled: MutableList<Node?> = LinkedList()
        while (i.hasNext()) {
            val head = i.next()
            var point = head
            while (!point!!.orphaned() && point !== ancestor) {
                point = point.parent()
                stack.push(point)
            }
            if (point !== ancestor) head!!.index(i.previousIndex()) else {
                i.remove()
                head!!.unassign()
                culled.add(head)
                culled.addAll(stack)
            }
            stack.clear()
        }
        for (node in culled) {
            node!!.reset()
            node.visited(false)
        }
    }

    fun view(): List<Node?> {
        return Collections.unmodifiableList(list)
    }

    /**
     * Gets the node on the top of this stack
     *
     * @return The node at the top of the stack.
     */
    fun top(): Node? {
        return list[0]
    }

    fun dequeue(): Node {
        val list = list
        val point: Node?
        if (list.size == 1) point = list.removeAt(0) else {
            point = list.set(0, list.removeAt(list.size - 1))
            sortForward(0)
        }
        point!!.unassign()
        return point
    }

    fun nextContains(ancestor: Node?): Boolean {
        return list[0]!!.contains(ancestor)
    }

    private fun sortBack(index: Int) {
        var index = index
        val list = list
        val originalPoint = list[index]
        val distanceRemaining = originalPoint!!.journey().toInt()
        val originalPassibility = originalPoint.passibility()
        while (index > 0) {
            val i = index - 1 shr 1
            val point = list[i]
            val passibility = point!!.passibility()
            if (distanceRemaining >= point.journey() && originalPassibility == passibility || originalPassibility.worseThan(
                    passibility
                )
            ) break
            list[index] = point
            point.index(index)
            index = i
        }
        list[index] = originalPoint
        originalPoint.index(index)
    }

    private fun sortForward(index: Int) {
        var index = index
        val list = list
        val originalPoint = list[index]
        val distanceRemaining = originalPoint!!.journey().toInt()
        val originalPassibility = originalPoint.passibility()
        do {
            val i = 1 + (index shl 1)
            val j = i + 1
            if (i >= list.size) break
            val pointAlpha = list[i]
            val distAlpha = pointAlpha!!.journey().toInt()
            val passibilityAlpha = pointAlpha.passibility()
            val pointBeta: Node?
            val distBeta: Int
            val passibilityBeta: Passibility?
            if (j >= list.size) {
                pointBeta = null
                distBeta = Int.MIN_VALUE
                passibilityBeta = Passibility.passible
            } else {
                pointBeta = list[j]
                distBeta = pointBeta!!.journey().toInt()
                passibilityBeta = pointBeta.passibility()
            }
            if (distAlpha < distBeta && passibilityAlpha == passibilityBeta
                || passibilityAlpha.betterThan(passibilityBeta)
            ) {
                if (distAlpha >= distanceRemaining && passibilityAlpha == originalPassibility
                    || passibilityAlpha.worseThan(originalPassibility)
                ) break
                list[index] = pointAlpha
                pointAlpha.index(index)
                index = i
            } else {
                if (pointBeta == null || distBeta >= distanceRemaining && passibilityAlpha == originalPassibility
                    || passibilityBeta.worseThan(originalPassibility)
                ) break
                list[index] = pointBeta
                pointBeta.index(index)
                index = j
            }
        } while (true)
        list[index] = originalPoint
        originalPoint.index(index)
    }

    fun appendTo(point: Node, parent: Node?, targetPoint: Vec3i?): Boolean {
        return appendTo(
            point, parent, sqrt(Node.squareDelta(point, targetPoint).toDouble())
                .toInt()
        )
    }

    fun appendTo(point: Node, parent: Node?, remaining: Int): Boolean {
        val squareDelta: Int = Node.squareDelta(parent, point)
        val length = point.length()
        if (!point.assigned() || parent!!.length() + squareDelta < length * length && !point.passibility().betterThan(
                parent.passibility()
            )
        ) {
            val distance0 = point.journey()
            if (point.appendTo(parent, Math.sqrt(squareDelta.toDouble()).toInt(), remaining)) return resort(
                point,
                distance0
            ) else point.orphan()
        }
        return false
    }

    fun addLength(point: Node?, diff: Int): Boolean {
        val journey0 = point!!.journey()
        point.addLength(diff)
        return resort(point, journey0)
    }

    private fun resort(point: Node?, journey0: Byte): Boolean {
        val journey = point!!.journey().toInt()
        if (point.assigned()) {
            if (journey < journey0) sortBack(point.index().toInt()) else sortForward(point.index().toInt())
            return true
        } else add(point)
        return false
    }

    fun add(point: Node?) {
        check(!point!!.assigned()) { "Point is already assigned" }
        if (fastAdd(point)) return
        val list = list
        val size = size()
        val i = list.listIterator(size)
        var amount = Math.ceil((size.toFloat() * CULL_THRESHOLD).toDouble())
            .toInt()
        while (amount > 0 && i.hasPrevious()) {
            i.previous()!!.unassign()
            i.remove()
            --amount
        }
        fastAdd(point)
    }

    fun size(): Int {
        return list.size
    }

    fun roots(): Set<Node?> {
        val roots: MutableSet<Node?> = HashSet(1)
        for (node in list) {
            val root = node!!.root()
            roots.add(root)
        }
        return roots
    }

    override fun toString(): String {
        return list.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as SortedNodeQueue
        return list == that.list
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }

    companion object {
        private const val CULL_THRESHOLD = 0.1f
    }
}