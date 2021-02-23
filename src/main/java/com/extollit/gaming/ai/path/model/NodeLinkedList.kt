package com.extollit.gaming.ai.path.model

import java.lang.UnsupportedOperationException
import com.extollit.collect.CollectionsExt

class NodeLinkedList private constructor(val self: Node, private var next: NodeLinkedList?) : Iterable<Node> {
    constructor(self: Node) : this(self, null)

    private class Iter(private var head: NodeLinkedList?) : MutableIterator<Node> {
        override fun hasNext(): Boolean {
            return head != null
        }

        override fun next(): Node {
            val head = head
            this.head = head!!.next
            return head.self
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    override fun iterator(): MutableIterator<Node> {
        return Iter(this)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val nodes = o as NodeLinkedList
        return if (next != nodes.next) false else self == nodes.self
    }

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

    fun add(child: Node): Boolean {
        var e: NodeLinkedList? = this
        var last: NodeLinkedList?
        do {
            if (e?.self === child) return false
            last = e
        } while (e?.next.also { e = it } != null)
        last?.next = NodeLinkedList(child)
        return true
    }

    override fun hashCode(): Int {
        return self.hashCode()
    }

    override fun toString(): String {
        return CollectionsExt.toList(this).toString()
    }

    companion object {
        fun contains(list: NodeLinkedList?, other: Node): Boolean {
            var list = list
            while (list != null) {
                if (list.self === other) return true
                list = list.next
            }
            return false
        }
    }
}