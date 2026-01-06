package MyApp.misc;

import MyApp.elevator.Elevator;
import lombok.Getter;
import lombok.AllArgsConstructor;

// TODO: javadocs for properties and methods in ElevatorStatus

@Getter
@AllArgsConstructor
public class ElevatorStatus implements Comparable<ElevatorStatus> {
	private final Elevator elevator;
	private final double height;
	private final double velocity;
	private final double breakDistance;
	private final double acceleration;
	private final int queueCount;
	private final int servingDirection;
	
	public ElevatorStatus(final Elevator elevator, final double height, final double velocity, final double breakDistance, final double acceleration, final int queueCount, final int servingDirection) {
		this.elevator = elevator;
		this.height = height;
		this.velocity = velocity;
		this.breakDistance = breakDistance;
		this.acceleration = acceleration;
		this.queueCount = queueCount;
		this.servingDirection = normalizeDirection(servingDirection);
    }
	
	private static int normalizeDirection(final int value) {
		if (value > 0)
			return 1;
		else if (value < 0)
			return -1;
		else
			return 0;
	}

	public double getYPosition(){
		return this.height;
	}

	/**
	 * The velocity (a.k.a. the speed) of the elevator that is used to traveling in the life shaft. <br/>
	 * Positive value means to travel upward, where negative value means to travel downward.
	 * @return The velocity (a.k.a. the speed) of the elevator.
	 */
	public double getVelocity(){
		return this.velocity;
	}

	public int getActualDirection() {
        if (getVelocity() > 0) {
            return 1;
        } else if (getVelocity() < 0) {
            return -1;
        }
        return 0;
    }

	@Override
	public int compareTo(final ElevatorStatus o) {
		return this.getElevator().compareTo(o.getElevator());
	}
}
