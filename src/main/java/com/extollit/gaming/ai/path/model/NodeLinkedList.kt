package com.extollit.gaming.ai.path.model

import com.extollit.gaming.ai.path.model.INode
import com.extollit.gaming.ai.path.model.NodeLinkedList
import kotlin.jvm.JvmOverloads
import com.extollit.gaming.ai.path.model.Passibility
import com.extollit.gaming.ai.path.model.Gravitation
import java.lang.StringBuilder
import java.text.MessageFormat
import java.util.Objects
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
import java.util.LinkedList
import java.util.Collections
import java.lang.IllegalStateException
import java.util.HashSet
import java.util.Deque
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

class NodeLinkedList private constructor(val self: Node, private var next: NodeLinkedList) : Iterable<Node?> {
    constructor(self: Node) : this(self, null) {}

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

    fun remove(child: Node): NodeLinkedList {
        var e = this
        var last: NodeLinkedList? = null
        do {
            if (e.self === child) {
                val tail = e.next
                var head = this
                if (last == null) head = tail else last.next = tail
                return head
            }
            last = e
            e = e.next
        } while (e != null)
        return this
    }

    fun add(child: Node): Boolean {
        var e = this
        var last: NodeLinkedList
        do {
            if (e.self === child) return false
            last = e
        } while (e.next.also { e = it } != null)
        last.next = NodeLinkedList(child)
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