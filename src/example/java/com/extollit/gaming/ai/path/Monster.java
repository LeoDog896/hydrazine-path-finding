package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Gravitation;
import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.gaming.ai.path.model.Passibility;
import com.extollit.gaming.ai.path.vector.ThreeDimensionalDoubleVector;

public class Monster implements IPathingEntity, IPathingEntity.Capabilities {
    public boolean fireResistant, cautious, climber, swimmer, aquatic, avian, aquaphobic, avoidsDoorways, opensDoors;

    private final ThreeDimensionalDoubleVector position = new ThreeDimensionalDoubleVector(0, 0, 0);

    private int age;

    @Override
    public int age() {
        return this.age;
    }

    @Override
    public float searchRange() {
        return 32;
    }

    @Override
    public Capabilities capabilities() {
        return this;
    }

    @Override
    public void moveTo(ThreeDimensionalDoubleVector position, Passibility passibility, Gravitation gravitation) {
        this.position.set(position);
    }

    @Override
    public ThreeDimensionalDoubleVector coordinates() {
        return new ThreeDimensionalDoubleVector(this.position);
    }

    @Override
    public float width() {
        return 0.6f;
    }

    @Override
    public float height() {
        return 1.8f;
    }

    @Override
    public float speed() {
        return 1.0f;
    }

    @Override
    public boolean fireResistant() {
        return this.fireResistant;
    }

    @Override
    public boolean cautious() {
        return this.cautious;
    }

    @Override
    public boolean climber() {
        return this.climber;
    }

    @Override
    public boolean swimmer() {
        return this.swimmer;
    }

    @Override
    public boolean aquatic() {
        return this.aquatic;
    }

    @Override
    public boolean avian() {
        return this.avian;
    }

    @Override
    public boolean aquaphobic() {
        return this.aquaphobic;
    }

    @Override
    public boolean avoidsDoorways() {
        return this.avoidsDoorways;
    }

    @Override
    public boolean opensDoors() {
        return this.opensDoors;
    }

    public void updateTick() {
        this.age++;
    }
}
