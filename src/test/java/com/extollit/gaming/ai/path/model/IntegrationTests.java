package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.vector.ThreeDimensionalDoubleVector;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalIntVector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.pathObject;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationTests {
    @Mock
    private IPathingEntity pathingEntity;

    @Mock
    private IPathingEntity.Capabilities capabilities;

    @Before
    public void setup() {
        when(pathingEntity.width()).thenReturn(0.6f);
        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(0.5, 0, 0.5));
        when(pathingEntity.capabilities()).thenReturn(capabilities);
    }

    @Test
    public void jackknife() {
        PathObject path = pathObject(
                new ThreeDimensionalIntVector(-1, 4, 10),
                new ThreeDimensionalIntVector(-2, 4, 11),
                new ThreeDimensionalIntVector(-3, 4, 11),
                new ThreeDimensionalIntVector(-4, 4, 11)
        );
        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(-0.5, 4, 10.5));

        path.update(pathingEntity);
        assertEquals(1, path.index);

        verify(pathingEntity).moveTo(new ThreeDimensionalDoubleVector(-1.5, 4, 11.5), Passibility.Passible, Gravitation.grounded);

        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(-1.5, 4, 11.5));

        path = pathObject(
                new ThreeDimensionalIntVector(-1, 4, 10),
                new ThreeDimensionalIntVector(-2, 4, 11),
                new ThreeDimensionalIntVector(-3, 4, 11),
                new ThreeDimensionalIntVector(-4, 4, 11),
                new ThreeDimensionalIntVector(-4, 4, 10),
                new ThreeDimensionalIntVector(-4, 4, 9),
                new ThreeDimensionalIntVector(-3, 4, 9),
                new ThreeDimensionalIntVector(-2, 4, 9),
                new ThreeDimensionalIntVector(-1, 4, 9),
                new ThreeDimensionalIntVector(0, 4, 9),
                new ThreeDimensionalIntVector(1, 4, 9)
        );

        path.update(pathingEntity);
        assertEquals(3, path.index);

        verify(pathingEntity).moveTo(new ThreeDimensionalDoubleVector(-3.5, 4, 11.5), Passibility.Passible, Gravitation.grounded);

        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(-3.5, 4, 11.5));
        path.update(pathingEntity);
        assertEquals(5, path.index);

        verify(pathingEntity).moveTo(new ThreeDimensionalDoubleVector(-3.5, 4, 9.5), Passibility.Passible, Gravitation.grounded);

        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(-3.5, 4, 9.5));
        path.update(pathingEntity);
        assertEquals(10, path.index);
    }

    @Test
    public void taxi() {
        PathObject path = pathObject(
                new ThreeDimensionalIntVector(0, 4, 2),
                new ThreeDimensionalIntVector(0, 4, 3),
                new ThreeDimensionalIntVector(1, 4, 3),
                new ThreeDimensionalIntVector(1, 4, 4),
                new ThreeDimensionalIntVector(2, 4, 4)
        );

        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(0.4, 4.5, 2.4));

        path.taxiUntil(2);

        path.update(pathingEntity);
        assertEquals(1, path.index);

        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(0.4, 4.5, 3.4));
        path.update(pathingEntity);
        assertEquals(2, path.index);

        when(pathingEntity.coordinates()).thenReturn(new ThreeDimensionalDoubleVector(1.4, 4.5, 3.4));
        path.update(pathingEntity);
        assertEquals(4, path.index);
    }
}
