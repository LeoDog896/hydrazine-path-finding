package com.extollit.gaming.ai.path.node

/**
 * A linked list designed for extra utility functions for [Node].
 *
 * Each [NodeLinkedList] has a reference to a [Node] and its next [NodeLinkedList].
 *
 * When adding a new [Node], it'll get the (looped) next node that doesn't have a [next] element,
 * and sets the child as so.
 *
 * Same for removal, except it'll re-link the separated nodes where that node is removed.
 */
class NodeLinkedList(
    /** Gets the node that is in this [NodeLinkedList]. Used as a reference to see what item is in here. */
    val self: Node,
    /** Reference to the next element in the linked list. */
    private var next: NodeLinkedList? = null
) : Iterable<Node> {

    /**
     * Represents an iterable for [NodeLinkedList]. Goes from one node to the next using references.
     */
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

        // Initialize this loop with [this], an element that may or may not have a next element.
        var element: NodeLinkedList = this
        var last: NodeLinkedList?

        do {
            // If this iteration's [Node] is the same as the child, then this child is already in this linked list
            if (element.self === child) return false

            // Else, move to the next element.
            last = element
        } while (element.next?.also { element = it } != null) // Sets [element] to element's [next] [Node], then loop if the next node is not null

        // If the child ([next]) node is null, that means the last element has no children and its child can be set..
        last?.next = NodeLinkedList(child)
        return true
    }
    /**
     * Adds a [Node] to this LinkedList
     *
     * @param child The child to add to this [NodeLinkedList]
     */
    operator fun plusAssign(child: Node) {
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

    override fun toString(): String = this.toList().toString()
}