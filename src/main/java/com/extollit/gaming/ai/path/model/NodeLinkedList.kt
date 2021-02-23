package com.extollit.gaming.ai.path.model

import com.extollit.collect.CollectionsExt

class NodeLinkedList(
    val self: Node,
    private var next: NodeLinkedList? = null
) : Iterable<Node> {

    private class NodeIterable(private var head: NodeLinkedList?) : MutableIterator<Node> {
        override fun hasNext() = head != null

        override fun next(): Node {
            val head = head
            this.head = head!!.next
            return head.self
        }

        override fun remove() = throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<Node> = NodeIterable(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val nodes = other as NodeLinkedList
        return if (next != nodes.next) false else self == nodes.self
    }

    /**
     * Removes an element from this array.
     *
     * @param child The child to remove
     *
     * @return The LinkedList of that node.
     */
    fun remove(child: Node): NodeLinkedList? {
        var e: NodeLinkedList? = this
        var last: NodeLinkedList? = null
        do {
            if (e?.self === child) {
                val tail = e.next
                var head: NodeLinkedList? = this
                if (last == null) head = tail else last.next = tail
                return head
            }
            last = e
            e = e?.next
        } while (e != null)
        return this
    }

    /**
     * Adds a [Node] to this LinkedList
     *
     * @param child The child to add to this [NodeLinkedList]
     *
     * @return If the addition operation was successful or not (will be unsuccessful if the node is already in the list)
     */
    fun add(child: Node): Boolean {
        var e: NodeLinkedList? = this
        var last: NodeLinkedList?
        do {
            if (e?.self === child) return false
            last = e
        } while (e?.next.apply { e = this } != null)
        last?.next = NodeLinkedList(child)
        return true
    }
    /**
     * Adds a [Node] to this LinkedList
     *
     * @param child The child to add to this [NodeLinkedList]
     */
    operator fun plusAssign(child: Node): Unit {
        add(child)
    }

    /**
     * Check if this [NodeLinkedList] contains a [Node]
     *
     * @param node The node to check this LinkedList by.
     *
     * @return If this [NodeLinkedList] contains the said [node]
     */
    operator fun contains(node: Node): Boolean {
        var list: NodeLinkedList? = this
        while (list != null) {
            if (list.self === node) return true
            list = list.next
        }
        return false
    }

    override fun hashCode(): Int = self.hashCode()

    override fun toString(): String = CollectionsExt.toList(this).toString()
}