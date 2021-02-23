package com.extollit.gaming.ai.path.model

import com.extollit.linalg.immutable.Vec3i

interface INodeCalculator {
    fun applySubject(subject: IPathingEntity)
    fun passibleNodeNear(coords0: Vec3i, origin: Vec3i?, flagSampler: FlagSampler): Node
    fun omnidirectional(): Boolean
}