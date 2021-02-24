package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.node.INodeCalculator;
import com.extollit.gaming.ai.path.node.Node;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector;

public class TestNodeCalculatorDecorator implements INodeCalculator {
    public final INodeCalculator delegate;

    public TestNodeCalculatorDecorator(INodeCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public void applySubject(IPathingEntity subject) {
        this.delegate.applySubject(subject);
    }

    @Override
    public Node passibleNodeNear(ThreeDimensionalIntVector coords0, ThreeDimensionalIntVector origin, FlagSampler flagSampler) {
        final Node node = this.delegate.passibleNodeNear(coords0, origin, flagSampler);
        return node == null ? new Node(coords0) : node;
    }

    @Override
    public boolean omnidirectional() {
        return this.delegate.omnidirectional();
    }
}
