package MyApp.misc;

import MyApp.elevator.Elevator;
import lombok.Getter;
import lombok.AllArgsConstructor;

// TODO: javadocs for properties and methods in ElevatorStatus

@Getter
public class ElevatorStatus implements Comparable<ElevatorStatus> {
	private final Elevator elevator;
	private final double height;
	/**
	 *  The velocity (a.k.a. the speed) of the elevator that is used to traveling in the life shaft. <br/>
	 *  Positive value means to travel upward, where negative value means to travel downward.
	 */
	private final double velocity;
	private final double brakeDistance;
	private final double acceleration;
	private final int queueCount;
	private final int servingDirection;

	public ElevatorStatus(final Elevator elevator, final double height, final double velocity, final double brakeDistance, final double acceleration, final int queueCount, final int servingDirection) {
		this.elevator = elevator;
		this.height = height;
		this.velocity = velocity;
		this.brakeDistance = brakeDistance;
		this.acceleration = acceleration;
		this.queueCount = queueCount;
		this.servingDirection = normalizeDirection(servingDirection);
    }
	
	private static int normalizeDirection(final int value) {
		return Integer.compare(value, 0);
	}

	public double getYPosition(){
		return this.height;
	}

	public int getActualDirection() {
		return Double.compare(getVelocity(), 0);
    }

	@Override
	public int compareTo(final ElevatorStatus o) {
		return this.getElevator().compareTo(o.getElevator());
	}
}
