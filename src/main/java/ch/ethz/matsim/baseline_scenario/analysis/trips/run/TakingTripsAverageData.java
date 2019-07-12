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
    private static final int NUMBER_OF_SEEDS = 3;
    private static final double[] rtShares = { 0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.75 };

    static void run(File simResultsDirectory) throws IOException {
        System.out.println("Now taking average trip data");

        Map<Double, CarTripsTotalDataGroup> dataMapCar = new HashMap<>();
        Map<Double, AVTripsTotalDataGroup> dataMapAV = new HashMap<>();

        for (File resultFolder : simResultsDirectory.listFiles()) {
            // Read simulation info
            Properties simulationInfo = new Properties();
            try {
                simulationInfo.load(new FileInputStream//
                (resultFolder.getName() + "/" + resultFolder.getName() + ".properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Load car trips data
            Properties carTripsData = new Properties();
            try {
                carTripsData.load(new FileInputStream//
                (resultFolder.getName() + "/analyzedResult.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Load av trips data
            Properties avTripsData = new Properties();
            try {
                carTripsData.load(new FileInputStream//
                (resultFolder.getName() + "/data/totalSimulationValues.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            double rtShare = Double.parseDouble(simulationInfo.getProperty("rtShare"));

            // store car trips gross data in data map
            double carMeanDriveTime = Double.parseDouble(carTripsData.getProperty("meanTravelTime"));
            double carTotalDistance = Double.parseDouble(carTripsData.getProperty("totalTravelDistance"));
            int numberOfCarTrips = Integer.parseInt(carTripsData.getProperty("numberOfCarTrip"));
            double carGrossDriveTime = carMeanDriveTime * numberOfCarTrips;
            CarTripsTotalDataGroup carTripsTotalDataGroup = //
                    new CarTripsTotalDataGroup(carGrossDriveTime, carTotalDistance, numberOfCarTrips);
            if (!dataMapCar.containsKey(rtShare)) {
                dataMapCar.put(rtShare, carTripsTotalDataGroup);
            } else {
                CarTripsTotalDataGroup newCarTripsDataGroup = //
                        CarTripsTotalDataGroup.addData(carTripsTotalDataGroup, dataMapCar.get(rtShare));
                dataMapCar.put(rtShare, newCarTripsDataGroup);
            }
            // Store av trips data in data map
            double avMeanDriveTime = Double.parseDouble(avTripsData.getProperty("MeanDriveTime"));
            double avTotalDistance = Double.parseDouble(avTripsData.getProperty("TotalRoboTaxiDistance"));
            int numOfAVTrips = Integer.parseInt(avTripsData.getProperty("totalRequests"));
            double avMeanWaitTime = Double.parseDouble(avTripsData.getProperty("MeanWaitingTime"));
            double avMedianWaitTime = Double.parseDouble(avTripsData.getProperty("WaitTimeQuantile2"));
            double avGrossDriveTime = avMeanDriveTime * numOfAVTrips;
            double avGrossWaitTime = avMeanWaitTime * numOfAVTrips;

            AVTripsTotalDataGroup avTripsTotalDataGroup = new AVTripsTotalDataGroup//
            (avGrossDriveTime, avTotalDistance, numOfAVTrips, avGrossWaitTime, avMedianWaitTime);

            if (!dataMapAV.containsKey(rtShare)) {
                dataMapAV.put(rtShare, avTripsTotalDataGroup);
            } else {
                AVTripsTotalDataGroup newAVTripsTotalDataGroup = AVTripsTotalDataGroup.addData//
                (avTripsTotalDataGroup, dataMapAV.get(rtShare));
                dataMapAV.put(rtShare, newAVTripsTotalDataGroup);
            }
        }
        System.out.println("averaging process complete!");
        // write in csv file
        System.out.println("now writing final result in csv file");
        FileWriter csvWriter = new FileWriter("simResultsDirectory/mixedResults.csv");
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
                            / NUMBER_OF_SEEDS;
            csvWriter.append(Double.toString(systemTotalDistance));
            csvWriter.append(",");
            double avMeanDT = dataMapAV.get(rtShare).getDriveTime() / dataMapAV.get(rtShare).getNumberOfTrips();
            csvWriter.append(Double.toString(avMeanDT));
            csvWriter.append(",");
            double avMeanWT = dataMapAV.get(rtShare).getMeanWaitTime() / dataMapAV.get(rtShare).getNumberOfTrips();
            csvWriter.append(Double.toString(avMeanWT));
            csvWriter.append(",");
            double avMedianWT = dataMapAV.get(rtShare).getMedianWaitTime() / NUMBER_OF_SEEDS;
            csvWriter.append(Double.toString(avMedianWT));
            csvWriter.append(",");
            double carMeanDT = dataMapCar.get(rtShare).getDriveTime() / dataMapCar.get(rtShare).getNumberOfTrips();
            csvWriter.append(Double.toString(carMeanDT));
            csvWriter.append(",");
        }
        csvWriter.flush();
        csvWriter.close();
        System.out.println("Final reulst successfully written!");
    }
}
