package ch.ethz.matsim.baseline_scenario.zurich.cutter.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class ParallelMinimumNetworkFinder implements MinimumNetworkFinder {
	final private static Logger logger = Logger.getLogger(ParallelMinimumNetworkFinder.class);

	final private Link referenceLink;
	final private Network network;
	final private ExecutorService executor;
	final private int numberOfRunners;

	public ParallelMinimumNetworkFinder(ExecutorService executor, int numberOfRunners, Network network,
			Link referenceLink) {
		this.network = network;
		this.referenceLink = referenceLink;
		this.executor = executor;
		this.numberOfRunners = numberOfRunners;
	}

	@Override
	public Set<Id<Link>> run(Set<Id<Link>> linkIds) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("links_input.txt"))));
			
			for (Id<Link> linkId : linkIds) {
				writer.write(linkId.toString() + "\n");
				writer.flush();
			}
			
			writer.close();
		} catch (IOException e) {}
		
		AtomicInteger numberOfProcessedLinks = new AtomicInteger(0);
		int numberOfLinks = linkIds.size();

		List<Id<Link>> pendingList = new LinkedList<>(linkIds);
		List<Future<?>> futures = new LinkedList<>();

		int linksPerRunner = numberOfLinks / numberOfRunners;
		Set<Id<Link>> relevantIds = Collections.synchronizedSet(new HashSet<>());

		for (int i = 0; i < numberOfRunners; i++) {
			List<Id<Link>> localPendingList = (i < numberOfLinks - 1)
					? pendingList.subList(i * linksPerRunner, (i + 1) * linksPerRunner)
					: pendingList.subList(i * linksPerRunner, pendingList.size());

			futures.add(CompletableFuture.supplyAsync(() -> {
				TravelTime travelTime = new FreeSpeedTravelTime();
				TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);
				LeastCostPathCalculator calculator = new DijkstraFactory().createPathCalculator(network,
						travelDisutility, travelTime);
				
				Set<Id<Link>> coveredSet = new HashSet<>();
				Set<Id<Link>> forwardTabuSet = new HashSet<>();
				Set<Id<Link>> backwardTabuSet = new HashSet<>();

				for (Id<Link> testLinkId : localPendingList) {
					Link testLink = network.getLinks().get(testLinkId);
					coveredSet.add(testLinkId);

					if (testLink == null) {
						throw new IllegalStateException("Cannot find link " + testLinkId);
					}

					if (true) { //!forwardTabuSet.contains(testLinkId)) {
						Path result = calculator.calcLeastCostPath(testLink.getToNode(), referenceLink.getFromNode(),
								0.0, null, null);

						if (result.links.size() > 1) {
							result.links.subList(1, result.links.size() - 1)
									.forEach(l -> forwardTabuSet.add(l.getId()));
						}
						
						result.links.forEach(l -> coveredSet.add(l.getId()));
					}

					if (true) { //!backwardTabuSet.contains(testLinkId)) {
						Path result = calculator.calcLeastCostPath(referenceLink.getToNode(), testLink.getFromNode(),
								0.0, null, null);

						if (result.links.size() > 1) {
							result.links.subList(1, result.links.size() - 1)
									.forEach(l -> backwardTabuSet.add(l.getId()));
						}
						
						result.links.forEach(l -> coveredSet.add(l.getId()));
					}

					int currentNumberOfProcessedLinks = numberOfProcessedLinks.incrementAndGet();
					double percentage = 100.0 * currentNumberOfProcessedLinks / numberOfLinks;
					logger.info(String.format("Finding minimum network %d/%d (%.2f%%)", currentNumberOfProcessedLinks,
							numberOfLinks, percentage));
				}

				return coveredSet;
			}, executor).thenAccept(partialSet -> relevantIds.addAll(partialSet)));
		}

		try {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("links_input.txt"))));
			
			for (Id<Link> linkId : relevantIds) {
				writer.write(linkId.toString() + "\n");
				writer.flush();
			}
			
			writer.close();
		} catch (IOException e) {}

		return relevantIds;
	}
}
