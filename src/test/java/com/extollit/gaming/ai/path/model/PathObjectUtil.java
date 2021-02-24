package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.node.Node;
import com.extollit.gaming.ai.path.node.path.IPath;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector;

import java.text.MessageFormat;
import java.util.ArrayList;
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

    private static void compareCoordinates(ThreeDimensionalIntVector[] coordinates, Node[] nodes) {
        final ThreeDimensionalIntVector[] otherCoords = new ThreeDimensionalIntVector[nodes.length];
        for (int c = 0; c < otherCoords.length; ++c)
            otherCoords[c] = nodes[c].getCoordinates();

        assertArrayEquals(coordinates, otherCoords);
    }

    private static Node[] nodesFrom(IPath path) {

        ArrayList<Node> list = new ArrayList<>(path.length());

        for (Node item : path)
            list.add(item);

        return list.toArray(new Node[path.length()]);
    }
}
