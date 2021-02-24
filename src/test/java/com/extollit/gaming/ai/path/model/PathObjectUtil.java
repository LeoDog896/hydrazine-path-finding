package com.extollit.gaming.ai.path.model;

import com.extollit.collect.CollectionsExt;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector;

import java.text.MessageFormat;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class PathObjectUtil {
    public static PathObject pathObject(ThreeDimensionalIntVector... coordinates) {
        final Node[] nodes = new Node[coordinates.length];
        for (int c = 0; c < nodes.length; ++c)
            nodes[c] = new Node(coordinates[c], Passibility.Passible);

        return new PathObject(1, nodes);
    }
    public static void assertPathNot(IPath path, ThreeDimensionalIntVector... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        try {
            compareCoordinates(coordinates, nodesFrom(path));
        } catch (AssertionError e) {
            return;
        }
        throw new AssertionError(MessageFormat.format("Path should not equal {0}", Arrays.asList(coordinates)));
    }

    public static void assertPath(IPath path, ThreeDimensionalIntVector... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        compareCoordinates(coordinates, nodesFrom(path));
    }

    private static void compareCoordinates(ThreeDimensionalIntVector[] coordinates, INode[] nodes) {
        final ThreeDimensionalIntVector[] otherCoords = new ThreeDimensionalIntVector[nodes.length];
        for (int c = 0; c < otherCoords.length; ++c)
            otherCoords[c] = nodes[c].getCoordinates();

        assertArrayEquals(coordinates, otherCoords);
    }

    private static INode[] nodesFrom(IPath path) {
        return CollectionsExt.toList(path).toArray(new INode[path.length()]);
    }
}
