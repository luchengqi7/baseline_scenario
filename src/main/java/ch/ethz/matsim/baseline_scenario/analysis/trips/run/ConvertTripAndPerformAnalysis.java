package ch.ethz.matsim.baseline_scenario.analysis.trips.run;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.baseline_scenario.analysis.trips.CSVTripWriter;
import ch.ethz.matsim.baseline_scenario.analysis.trips.TripItem;
import ch.ethz.matsim.baseline_scenario.analysis.trips.listeners.TripListener;
import ch.ethz.matsim.baseline_scenario.analysis.trips.readers.EventsTripReader;
import ch.ethz.matsim.baseline_scenario.analysis.trips.utils.BaselineHomeActivityTypes;
import ch.ethz.matsim.baseline_scenario.analysis.trips.utils.HomeActivityTypes;

public class ConvertTripAndPerformAnalysis {
    // fields
    private static final int INDEX_TRAVEL_TIME = 7; // travel time is the 8th item (index=7)
    private static final int INDEX_TRAVEL_DIST = 8; // travel distance is the 9th item (index=8)
    private static final int INDEX_TRAVEL_MODE = 9; // travel mode is the 10th item (index 9)

    private final List<List<String>> records = new ArrayList<>();
    private final List<Double> listOfTravelTime = new ArrayList<>();
    private final List<Double> listOfTravelDistance = new ArrayList<>();
    double meanTravelTime = 0.0;
    double totalTravelDistance = 0.0;

    // construtors
    private ConvertTripAndPerformAnalysis() {
        // empty by design
    }

    // methods
    /** Main function
     * 
     * @throws IOException */
    public static void main(String[] args) throws IOException {
        System.out.println("Begin result harvesting processing");
        ConvertTripAndPerformAnalysis resultHarvester = new ConvertTripAndPerformAnalysis();
        // step 1. change to the folder
        File simResultsDirectory = new File("input/simulation/output/folder/path/here");
        for (File simulationDataFolder : simResultsDirectory.listFiles()) {
            // step 2. convert trips from event
            resultHarvester.convertTripFromEvents(simulationDataFolder.getName());
            // step 3. analyze trips data and store in a file
            resultHarvester.scanFile(simulationDataFolder);
            resultHarvester.processData();
            File outFile = new File(simulationDataFolder.getName() + "analyzedResult.properties");
            resultHarvester.storeData(outFile);
            System.out.println("Analysis in " + simulationDataFolder.getName() + " is complete!");
        }

        // TODO step 4. output the averaged data in csv file
        TakingTripsAverageData.run(simResultsDirectory);

        System.out.println("Result harvesting complete!");
    }

    // 1. scan the file and store the data in a list of list
    public void scanFile(File file) throws FileNotFoundException, IOException {
        System.out.println("Starting data collection now!");
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine())
                records.add(getRecordFromLine(scanner.nextLine()));
        }
        System.out.println("Data successfully scanned and stored!");
        int numTrips = records.size() - 1;
        System.out.println("the total number of trips is " + numTrips);
    }

    private static List<String> getRecordFromLine(String line) {
        List<String> values = new ArrayList<>();
        try (Scanner scanner = new Scanner(line)) {
            scanner.useDelimiter(";");
            while (scanner.hasNext())
                values.add(scanner.next());
        }
        return values;
    }

    // 2. process data
    private void processData() {
        System.out.println("Starting data Processing now!");
        for (List<String> trip : records)
            if (trip.get(INDEX_TRAVEL_MODE).equals("car")) {
                // then we record this data
                double travelTime = Double.parseDouble(trip.get(INDEX_TRAVEL_TIME));
                double travelDistance = Double.parseDouble(trip.get(INDEX_TRAVEL_DIST));
                listOfTravelTime.add(travelTime);
                listOfTravelDistance.add(travelDistance);
            }
        double totalTravelTime = 0.0;
        for (double travelTime : listOfTravelTime)
            totalTravelTime += travelTime;

        meanTravelTime = totalTravelTime / listOfTravelTime.size();

        double totalDistance = 0.0;
        for (double distance : listOfTravelDistance)
            totalDistance += distance;

        totalTravelDistance = totalDistance;
        System.out.println("Data Processing Complete!");
        System.out.println("Number of car trips is " + listOfTravelTime.size());
        System.out.println("Mean travel time is " + meanTravelTime);
        System.out.println("Total travel distance is " + totalTravelDistance);
    }

    // 3. store the result in a property file
    private void storeData(File file) throws FileNotFoundException, IOException {
        System.out.println("Storing data into a properties file now!");
        Properties properties = new Properties();
        properties.setProperty("meanTravelTime", Double.toString(meanTravelTime));
        properties.setProperty("totalTravelDistance", Double.toString(totalTravelDistance));
        properties.setProperty("numberOfCarTrip", Integer.toString(listOfTravelTime.size()));
        properties.store(new FileOutputStream(file), "no comments");
        System.out.println("Properties file stored successfully!");
    }

    private void convertTripFromEvents(String simulationDataFolder) throws IOException {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile("/home/pc/Desktop/NewSiouxFalls/preparedNetwork.xml");

        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
        HomeActivityTypes homeActivityTypes = new BaselineHomeActivityTypes();
        MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
        Collection<String> networkRouteModes = Arrays.asList("car");

        TripListener tripListener = new TripListener(network, stageActivityTypes, homeActivityTypes, mainModeIdentifier, networkRouteModes);
        String eventFilePath = simulationDataFolder + "/output_events.xml.gz";
        String tripCSVFilePath = simulationDataFolder + "/result.csv";

        Collection<TripItem> trips = new EventsTripReader(tripListener).readTrips(eventFilePath);
        new CSVTripWriter(trips).write(tripCSVFilePath);
    }
}
