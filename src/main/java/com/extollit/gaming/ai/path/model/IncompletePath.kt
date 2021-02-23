package com.extollit.gaming.ai.path.model

import com.extollit.collect.Option

class IncompletePath(val node: INode) : IPath {
    override fun truncateTo(length: Int): Nothing =
        throw ArrayIndexOutOfBoundsException("Cannot truncate incomplete paths")

    override fun untruncate() {}

    override fun length(): Int = 1

    override fun cursor(): Int = 0

    override fun at(i: Int): INode = node

    override fun current(): INode = node

    override fun last(): INode = node

    override fun done(): Boolean = false

    override fun taxiing(): Boolean = false

    override fun taxiUntil(index: Int) {}

    override fun sameAs(other: IPath): Boolean {
        if (other is IncompletePath) return other.node.coordinates == node.coordinates
        val i: Iterator<INode?> = other.iterator()
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

    override fun iterator(): MutableIterator<INode> = Option.of(node).iterator()
}