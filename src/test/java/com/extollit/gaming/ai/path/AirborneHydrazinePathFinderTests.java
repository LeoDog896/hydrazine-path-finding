package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.node.path.IPath;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector;
import org.junit.Before;
import org.junit.Test;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class AirborneHydrazinePathFinderTests extends AbstractHydrazinePathFinderTests {
    @Before
    public void setup() {
        when(super.capabilities.cautious()).thenReturn(false);
        when(super.capabilities.avian()).thenReturn(true);

        super.setup();

        pathFinder.schedulingPriority(125, 125);
    }

    @Test
    public void takeOffEh() {
        solid(0, -1, 0);
        solid(0, -1, 1);
        solid(0, -1, 2);
        solid(0, -1, 3);
        solid(0, -1, 4);

        final IPath path = pathFinder.initiatePathTo(0, 5, 4);

        assertNotNull(path);
        assertEquals(new ThreeDimensionalIntVector(0, 5, 4), path.last().getCoordinates());
    }

    @Test
    public void headSpace() {
        solid(0, 1, 2);

        final IPath path = pathFinder.initiatePathTo(0, 0, 4);

        assertPath(
                path,
                new ThreeDimensionalIntVector(0, 0, 0),
                new ThreeDimensionalIntVector(-1, 0, 0),
                new ThreeDimensionalIntVector(-1, 0, 1),
                new ThreeDimensionalIntVector(-1, 0, 2),
                new ThreeDimensionalIntVector(-1, 0, 3),
                new ThreeDimensionalIntVector(0, 0, 3),
                new ThreeDimensionalIntVector(0, 0, 4)
        );
    }
}
