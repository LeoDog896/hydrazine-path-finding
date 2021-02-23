package com.extollit.gaming.ai.path.model

import com.extollit.linalg.immutable.Vec3i
import java.text.MessageFormat
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.sqrt

/**
 * Implementation of a node in an A* graph which is used by a path to move an entity between points
 *
 * @see PathObject
 */
class Node : INode {

    /**
     * The coordinates of this node in Space.
     */
    override val coordinates: Vec3i

    private var word = 0

    /** The previous node of this node. If this node is a root, there won't be a parent. */
    var parent: Node? = null
        private set

    private var children: NodeLinkedList? = null

    constructor(coordinates: Vec3i) {
        this.coordinates = coordinates
        unassign()
    }

    @JvmOverloads
    constructor(
        key: Vec3i,
        passibility: Passibility,
        volatility: Boolean = false,
        gravitation: Gravitation? = Gravitation.grounded
    ) {
        coordinates = key
        word =
            Mask_512 shl Index_BitOffs.toInt() or (gravitation!!.ordinal and Mask_Gravitation.toInt() shl Gravitation_BitOffs.toInt()) or (passibility.ordinal and Mask_Passibility.toInt()) or ((if (volatility) 1 else 0) shl Volatile_BitOffs.toInt())
    }


    fun length(): Byte = (word shr Length_BitOffs.toInt() and Mask_128).toByte()

    fun remaining(): Byte = (word shr Remain_BitOffs.toInt() and Mask_128).toByte()

    fun journey(): Byte = (length() + remaining()).toByte()

    /**
     * Goes through all parents of this node to find the oldest relative of this Node.
     *
     * @return The highest up node in this Node family tree.
     */
    fun root(): Node {

        // Gets mutable node for looping
        var node: Node = this

        while (!node.orphaned()) {
            // If the node is orphaned, this is the root.
            if (node.orphaned()) return node

            // Else continue with the next parent (will not be null)
            node = node.parent!!
        }

        return node
    }

    override fun passibility(): Passibility = Passibility.values()[word and Mask_Passibility.toInt()]

    fun passibility(passibility: Passibility?) {
        var mutablePassibility = passibility
        if (parent != null) mutablePassibility = mutablePassibility!!.between(parent!!.passibility())
        word = word and Mask_Passibility.inv().toInt() or mutablePassibility!!.ordinal
    }

    override fun gravitation(): Gravitation = Gravitation.values()[word shr Gravitation_BitOffs.toInt() and Mask_Gravitation.toInt()]

    fun gravitation(gravitation: Gravitation?) {
        word =
            word and (Mask_Gravitation shl Gravitation_BitOffs).toInt().inv() or (gravitation!!.ordinal shl Gravitation_BitOffs.toInt())
    }

    fun length(length: Int): Boolean {
        if (length > Mask_128 || length < 0) return false
        word =
            word and (Mask_128 shl Length_BitOffs.toInt() or (1 shl LengthDirty_BitOffs.toInt())).inv() or (length shl Length_BitOffs.toInt())
        return true
    }

    fun addLength(dl: Int): Boolean = length(length() + dl)

    fun remaining(delta: Int): Boolean {
        if (delta > Mask_128 || delta < 0) return false
        word = word and (Mask_128 shl Remain_BitOffs.toInt()).inv() or (delta shl Remain_BitOffs.toInt())
        return true
    }

    fun reset() {
        word = wordReset(this)
    }

    fun rollback(): Unit = reset()

    fun index(): Short {
        val index = (word shr Index_BitOffs.toInt() and Mask_512).toShort()
        return if (index.toInt() == Mask_512) -1 else index
    }

    fun index(index: Int): Boolean {
        if (index >= Mask_512 || index < -1) return false
        word = word and (Mask_512 shl Index_BitOffs.toInt()).inv() or (index and Mask_512 shl Index_BitOffs.toInt())
        return true
    }

    fun dirty(): Boolean = word shr LengthDirty_BitOffs.toInt() and 1 == 1

    fun dirty(flag: Boolean) {
        word = word and (1 shl LengthDirty_BitOffs.toInt()).inv() or if (flag) 1 shl LengthDirty_BitOffs.toInt() else 0
    }

    fun visited(): Boolean = word shr Visited_BitOffs.toInt() and 1 == 1

    fun visited(flag: Boolean) {
        word = word and (1 shl Visited_BitOffs.toInt()).inv() or if (flag) 1 shl Visited_BitOffs.toInt() else 0
    }

    fun volatile_(): Boolean = word shr Volatile_BitOffs.toInt() and 1 == 1

    fun volatile_(flag: Boolean) {
        word = word and (1 shl Volatile_BitOffs.toInt()).inv() or if (flag) 1 shl Volatile_BitOffs.toInt() else 0
    }

    fun assigned(): Boolean = index().toInt() != -1

    fun target(targetPoint: Vec3i?): Boolean {
        val distance = sqrt(squareDelta(this, targetPoint).toDouble())
            .toInt()
        if (distance > Mask_128) return false
        word = word and (Mask_128 shl Remain_BitOffs.toInt()).inv() or (distance shl Remain_BitOffs.toInt())
        return true
    }

