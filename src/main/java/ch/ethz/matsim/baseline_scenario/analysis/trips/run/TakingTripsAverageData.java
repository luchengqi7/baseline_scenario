package ch.ethz.matsim.baseline_scenario.analysis.trips.run;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/*package*/ enum TakingTripsAverageData {
    ;

    // TODO recognize number of seeds automatically (which allows various seeds number for different setups)
    private static final double[] rtShares = { 0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.75 };

    static void run(File simResultsDirectory) throws IOException {
        System.out.println("Now taking average trip data");

        Map<Double, CarTripsTotalDataGroup> dataMapCar = new HashMap<>();
        Map<Double, AVTripsTotalDataGroup> dataMapAV = new HashMap<>();

        for (File resultFolder : simResultsDirectory.listFiles()) {
            System.out.println("now processing: " + resultFolder.getPath());
            // Read simulation info
            Properties simulationInfo = new Properties();
            try {
                simulationInfo.load(new FileInputStream//
                (resultFolder.getPath() + "/" + resultFolder.getName() + ".properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Load car trips data
            Properties carTripsData = new Properties();
            try {
                carTripsData.load(new FileInputStream//
                (resultFolder.getPath() + "/analyzedResult.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Load av trips data
            Properties avTripsData = new Properties();
            try {
                avTripsData.load(new FileInputStream//
                (resultFolder.getPath() + "/data/totalSimulationValues.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            double rtShare = Double.parseDouble(simulationInfo.getProperty("rtshare"));

            // store car trips gross data in data map
            double carMeanDriveTime = Double.parseDouble(carTripsData.getProperty("meanTravelTime"));
            double carTotalDistance = Double.parseDouble(carTripsData.getProperty("totalTravelDistance"));
            int numberOfCarTrips = Integer.parseInt(carTripsData.getProperty("numberOfCarTrip"));
            double carGrossDriveTime = carMeanDriveTime * numberOfCarTrips;
            CarTripsTotalDataGroup carTripsTotalDataGroup = //
                    new CarTripsTotalDataGroup(carGrossDriveTime, carTotalDistance, numberOfCarTrips, 1);
            if (!dataMapCar.containsKey(rtShare)) {
                dataMapCar.put(rtShare, carTripsTotalDataGroup);
            } else {
                CarTripsTotalDataGroup newCarTripsDataGroup = //
                        CarTripsTotalDataGroup.addData(carTripsTotalDataGroup, dataMapCar.get(rtShare));
                dataMapCar.put(rtShare, newCarTripsDataGroup);
            }
            // Store av trips data in data map
            double avMeanDriveTime = Double.parseDouble(avTripsData.getProperty("MeanDriveTime")) - 25;
            double avTotalDistance = Double.parseDouble(avTripsData.getProperty("TotalRoboTaxiDistance"));
            int numOfAVTrips = Integer.parseInt(avTripsData.getProperty("totalRequests"));
            double avMeanWaitTime = Double.parseDouble(avTripsData.getProperty("MeanWaitingTime"));
            double avMedianWaitTime = Double.parseDouble(avTripsData.getProperty("WaitTimeQuantile2"));
            double avGrossDriveTime = avMeanDriveTime * numOfAVTrips;
            double avGrossWaitTime = avMeanWaitTime * numOfAVTrips;

            AVTripsTotalDataGroup avTripsTotalDataGroup = new AVTripsTotalDataGroup//
            (avGrossDriveTime, avTotalDistance, numOfAVTrips, avGrossWaitTime, avMedianWaitTime, 1);

            if (!dataMapAV.containsKey(rtShare)) {
                dataMapAV.put(rtShare, avTripsTotalDataGroup);
            } else {
                AVTripsTotalDataGroup newAVTripsTotalDataGroup = AVTripsTotalDataGroup.addData//
                (avTripsTotalDataGroup, dataMapAV.get(rtShare));
                dataMapAV.put(rtShare, newAVTripsTotalDataGroup);
            }
            // internal double check
            int totalTrips = numberOfCarTrips + numOfAVTrips;
            if (totalTrips != 36148) { // Sioux Falls population cutter seeds 1234 --> 36148 total requests
                System.out.println(totalTrips);
                System.err.println("Warning: Something is wrong!!!!!");
                System.err.println("However, if it is not in Sioux Falls, you can ignore this warning.");
            } else {
                System.out.println("internal check passed!");
            }
        }
        System.out.println("averaging process complete!");
        // write in csv file
        System.out.println("now writing final result in csv file");
        FileWriter csvWriter = new FileWriter(simResultsDirectory.getPath() + "/mixedResults.csv");
        // making title row
        csvWriter.append("RTShare");
        csvWriter.append(",");
        csvWriter.append("SystemMeanDT");
        csvWriter.append(",");
        csvWriter.append("SystemTotalDist");
        csvWriter.append(",");
        csvWriter.append("AVMeanDT");
        csvWriter.append(",");
        csvWriter.append("AVMeanWT");
        csvWriter.append(",");
        csvWriter.append("AVMedianWT");
        csvWriter.append(",");
        csvWriter.append("CarMeanDT");
        csvWriter.append("\n");

        for (double rtShare : rtShares) {
            csvWriter.append(Double.toString(rtShare));
            csvWriter.append(",");
            double systemMeanDT = (dataMapCar.get(rtShare).getDriveTime() + dataMapAV.get(rtShare).getDriveTime())//
                    / (dataMapCar.get(rtShare).getNumberOfTrips() + dataMapAV.get(rtShare).getNumberOfTrips());
            csvWriter.append(Double.toString(systemMeanDT));
            csvWriter.append(",");
            double systemTotalDistance = //
                    (dataMapCar.get(rtShare).getDistance() + dataMapAV.get(rtShare).getDistance())//
                            / dataMapAV.get(rtShare).getNumberOfSeeds();
            csvWriter.append(Double.toString(systemTotalDistance));
            csvWriter.append(",");
            double avMeanDT = dataMapAV.get(rtShare).getDriveTime() / dataMapAV.get(rtShare).getNumberOfTrips();
            csvWriter.append(Double.toString(avMeanDT));
            csvWriter.append(",");
            double avMeanWT = dataMapAV.get(rtShare).getMeanWaitTime() / dataMapAV.get(rtShare).getNumberOfTrips();
            csvWriter.append(Double.toString(avMeanWT));
            csvWriter.append(",");
            double avMedianWT = dataMapAV.get(rtShare).getMedianWaitTime() / dataMapAV.get(rtShare).getNumberOfSeeds();
            csvWriter.append(Double.toString(avMedianWT));
            csvWriter.append(",");
            double carMeanDT = dataMapCar.get(rtShare).getDriveTime() / dataMapCar.get(rtShare).getNumberOfTrips();
            csvWriter.append(Double.toString(carMeanDT));
            csvWriter.append("\n");
        }
        csvWriter.flush();
        csvWriter.close();
        System.out.println("Final reulst successfully written!");
    }
}
