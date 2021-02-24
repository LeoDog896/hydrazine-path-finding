package com.extollit.gaming.ai.path.model

import com.extollit.gaming.ai.path.iterable.OneIterator
import com.extollit.gaming.ai.path.node.Node
import com.extollit.gaming.ai.path.node.path.IPath

class IncompletePath(val node: Node) : IPath {
    override fun truncateTo(length: Int): Nothing =
        throw ArrayIndexOutOfBoundsException("Cannot truncate incomplete paths")

    override fun untruncate() {}

    override fun length(): Int = 1

    override fun cursor(): Int = 0

    override fun at(i: Int): Node = node

    override fun current(): Node = node

    override fun last(): Node = node

    override fun done(): Boolean = false

    override fun taxiing(): Boolean = false

    override fun taxiUntil(index: Int) {}

    override fun sameAs(other: IPath): Boolean {
        if (other is IncompletePath) return other.node.coordinates == node.coordinates
        val i: Iterator<Node?> = other.iterator()
        return (!i.hasNext() || node.coordinates == i.next()!!.coordinates) && !i.hasNext()
    }

    override fun stagnantFor(subject: IPathingEntity): Float = 0f

    override fun update(pathingEntity: IPathingEntity) {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val iNodes = other as IncompletePath
        return node == iNodes.node
    }

    override fun hashCode(): Int = node.hashCode()

    override fun toString(): String = "*" + node.coordinates + "...?"

    override fun iterator(): MutableIterator<Node> = OneIterator(node)
}