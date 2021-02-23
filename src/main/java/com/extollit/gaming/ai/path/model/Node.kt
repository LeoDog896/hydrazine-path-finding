package com.extollit.gaming.ai.path.model

import com.extollit.linalg.immutable.Vec3i
import java.text.MessageFormat
import kotlin.experimental.inv
import kotlin.experimental.or

class Node : INode {
    @kotlin.jvm.JvmField
    val key: Vec3i
    private var word = 0
    private var previous: Node? = null
    private var children: NodeLinkedList? = null

    constructor(key: Vec3i) {
        this.key = key
        unassign()
    }

    @JvmOverloads
    constructor(
        key: Vec3i,
        passibility: Passibility,
        volatility: Boolean = false,
        gravitation: Gravitation? = Gravitation.grounded
    ) {
        this.key = key
        word =
            Mask_512 shl Index_BitOffs.toInt() or (gravitation!!.ordinal and Mask_Gravitation.toInt() shl Gravitation_BitOffs.toInt()) or (passibility.ordinal and Mask_Passibility.toInt()) or ((if (volatility) 1 else 0) shl Volatile_BitOffs.toInt())
    }

    override fun coordinates(): Vec3i {
        return key
    }

    fun length(): Byte = (word shr Length_BitOffs.toInt() and Mask_128).toByte()

    fun remaining(): Byte = (word shr Remain_BitOffs.toInt() and Mask_128).toByte()

    fun journey(): Byte = (length() + remaining()).toByte()

    /**
     * Gets the previous node of this node, or its "parent"
     *
     * @return The parent of the node.
     */
    fun parent(): Node? = previous

    /**
     * Goes through all parents of this node to find the oldest relative of this Node.
     *
     * @return The highest up node in this Node family tree.
     */
    fun root(): Node {
        var node: Node? = this
        while (!node!!.orphaned()) node = node.parent()
        return node
    }

    override fun passibility() = Passibility.values()[word and Mask_Passibility.toInt()]

    fun passibility(passibility: Passibility?) {
        var mutablePassibility = passibility
        val previous = parent()
        if (previous != null) mutablePassibility = mutablePassibility!!.between(previous.passibility())
        word = word and Mask_Passibility.inv().toInt() or mutablePassibility!!.ordinal
    }

    override fun gravitation() = Gravitation.values()[word shr Gravitation_BitOffs.toInt() and Mask_Gravitation.toInt()]

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

    fun rollback() = reset()

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
        val distance = Math.sqrt(squareDelta(this, targetPoint).toDouble())
            .toInt()
        if (distance > Mask_128) return false
        word = word and (Mask_128 shl Remain_BitOffs.toInt()).inv() or (distance shl Remain_BitOffs.toInt())
        return true
    }

    operator fun contains(node: Node?): Boolean {
        var curr: Node? = this
        do {
            if (curr === node) return true
        } while (curr?.previous.also { curr = it } != null)
        return false
    }

    /**
     * Checks if the node has any parent.
     *
     * @return True if this node has no parents
     */
    fun orphaned(): Boolean {
        return parent() == null
    }

    fun orphan() {
        if (previous != null) previous!!.removeChild(this)
        previous = null
    }

    fun isolate() {
        orphan()
        sterilize()
    }

    fun sterilize() {
        if (children != null) {
            for (child in children!!) {
                assert(child.previous === this)
                child.previous = null
            }
            children = null
        }
    }

    fun infecund(): Boolean = children == null

    private fun removeChild(child: Node) {
        if (children != null) children = children!!.remove(child)
        assert(!NodeLinkedList.contains(children, child))
    }

    fun unassign(): Boolean = index(-1)

    fun appendTo(parent: Node?, delta: Int, remaining: Int): Boolean {
        bindParent(parent)
        return if (!length(parent!!.length() + delta)) false else remaining(remaining)
    }

    fun bindParent(parent: Node?) {
        assert(!cyclic(parent))
        orphan()
        previous = parent
        parent!!.addChild(this)
        passibility(passibility())
    }

    private fun addChild(child: Node) {
        if (children == null) children = NodeLinkedList(child) else children!!.add(child)
        assert(NodeLinkedList.contains(children, child))
    }

    /**
     * Get all the children of this Node.
     *
     * @return All children of this node.
     */
    fun children(): Iterable<Node> {
        return children ?: emptyList()
    }

    private fun cyclic(parent: Node?): Boolean {
        var p = parent
        while (p != null) p = if (p === this || p.key == key) return true else p.parent()
        return false
    }

    override fun toString(): String {
        val sb = StringBuilder(key.toString())
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
        var length = java.lang.Byte.toString(length())
        if (dirty()) length += '*'
        return sb.toString() + MessageFormat.format(
            " ({0}) : length={1}, remaining={2}, journey={3}",
            passibility(),
            length,
            remaining(),
            journey()
        )
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val pathPoint = o as Node
        return key == pathPoint.key
    }

    override fun hashCode(): Int {
        val key = key
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
        const val MAX_PATH_DISTANCE = ((1 shl BitWidth_128.toInt()) - 1).toShort()
        private const val Mask_128 = MAX_PATH_DISTANCE.toInt()
        private const val Mask_512 = (1 shl BitWidth_512.toInt()) - 1
        const val MAX_INDICES = (1 shl BitWidth_512.toInt()) - 1
        private fun wordReset(copy: Node): Int {
            return copy.word and ((Mask_Passibility or ((1 shl Volatile_BitOffs.toInt()).toByte()) or (Mask_Gravitation shl Gravitation_BitOffs)).toInt()) or (Mask_512 shl Index_BitOffs.toInt() or (1 shl LengthDirty_BitOffs.toInt()))
        }

        fun squareDelta(left: Node?, right: Node?): Int {
            return squareDelta(left, right!!.key)
        }

        fun squareDelta(left: Node?, rightCoords: Vec3i?): Int {
            val leftCoords = left!!.key
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
        fun passible(node: Node?): Boolean {
            return node != null && node.passibility().betterThan(Passibility.impassible)
        }
    }
}