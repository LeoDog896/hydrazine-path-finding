package com.extollit.gaming.ai.path.model;

import com.extollit.collect.CollectionsExt;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NodeTests {
    private Node root;

    @Before
    public void setup() {
        this.root = new Node(new Vec3i(1, 2, 3));
    }

    @Test
    public void reset() {
        final Node node = this.root;

        node.length(7);
        node.remaining(11);
        node.passibility(Passibility.Risky);
        node.index(42);
        node.volatile_(true);
        node.visited(true);

        node.reset();

        assertEquals(0, node.length());
        assertEquals(0, node.remaining());
        assertEquals(0, node.journey());
        assertEquals(Passibility.Risky, node.passibility());
        assertFalse(node.assigned());
        assertTrue(node.dirty());
        assertFalse(node.visited());
        assertTrue(node.volatile_());
    }

    @Test
    public void sterilize() {
        final Node
            alphaChild = new Node(new Vec3i(2, 2, 3)),
            betaChild = new Node(new Vec3i(0, 2, 3));

        alphaChild.appendTo(this.root, 0, 0);
        betaChild.appendTo(this.root, 0, 0);

        assertSame(this.root, alphaChild.getParent());
        assertSame(this.root, betaChild.getParent());

        assertFalse(alphaChild.orphaned());
        assertFalse(betaChild.orphaned());

        this.root.sterilize();

        assertTrue(alphaChild.orphaned());
        assertTrue(betaChild.orphaned());
    }

    @Test
    public void orphan() {
        final Node
                child = new Node(new Vec3i(2, 2, 3));

        child.appendTo(this.root, 0, 0);

        assertSame(this.root, child.getParent());
        assertFalse(child.orphaned());

        child.orphan();

        assertTrue(child.orphaned());
        assertNull(child.getParent());
        assertTrue(this.root.infecund());
    }

    @Test
    public void appendTo() {
        final Node subject = new Node(new Vec3i(2, 2, 3));
        subject.appendTo(this.root, 1, 3);

        final Node newNode = new Node(new Vec3i(4, 5, 6));
        subject.appendTo(newNode, 1, 3);

        assertTrue(this.root.infecund());
        assertTrue(CollectionsExt.toList(newNode.children()).contains(subject));
    }

    @Test
    public void lengthSetDirtiness() {
        final Node subject = new Node(new Vec3i(1, 2, 3));

        subject.dirty(true);
        assertTrue(subject.dirty());
        subject.length(42);
        assertFalse(subject.dirty());
    }

    @Test
    public void lengthDirty() {
        final Node subject = new Node(new Vec3i(1, 2, 3));

        subject.dirty(true);
        assertTrue(subject.dirty());
        subject.dirty(false);
        assertFalse(subject.dirty());
    }
}
