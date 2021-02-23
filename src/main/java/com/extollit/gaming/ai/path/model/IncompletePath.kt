package com.extollit.gaming.ai.path.model

import com.extollit.collect.Option

class IncompletePath(val node: INode) : IPath {
    override fun truncateTo(length: Int) =
        throw ArrayIndexOutOfBoundsException("Cannot truncate incomplete paths")

    override fun untruncate() {}

    override fun length() = 1

    override fun cursor() = 0

    override fun at(i: Int) = node

    override fun current() = node

    override fun last() = node

    override fun done() = false

    override fun taxiing() = false

    override fun taxiUntil(index: Int) {}

    override fun sameAs(other: IPath): Boolean {
        if (other is IncompletePath) return other.node.coordinates() == node.coordinates()
        val i: Iterator<INode?> = other.iterator()
        return (!i.hasNext() || node.coordinates() == i.next()!!.coordinates()) && !i.hasNext()
    }

    override fun stagnantFor(subject: IPathingEntity) = 0f

    override fun update(pathingEntity: IPathingEntity) {}
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val iNodes = other as IncompletePath
        return node == iNodes.node
    }

    override fun hashCode() = node.hashCode()

    override fun toString() = "*" + node.coordinates() + "...?"

    override fun iterator() = Option.of(node).iterator()
}