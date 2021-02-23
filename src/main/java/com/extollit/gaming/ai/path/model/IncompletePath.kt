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
import com.extollit.collect.Option
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

class IncompletePath(node: INode?) : IPath {
    val node: INode
    override fun truncateTo(length: Int) {
        throw ArrayIndexOutOfBoundsException("Cannot truncate incomplete paths")
    }

    override fun untruncate() {}
    override fun length(): Int {
        return 1
    }

    override fun cursor(): Int {
        return 0
    }

    override fun at(i: Int): INode {
        return node
    }

    override fun current(): INode {
        return node
    }

    override fun last(): INode {
        return node
    }

    override fun done(): Boolean {
        return false
    }

    override fun taxiing(): Boolean {
        return false
    }

    override fun taxiUntil(index: Int) {}
    override fun sameAs(other: IPath): Boolean {
        if (other is IncompletePath) return other.node.coordinates() == node.coordinates()
        val i: Iterator<INode?> = other.iterator()
        return (!i.hasNext() || node.coordinates() == i.next()!!.coordinates()) && !i.hasNext()
    }

    override fun stagnantFor(subject: IPathingEntity): Float {
        return 0f
    }

    override fun update(pathingEntity: IPathingEntity) {}
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val iNodes = o as IncompletePath
        return node == iNodes.node
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

    override fun toString(): String {
        return "*" + node.coordinates() + "...?"
    }

    override fun iterator(): MutableIterator<INode> {
        return Option.of(node).iterator()
    }

    init {
        if (node == null) throw NullPointerException()
        this.node = node
    }
}