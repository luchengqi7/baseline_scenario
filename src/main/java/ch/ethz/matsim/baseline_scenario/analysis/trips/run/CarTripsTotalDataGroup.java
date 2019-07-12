package ch.ethz.matsim.baseline_scenario.analysis.trips.run;

/* package */class CarTripsTotalDataGroup {
    // fields
    private final double driveTime;
    private final double distance;
    private final int numberOfTrips;

    // constructor
    public CarTripsTotalDataGroup(double driveTime, double distance, int numberOfTrips) {
        // TODO Auto-generated constructor stub
        this.distance = distance;
        this.driveTime = driveTime;
        this.numberOfTrips = numberOfTrips;
    }

    // methods
    public static CarTripsTotalDataGroup addData(CarTripsTotalDataGroup currentData, CarTripsTotalDataGroup newDataToAdd) {
        double newDriveTime = currentData.getDriveTime() + newDataToAdd.getDriveTime();
        double newDistance = currentData.getDistance() + newDataToAdd.getDistance();
        int newNumTrips = currentData.getNumberOfTrips() + newDataToAdd.getNumberOfTrips();
        return new CarTripsTotalDataGroup(newDriveTime, newDistance, newNumTrips);
    }

    public double getDistance() {
        return distance;
    }

    public int getNumberOfTrips() {
        return numberOfTrips;
    }

    public double getDriveTime() {
        return driveTime;
    }
}
