package com.extollit.gaming.ai.path.model

import com.extollit.linalg.immutable.Vec3i
import java.util.*
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * A queue of [Node]s
 */
class SortedNodeQueue {
    private val list = ArrayList<Node?>(8)

    /**
     * Adds a node to this sorted queue without the overhead of checks.
     *
     * @param node The [Node] to add to this list
     *
     * @return If adding this node to this sorted queue was successful
     */
    fun fastAdd(node: Node): Boolean {
        if (!node.index(list.size)) return false
        list.add(node)
        sortBack(node.index().toInt())
        return true
    }

    /**
     * Adds a node to this sorted queue/
     *
     * @param point The [Node] to add to this list
     *
     * @return If adding this node was successful
     *
     * @see [fastAdd] for adding nodes without the extra conditional overhead.
     */
    fun add(point: Node): Boolean {
        check(!point.assigned()) { "Point is already assigned" }
        if (fastAdd(point)) return true
        val list = list
        val size = size()
        val i = list.listIterator(size)
        var amount = ceil((size.toFloat() * CULL_THRESHOLD).toDouble())
            .toInt()
        while (amount > 0 && i.hasPrevious()) {
            i.previous()!!.unassign()
            i.remove()
            --amount
        }

        return fastAdd(point)
    }

    /**
     * Removes all elements from this queue
     */
    fun clear() {
        for (node in list) node!!.unassign()
        list.clear()
    }

    /**
     * Checks if this queue is empty.
     */
    val isEmpty: Boolean
        get() = list.isEmpty()

    fun trimFrom(source: Node): Node {
        if (source.orphaned()) return source
        val root0 = source.root()
        val dd = source.coordinates.subOf(root0.coordinates)
        val list: MutableList<Node?> = list
        val length0 = source.length()
        val path = Stack<Node?>()
        val treeTransitional = TreeTransitional(source)
        val i = list.listIterator()
        while (i.hasNext()) {
            val head = i.next()
            var point = head
            while (!point!!.orphaned()) {
                point = point.parent
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
                val root: Node? = if (path.isEmpty()) head else path.pop()
                if (head === point || head!!.coordinates.subOf(point.coordinates).dot(dd) <= 0) {
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
                point = point.parent
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

    /**
     * Gets an Unmodifiable List of all nodes in this queue.
     *
     * @return unmodifiable list of all nodes in this sorted queue.
     */
    fun view(): List<Node?> = Collections.unmodifiableList(list)

    /**
     * Gets the node on the top of this stack.
     * This node was also the first to enter this stack out of every other stack.
     *
     * @return The node at the top of the stack.
     */
    fun top(): Node? = list[0]

    /**
     * Removes and returns the oldest element in this stack.
     *
     * @return The oldest element (most waiting element) in this stack.
     */
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

    fun nextContains(ancestor: Node?): Boolean =
        list[0]!!.contains(ancestor)

    private fun sortBack(index: Int) {
        var mutableIndex = index
        val list = list
        val originalPoint = list[mutableIndex]
        val distanceRemaining = originalPoint!!.journey().toInt()
        val originalPassibility = originalPoint.passibility()
        while (mutableIndex > 0) {
            val i = mutableIndex - 1 shr 1
            val point = list[i]
            val passibility = point!!.passibility()
            if (distanceRemaining >= point.journey() && originalPassibility == passibility || originalPassibility.worseThan(
                    passibility
                )
            ) break
            list[mutableIndex] = point
            point.index(mutableIndex)
            mutableIndex = i
        }
        list[mutableIndex] = originalPoint
        originalPoint.index(mutableIndex)
    }

    private fun sortForward(index: Int) {
        var mutableIndex = index
        val list = list
        val originalPoint = list[mutableIndex]
        val distanceRemaining = originalPoint!!.journey().toInt()
        val originalPassibility = originalPoint.passibility()
        do {
            val i = 1 + (mutableIndex shl 1)
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
                list[mutableIndex] = pointAlpha
                pointAlpha.index(mutableIndex)
                mutableIndex = i
            } else {
                if (pointBeta == null || distBeta >= distanceRemaining && passibilityAlpha == originalPassibility
                    || passibilityBeta.worseThan(originalPassibility)
                ) break
                list[mutableIndex] = pointBeta
                pointBeta.index(mutableIndex)
                mutableIndex = j
            }
        } while (true)
        list[mutableIndex] = originalPoint
        originalPoint.index(mutableIndex)
    }

    fun appendTo(point: Node, parent: Node, targetPoint: Vec3i?): Boolean =
        appendTo(
            point, parent, sqrt(Node.squareDelta(point, targetPoint).toDouble())
                .toInt()
        )

    fun appendTo(point: Node, parent: Node, remaining: Int): Boolean {
        val squareDelta: Int = Node.squareDelta(parent, point)
        val length = point.length()
        if (!point.assigned() || parent.length() + squareDelta < length * length && !point.passibility().betterThan(
                parent.passibility()
            )
        ) {
            val distance0 = point.journey()
            if (point.appendTo(parent, sqrt(squareDelta.toDouble()).toInt(), remaining)) return resort(
                point,
                distance0
            ) else point.orphan()
        }
        return false
    }

    fun addLength(point: Node, diff: Int): Boolean {
        val journey0 = point.journey()
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

    fun size(): Int = list.size

    fun roots(): Set<Node?> = list.mapTo<Node?, Node, HashSet<Node?>>(HashSet(1)) { it!!.root() }

    override fun toString(): String = "$list"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as SortedNodeQueue
        return list == that.list
    }

    override fun hashCode(): Int = list.hashCode()

    companion object {
        private const val CULL_THRESHOLD = 0.1f
    }
}