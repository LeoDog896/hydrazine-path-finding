package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IBlockObject;
import com.extollit.linalg.immutable.AxisAlignedBBox;

public class BlockObject implements IBlockObject {
    public AxisAlignedBBox bounds;
    public boolean fenceLike, climbable, door, impeding, fullyBounded, liquid, incinerating;

    @Override
    public AxisAlignedBBox bounds() {
        return this.bounds;
    }

    @Override
    public boolean getFenceLike() {
        return this.fenceLike;
    }

    @Override
    public boolean getClimbable() {
        return this.climbable;
    }

    @Override
    public boolean getDoor() {
        return this.door;
    }

    @Override
    public boolean getImpeding() {
        return this.impeding;
    }

    @Override
    public boolean getFullyBounded() {
        return this.fullyBounded;
    }

    @Override
    public boolean getLiquid() {
        return this.liquid;
    }

    @Override
    public boolean getIncinerating() {
        return this.incinerating;
    }
}
