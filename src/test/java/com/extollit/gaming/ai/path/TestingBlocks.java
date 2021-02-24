package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IBlockObject;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalDoubleBox;

public class TestingBlocks {
    public static final IBlockObject
            stone = new Stone(),
            wall = new Wall(),
            lava = new Lava(),
            air = new Air(),
            ladder = new Ladder(),
            slabUp = new SlabUp(),
            slabDown = new SlabDown(),
            torch = air;

    public static final Door
            door = new Door();
    public static final FenceGate
            fenceGate = new FenceGate();

    private static class AbstractBlockDescription implements IBlockObject {
        @Override
        public boolean getFenceLike() {
            return false;
        }

        @Override
        public boolean getClimbable() {
            return false;
        }

        @Override
        public boolean getDoor() {
            return false;
        }

        @Override
        public boolean getImpeding() {
            return false;
        }

        @Override
        public boolean getFullyBounded() {
            return false;
        }

        @Override
        public boolean getLiquid() {
            return false;
        }

        @Override
        public boolean getIncinerating() {
            return false;
        }

        @Override
        public ThreeDimensionalDoubleBox bounds() {
            return new ThreeDimensionalDoubleBox(
                0, 0, 0,
                1, getFenceLike() ? 1.5f : 0, 1
            );
        }
    }

    public static class Stone extends AbstractBlockDescription {
        @Override
        public boolean getImpeding() {
            return true;
        }

        @Override
        public boolean getFullyBounded() {
            return true;
        }

        @Override
        public ThreeDimensionalDoubleBox bounds() {
            return new ThreeDimensionalDoubleBox(
                    0, 0, 0,
                    1, 1, 1
            );
        }
    }

    public static class Wall extends Stone {
        @Override
        public boolean getFenceLike() {
            return true;
        }

        @Override
        public boolean getFullyBounded() {
            return false;
        }
    }

    public static final class Lava extends AbstractBlockDescription {
        @Override
        public boolean getLiquid() {
            return true;
        }

        @Override
        public boolean getIncinerating() {
            return true;
        }
    }

    public static final class Air extends AbstractBlockDescription {}

    private static abstract class AbstractDoor extends AbstractBlockDescription {
        public boolean open;

        @Override
        public final boolean getDoor() {
            return true;
        }

        @Override
        public final boolean getImpeding() {
            return !open;
        }

        @Override
        public ThreeDimensionalDoubleBox bounds() {
            return new ThreeDimensionalDoubleBox(
                    0, 0, 0,
                    1, 1, 1
            );
        }
    }

    public static final class FenceGate extends AbstractDoor {
        @Override
        public boolean getFenceLike() {
            return true;
        }
    }

    public static final class Door extends AbstractDoor {}

    public static final class Ladder extends AbstractBlockDescription {
        @Override
        public boolean getClimbable() {
            return true;
        }
    }

    public static final class SlabDown extends AbstractBlockDescription {
        @Override
        public boolean getImpeding() {
            return true;
        }
        @Override
        public ThreeDimensionalDoubleBox bounds() {
            return new ThreeDimensionalDoubleBox(
                    0, 0, 0,
                    1, 0.5, 1
            );
        }
    }

    public static final class SlabUp extends AbstractBlockDescription {
        @Override
        public boolean getImpeding() {
            return true;
        }

        @Override
        public ThreeDimensionalDoubleBox bounds() {
            return new ThreeDimensionalDoubleBox(
                    0, 0.5, 0,
                    1, 1, 1
            );
        }
    }
}
