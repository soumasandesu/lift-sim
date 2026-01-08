package MyApp.building;

import java.io.Serializable;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * Represents a floor that separates a building in a vertical spaces.
 */
@Getter
@AllArgsConstructor
public final class Floor implements Serializable, Comparable<Floor> {
    /**
     * Human-readable alias of the floor.
     */
    private final String name;

    /**
     * The vertical displacement between the ground of such floor and the sea level.
     */
    private final double yDisplacement;

    /**
     * The lower floor of this floor.
     */
    private Floor lowerFloor = null;
    /**
     * The upper floor of this floor.
     */
    private Floor upperFloor = null;

    /**
     * Creates a floor object.
     * @param name Human-readable alias of the floor.
     * @param yDisplacement The vertical displacement between the ground of such floor and the sea level.
     */
    public Floor(final String name, final double yDisplacement) {
        this.name = name;
        this.yDisplacement = yDisplacement;
    }

    /**
     * Gets the vertical displacement between the ground of such floor and the sea level.
     * @return The vertical displacement between the ground of such floor and the sea level.
     */
    public double getYPosition() {
        return yDisplacement;
    }

    /**
     * Sets the lower floor.
     * @param lowerFloor The lower floor.
     */
    public void setLowerFloor(final Floor lowerFloor) {
        this.lowerFloor = lowerFloor;
        if (lowerFloor == null) return;
        this.lowerFloor.upperFloor = this;
    }

    /**
     * Sets the upper floor.
     * @param upperFloor The upper floor.
     */
    public void setUpperFloor(final Floor upperFloor) {
        this.upperFloor = upperFloor;
        if (upperFloor == null) return;
        this.upperFloor.lowerFloor = this;
    }

    /**
     * Comparing this floor with another.
     * @param comparing The another floor.
     * @return Negative if this, or positive if another is upper floor.
     */
    @Override
    public int compareTo(final Floor comparing) {
        if (this.equals(comparing)) return 0;
        return (int)this.yDisplacement - (int)comparing.yDisplacement;
    }

    /**
     * Comparing if this and another floor is the same floor.
     * @param obj Another floor to compare.
     * @return If these floors are same.
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Floor floor &&
                this.yDisplacement == floor.yDisplacement &&
                this.name.equals(floor.name);
    }
}
