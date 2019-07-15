package ch.ethz.matsim.baseline_scenario.analysis.trips.run;

class AVTripsTotalDataGroup extends CarTripsTotalDataGroup {
    // additional fileds for AVTripsTotalDataGroup
    final double meanWaitTime;
    final double medianWaitTime;

    // constructor
    public AVTripsTotalDataGroup//
    (double driveTime, double distance, int numberOfTrips, double meanWaitTime, double medianWaitTime, int numberOfSeeds) {
        super(driveTime, distance, numberOfTrips, numberOfSeeds);
        this.meanWaitTime = meanWaitTime;
        this.medianWaitTime = medianWaitTime;
    }

    // additional/override methods
    public double getMeanWaitTime() {
        return meanWaitTime;
    }

    public double getMedianWaitTime() {
        return medianWaitTime;
    }

    public static AVTripsTotalDataGroup addData(AVTripsTotalDataGroup data1, AVTripsTotalDataGroup data2) {
        double newDriveTime = data1.getDriveTime() + data2.getDriveTime();
        double newDistance = data1.getDistance() + data2.getDistance();
        int newNumTrips = data1.getNumberOfTrips() + data2.getNumberOfTrips();
        double newWaitTime = data1.getMeanWaitTime() + data2.getMeanWaitTime();
        double newmedianWaitTime = data1.getMedianWaitTime() + data2.getMedianWaitTime();
        int newNumberOfSeeds = data1.getNumberOfSeeds() + data2.getNumberOfSeeds();

        return new AVTripsTotalDataGroup(newDriveTime, newDistance, newNumTrips, newWaitTime, newmedianWaitTime, newNumberOfSeeds);
    }

}
