package com.extollit.gaming.ai.path.model

import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector

interface INodeCalculator {
    fun applySubject(subject: IPathingEntity)
    fun passibleNodeNear(coords0: ThreeDimensionalIntVector, origin: ThreeDimensionalIntVector?, flagSampler: FlagSampler): Node
    fun omnidirectional(): Boolean
}