    operator fun contains(node: Node?): Boolean {
        var curr: Node? = this
        do {
            if (curr === node) return true
        } while (curr?.parent.apply { curr = this } != null)
        return false
    }

    /**
     * Checks if the node has any parent.
     *
     * @return True if this node has no parents
     */
    fun orphaned(): Boolean = parent == null

    fun orphan() {
        if (parent != null) parent!!.removeChild(this)
        parent = null
    }

    fun isolate() {
        orphan()
        sterilize()
    }

    fun sterilize() {
        if (children != null) {
            for (child in children!!) {
                assert(child.parent === this)
                child.parent = null
            }
            children = null
        }
    }

    fun infecund(): Boolean = children == null

    private fun removeChild(child: Node) {
        if (children != null) children = children!!.remove(child)
        assert(children == null || child !in children!!)
    }

    fun unassign(): Boolean = index(-1)

    fun appendTo(parent: Node?, delta: Int, remaining: Int): Boolean {
        bindParent(parent)
        return if (!length(parent!!.length() + delta)) false else remaining(remaining)
    }

    fun bindParent(parent: Node?) {
        assert(!cyclic(parent))
        orphan()
        this.parent = parent
        parent!!.addChild(this)
        passibility(passibility())
    }

    private fun addChild(child: Node) {
        if (children == null) children = NodeLinkedList(child) else children!! += child
        assert(child in children!!)
    }

    /**
     * Get all the children of this Node.
     *
     * @return All children of this node.
     */
    fun children(): Iterable<Node> = children ?: emptyList()

    private fun cyclic(parent: Node?): Boolean {
        var p = parent
        while (p != null) p = if (p === this || p.coordinates == coordinates) return true else p.parent
        return false
    }

    override fun toString(): String {
        val sb = StringBuilder("$coordinates")
        val index = index()
        if (volatile_()) sb.append('!')
        if (visited()) sb.insert(0, '|')
        when (gravitation()) {
            Gravitation.airborne -> sb.append('^')
            Gravitation.buoyant -> sb.append('~')
            else -> Unit
        }
        if (index.toInt() == -1) sb.append(" (unassigned)") else {
            sb.append(" @ ")
            sb.append(index.toInt())
        }
        var length = "${length()}"
        if (dirty()) length += '*'
        return "$sb" + MessageFormat.format(
            " ({0}) : length={1}, remaining={2}, journey={3}",
            passibility(),
            length,
            remaining(),
            journey()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val pathPoint = other as Node
        return coordinates == pathPoint.coordinates
    }

    override fun hashCode(): Int {
        val key = coordinates
        var result = key.x shr 4
        result = 31 * result + (key.y shr 4)
        result = 31 * result + (key.z shr 4)
        return result
    }

    companion object {
        /** Magic Bytes. No idea.  */
        private const val BitWidth_512: Byte = 9
        private const val BitWidth_128: Byte = 7
        private const val Mask_Passibility = 3.toByte()
        private const val Mask_Gravitation = 3.toByte()
        private const val Index_BitOffs: Byte = 2
        private const val Volatile_BitOffs = (Index_BitOffs + BitWidth_512).toByte()
        private const val Length_BitOffs = (Volatile_BitOffs + 1).toByte()
        private const val Remain_BitOffs = (Length_BitOffs + BitWidth_128).toByte()
        private const val Visited_BitOffs = (Remain_BitOffs + BitWidth_128).toByte()
        private const val Gravitation_BitOffs = (Visited_BitOffs + 1).toByte()
        private const val LengthDirty_BitOffs = (Gravitation_BitOffs + 2).toByte()
        const val MAX_PATH_DISTANCE: Short = ((1 shl BitWidth_128.toInt()) - 1).toShort()
        private const val Mask_128 = MAX_PATH_DISTANCE.toInt()
        private const val Mask_512 = (1 shl BitWidth_512.toInt()) - 1
        const val MAX_INDICES: Int = (1 shl BitWidth_512.toInt()) - 1
        private fun wordReset(copy: Node): Int =
            copy.word and ((Mask_Passibility or ((1 shl Volatile_BitOffs.toInt()).toByte()) or (Mask_Gravitation shl Gravitation_BitOffs)).toInt()) or (Mask_512 shl Index_BitOffs.toInt() or (1 shl LengthDirty_BitOffs.toInt()))

        fun squareDelta(left: Node?, right: Node?): Int = squareDelta(left, right!!.coordinates)

        fun squareDelta(left: Node?, rightCoords: Vec3i?): Int {
            val leftCoords = left!!.coordinates
            val dx = leftCoords.x - rightCoords!!.x
            val dy = leftCoords.y - rightCoords.y
            val dz = leftCoords.z - rightCoords.z
            return dx * dx + dy * dy + dz * dz
        }

        /**
         * Checks if the node paramater is not blocked, and has less risk than this node.
         *
         * @param node The node to check
         *
         * @return If an entity can go from this node to that node without any horrible risk changes.
         */
        fun passible(node: Node): Boolean = node.passibility().betterThan(Passibility.impassible)
    }
}