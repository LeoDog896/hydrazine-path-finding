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

internal class TreeTransitional(nextRoot: Node) {
    private class RotateNodeOp(val root: Node, val diff: Int) {
        val heads: MutableList<Node?> = LinkedList()
        override fun toString(): String {
            return diff.toString() + ": " + this.root
        }
    }

    private val nextRoot: Node? = null
    private val dq: Deque<RotateNodeOp>
    fun queue(head: Node?, root: Node?): Boolean {
        for (op in dq) {
            if (op.root === root) {
                op.heads.add(head)
                return true
            }
        }
        return false
    }

    fun finish(queue: SortedNodeQueue) {
        val dq = dq
        var prev = nextRoot
        while (!dq.isEmpty()) {
            val op = dq.pop()
            val next = op.root
            next.bindParent(prev)
            for (head in op.heads) {
                if (head!!.dirty()) queue.addLength(head, op.diff)
                var curr = head.parent()
                while (curr != null && curr !== next && curr.dirty()) {
                    curr.addLength(op.diff)
                    curr = curr.parent()
                }
                if (next.dirty()) next.addLength(op.diff)
            }
            prev = next
        }
    }

    init {
        val dq: Deque<RotateNodeOp> = LinkedList()
        var curr = nextRoot.also { this.nextRoot = it }.parent()
        var length = nextRoot.length().toInt()
        var newLength0 = 0
        while (curr != null) {
            val up = curr!!.parent()
            val length0 = length
            length = curr!!.length().toInt()
            curr!!.orphan()
            curr!!.dirty(true)
            val dl = newLength0 + (length0 - length) - length
            dq.add(RotateNodeOp(curr!!, dl))
            newLength0 = length + dl
            curr = up
        }
        this.dq = dq
        this.nextRoot!!.orphan()
    }
}