package com.extollit.gaming.ai.path.model;

import com.extollit.collect.CollectionsExt;
import com.extollit.gaming.ai.path.node.Node;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalDoubleVector;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.pathObject;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PathObjectTests {
    private final PathObject
        pathAlpha = pathObject(
            new ThreeDimensionalIntVector(-10, 42, -10),
            new ThreeDimensionalIntVector(-10, 42, -9),
            new ThreeDimensionalIntVector(-10, 41, -8),
            new ThreeDimensionalIntVector(-10, 41, -7),
            new ThreeDimensionalIntVector(-10, 41, -6),
            new ThreeDimensionalIntVector(-10, 41, -5),
            new ThreeDimensionalIntVector(-10, 40, -4),
            new ThreeDimensionalIntVector(-10, 40, -3),
            new ThreeDimensionalIntVector(-10, 39, -2),
            new ThreeDimensionalIntVector(-10, 39, -1),
            new ThreeDimensionalIntVector(-10, 39, 0),
            new ThreeDimensionalIntVector(-10, 38, 1),
            new ThreeDimensionalIntVector(-10, 38, 2),
            new ThreeDimensionalIntVector(-10, 37, 3),
            new ThreeDimensionalIntVector(-9, 37, 3),
            new ThreeDimensionalIntVector(-8, 37, 3),
            new ThreeDimensionalIntVector(-8, 37, 4),
            new ThreeDimensionalIntVector(-8, 36, 5)
        ),
        pathBeta = pathObject(
            new ThreeDimensionalIntVector(-10, 42, -10),
            new ThreeDimensionalIntVector(-10, 42, -9),
            new ThreeDimensionalIntVector(-10, 41, -8),
            new ThreeDimensionalIntVector(-10, 41, -7),
            new ThreeDimensionalIntVector(-10, 41, -6),
            new ThreeDimensionalIntVector(-10, 41, -5),
            new ThreeDimensionalIntVector(-10, 40, -4),
            new ThreeDimensionalIntVector(-9, 40, -4),
            new ThreeDimensionalIntVector(-8, 40, -4),
            new ThreeDimensionalIntVector(-8, 40, -3),
            new ThreeDimensionalIntVector(-8, 39, -2),
            new ThreeDimensionalIntVector(-7, 39, -2),
            new ThreeDimensionalIntVector(-7, 39, -1),
            new ThreeDimensionalIntVector(-6, 39, -1),
            new ThreeDimensionalIntVector(-6, 38, 0),
            new ThreeDimensionalIntVector(-6, 38, 1),
            new ThreeDimensionalIntVector(-6, 37, 2),
            new ThreeDimensionalIntVector(-6, 37, 3)
        );

    @Mock private IPathingEntity pathingEntity;
    @Mock private IPathingEntity.Capabilities capabilities;

    private int time;

    @Before
    public void setup() {
        when(pathingEntity.width()).thenReturn(0.6f);
        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(0.5, 0, 0.5));
        when(pathingEntity.capabilities()).thenReturn(capabilities);

        pathAlpha.index = pathBeta.index = 0;
        this.time = 0;
    }

    private void pos(double x, double y, double z) {
        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(x, y, z));
    }
    private void tick(int delta) {
        this.time += delta;
        when(pathingEntity.age()).thenReturn(this.time);
    }

    @Test
    public void updateMutationState() {
        final PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(0, 0, 0),
                new ThreeDimensionalIntVector(1, 0, 0),
                new ThreeDimensionalIntVector(2, 0, 0),
                new ThreeDimensionalIntVector(2, 0, 1)
        );
        tick(42);
        assertEquals(0, pathObject.stagnantFor(pathingEntity), 0.01);

        pathObject.update(pathingEntity);
        tick(52);
        assertEquals(94 - 42, pathObject.stagnantFor(pathingEntity), 0.01);

        pos(2.5, 0.2, 1.5);

        pathObject.update(pathingEntity);

        assertEquals(0, pathObject.stagnantFor(pathingEntity), 0.01);
    }

    @Test
    public void update() {
        final PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(0, 0, 0),
                new ThreeDimensionalIntVector(1, 0, 0),
                new ThreeDimensionalIntVector(2, 0, 0),
                new ThreeDimensionalIntVector(2, 0, 1),
                new ThreeDimensionalIntVector(3, 0, 1),
                new ThreeDimensionalIntVector(4, 1, 1),
                new ThreeDimensionalIntVector(4, 1, 2),
                new ThreeDimensionalIntVector(5, 1, 2),
                new ThreeDimensionalIntVector(4, 1, 2),
                new ThreeDimensionalIntVector(3, 1, 3)
        );
        pathObject.update(pathingEntity);

        assertEquals(4, pathObject.index);
    }

    @Test
    public void updateLateStage() {
        final PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(0, 0, 0),
                new ThreeDimensionalIntVector(1, 0, 0),
                new ThreeDimensionalIntVector(2, 0, 0),
                new ThreeDimensionalIntVector(2, 0, 1),
                new ThreeDimensionalIntVector(3, 0, 1),
                new ThreeDimensionalIntVector(4, 0, 1),
                new ThreeDimensionalIntVector(4, 0, 2),
                new ThreeDimensionalIntVector(5, 0, 2),
                new ThreeDimensionalIntVector(6, 0, 2),
                new ThreeDimensionalIntVector(6, 0, 3)
        );
        pos(4.5, 0, 1.5);

        pathObject.update(pathingEntity);

        assertEquals(6, pathObject.index);
    }

    @Test
    public void waterStuck() {
        final PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(6, 4, 7),
                new ThreeDimensionalIntVector(7, 4, 7),
                new ThreeDimensionalIntVector(8, 4, 7),
                new ThreeDimensionalIntVector(9, 4, 7)
        );
        when(pathingEntity.width()).thenReturn(0.3f);
        pos(6.5, 4.1, 7.5);

        pathObject.update(pathingEntity);

        assertEquals(3, pathObject.index);
    }

    @Test
    public void truncation() {
        final PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(2, 5, 3),
                new ThreeDimensionalIntVector(7, 8, 2),
                new ThreeDimensionalIntVector(9, 2, 6),
                new ThreeDimensionalIntVector(5, 7, 9),
                new ThreeDimensionalIntVector(1, 5, 3)
        );

        pathObject.truncateTo(3);

        assertEquals(3, pathObject.length());
        List<ThreeDimensionalIntVector> actual = new ArrayList<>();
        for (Node node : pathObject)
            actual.add(node.getCoordinates());

        assertEquals(
            Arrays.asList(
                new ThreeDimensionalIntVector(2, 5, 3),
                new ThreeDimensionalIntVector(7, 8, 2),
                new ThreeDimensionalIntVector(9, 2, 6)
            ),
            actual
        );
    }

    @Test
    public void untruncation() {
        final PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(2, 5, 3),
                new ThreeDimensionalIntVector(7, 8, 2),
                new ThreeDimensionalIntVector(9, 2, 6),
                new ThreeDimensionalIntVector(5, 7, 9),
                new ThreeDimensionalIntVector(1, 5, 3)
        );

        pathObject.truncateTo(3);
        pathObject.untruncate();

        assertEquals(5, pathObject.length());
        assertEquals(
                Arrays.asList(pathObject.nodes),
                CollectionsExt.toList(pathObject)
        );
    }

    @Test
    public void positionFor1() {
        when(pathingEntity.width()).thenReturn(0.8f);

        final ThreeDimensionalDoubleVector pos = PathObject.positionFor(pathingEntity, new ThreeDimensionalIntVector(1, 2, 3));

        assertEquals(new ThreeDimensionalDoubleVector(1.5, 2, 3.5), pos);
    }


    @Test
    public void positionFor2() {
        when(pathingEntity.width()).thenReturn(1.4f);

        final ThreeDimensionalDoubleVector pos = PathObject.positionFor(pathingEntity, new ThreeDimensionalIntVector(1, 2, 3));

        assertEquals(new ThreeDimensionalDoubleVector(1, 2, 3), pos);
    }

    @Test
    public void positionFor3() {
        when(pathingEntity.width()).thenReturn(2.3f);

        final ThreeDimensionalDoubleVector pos = PathObject.positionFor(pathingEntity, new ThreeDimensionalIntVector(1, 2, 3));

        assertEquals(new ThreeDimensionalDoubleVector(1.5, 2, 3.5), pos);
    }

    @Test
    public void dontAdvanceBigBoiTooMuch() {
        when(pathingEntity.width()).thenReturn(1.4f);
        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(-0.45f, 0, 0));
        final PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(0, 0, 0),
                new ThreeDimensionalIntVector(-1, 0, 0),
                new ThreeDimensionalIntVector(-1, 0, 1),
                new ThreeDimensionalIntVector(0, 1, 1)
        );

        pathObject.update(pathingEntity);

        assertEquals(2, pathObject.index);
    }

    @Test
    public void dontDoubleBack() {
        PathObject pathObject = pathObject(
                new ThreeDimensionalIntVector(1, 0, 0),
                new ThreeDimensionalIntVector(2, 0, 0),
                new ThreeDimensionalIntVector(3, 0, 0),
                new ThreeDimensionalIntVector(4, 0, 0),
                new ThreeDimensionalIntVector(5, 0, 0)
        );
        pos(3.5, 0, 0.5);
        pathObject.update(pathingEntity);

        assertEquals(4, pathObject.index);
        verify(pathingEntity).moveTo(new ThreeDimensionalDoubleVector(5.5, 0, 0.5), Passibility.Passible, Gravitation.grounded);

        tick(100);
        pos(3.5, 0, 1.5);

        pathObject.update(pathingEntity);

        assertEquals(2, pathObject.index);
        verify(pathingEntity).moveTo(new ThreeDimensionalDoubleVector(3.5, 0, 0.5), Passibility.Passible, Gravitation.grounded);
    }


    @Test
    public void nonRepudiantUpdate() {
        PathObject path = pathObject(
                new ThreeDimensionalIntVector(-2, 4, 11),
                new ThreeDimensionalIntVector(-3, 4, 11),
                new ThreeDimensionalIntVector(-4, 4, 11),
                new ThreeDimensionalIntVector(-5, 4, 11),
                new ThreeDimensionalIntVector(-5, 4, 10),
                new ThreeDimensionalIntVector(-4, 4, 10),
                new ThreeDimensionalIntVector(-3, 4, 10),
                new ThreeDimensionalIntVector(-2, 4, 10),
                new ThreeDimensionalIntVector(-1, 4, 10),
                new ThreeDimensionalIntVector(0, 4, 10)
        );

        pos(-1.5, 4, 11.5);
        path.update(pathingEntity);
        final int first = path.index;

        path.update(pathingEntity);
        assertEquals(first, path.index);
    }

    @Test
    public void approximateAdjacent() {
        PathObject path = pathObject(
                new ThreeDimensionalIntVector(0, 1, 0),
                new ThreeDimensionalIntVector(1, 1, 0)
        );
        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(0.4, 0.5, 0.4));

        path.update(pathingEntity);
        assertEquals(1, path.index);
    }

    @Test
    public void stairMaster() {
        PathObject path = pathObject(
                new ThreeDimensionalIntVector(13, 4, 6),
                new ThreeDimensionalIntVector(12, 5, 6),
                new ThreeDimensionalIntVector(11, 5, 6),
                new ThreeDimensionalIntVector(11, 4, 7),
                new ThreeDimensionalIntVector(10, 4, 7)
        );
        pos(13.5, 4, 6.5);
        path.update(pathingEntity);

        pos(11.4, 5, 7.3);
        path.update(pathingEntity);

        assertEquals(4, path.index);
    }

    @Test
    public void disparatePathAdjustment() {
        pathAlpha.index = 11;

        pos(-9.5, 38.0, 1.5);
        pathBeta.adjustPathPosition(pathAlpha, pathingEntity);

        assertEquals(10, pathBeta.index);
    }

    @Test
    public void unreachablePath() {
        pathAlpha.index = 11;
        assertFalse(pathBeta.reachableFrom(pathAlpha));
    }

    @Test
    public void fatOscillatingTaxi() {
        PathObject path = pathObject(
                new ThreeDimensionalIntVector(2, 4, 0),
                new ThreeDimensionalIntVector(2, 4, 1),
                new ThreeDimensionalIntVector(3, 4, 1),
                new ThreeDimensionalIntVector(4, 4, 1),
                new ThreeDimensionalIntVector(4, 4, 0),
                new ThreeDimensionalIntVector(4, 4, -1),
                new ThreeDimensionalIntVector(5, 4, -1),
                new ThreeDimensionalIntVector(6, 4, -1)
        );

        pos(2.1, 4, 0.5);
        path.update(pathingEntity);
        assertEquals(1, path.index);

        tick(1);

        pos(2.1, 4, 1.3);
        path.update(pathingEntity);

        assertEquals(3, path.index);

        tick(1);

        pos(2.1, 4, 0.9);
        path.update(pathingEntity);
        assertEquals(1, path.index);

        tick(1);

        pos(2.1, 4, 1.3);
        path.update(pathingEntity);

        assertEquals(3, path.index);

        tick(1);

        pos(2.1, 4, 0.9);
        path.update(pathingEntity);
        assertEquals(1, path.index);

        tick(1);

        pos(2.1, 4, 1.3);
        path.update(pathingEntity);

        assertTrue(path.taxiing());
        assertEquals(2, path.index);
    }

    @Test
    public void deviantRocket() {
        when(capabilities.avian()).thenReturn(true);

        final PathObject path = pathObject(
                new ThreeDimensionalIntVector(0, 0, 0),
                new ThreeDimensionalIntVector(0, 1, 0),
                new ThreeDimensionalIntVector(0, 2, 0),
                new ThreeDimensionalIntVector(0, 3, 0),
                new ThreeDimensionalIntVector(0, 4, 0)
        );

        pos(0.2, 0.2, 0.2);
        path.update(pathingEntity);
        assertEquals(4, path.index);
    }
}
