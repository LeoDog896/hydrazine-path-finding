package com.extollit.gaming.ai.path.model

import java.util.LinkedList
import java.util.Deque

internal class TreeTransitional(nextRoot: Node) {
    class RotateNodeOp(val root: Node, val diff: Int) {
        val heads: MutableList<Node?> = LinkedList()
        override fun toString(): String {
            return diff.toString() + ": " + this.root
        }
    }

    private var nextRoot: Node? = null
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