/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		for (int numConcurrentWorkloadThreads = 10; numConcurrentWorkloadThreads <= 1000; numConcurrentWorkloadThreads+=10) {

			String serverAddress = "http://localhost:8081";
			boolean localTest = true;
			List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
			List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

			// Initialize the RPC interfaces if its not a localTest, the variable is
			// overriden if the property is set
			String localTestProperty = System
					.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean
					.parseBoolean(localTestProperty) : localTest;

			BookStore bookStore = null;
			StockManager stockManager = null;
			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				bookStore = store;
				stockManager = store;
			} else {
				stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
				bookStore = new BookStoreHTTPProxy(serverAddress);
			}

			// Generate data in the bookstore before running the workload
			initializeBookStoreData(bookStore, stockManager);

			ExecutorService exec = Executors
					.newFixedThreadPool(numConcurrentWorkloadThreads);

			for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
				WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
						stockManager);
				Worker workerTask = new Worker(config);
				// Keep the futures to wait for the result from the thread
				runResults.add(exec.submit(workerTask));
			}

			// Get the results from the threads using the futures returned
			for (Future<WorkerRunResult> futureRunResult : runResults) {
				WorkerRunResult runResult = futureRunResult.get(); // blocking call
				workerRunResults.add(runResult);
			}

			exec.shutdownNow(); // shutdown the executor

			// Finished initialization, stop the clients if not localTest
			if (!localTest) {
				((BookStoreHTTPProxy) bookStore).stop();
				((StockManagerHTTPProxy) stockManager).stop();
			}

			reportMetric(workerRunResults);
			stockManager.removeAllBooks();
		}
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		double throughput = 0.0;
		double latency = 0.0;
		for (WorkerRunResult workerRunResult: workerRunResults) {
			double elapsedTimeInSecs = (workerRunResult.getElapsedTimeInNanoSecs() / Math.pow(10.0,9));
			//System.out.println(workerRunResult.getTotalFrequentBookStoreInteractionRuns() + " " + workerRunResult.getSuccessfulInteractions() + " " + (workerRunResult.getElapsedTimeInNanoSecs() / 3600) + "s");
			//System.out.println(workerRunResult.getSuccessfulFrequentBookStoreInteractionRuns() + " " + workerRunResults.size());
			throughput += workerRunResult.getSuccessfulInteractions() / elapsedTimeInSecs;
			latency += elapsedTimeInSecs/workerRunResult.getSuccessfulInteractions();
		}
		//System.out.println("++++++++++++++++++++++++++++++++++");
		System.out.println("Workers " +  workerRunResults.size());
		System.out.println("Throughput: " + throughput);
		System.out.println("Latency: " + latency);
		System.out.println("++++++++++++++++++++++++++++++++++");
		try(FileWriter fw = new FileWriter("Throughput.txt", true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter out = new PrintWriter(bw))
		{
			out.println(throughput);
		} catch (IOException e) {
			//exception handling left as an exercise for the reader
		}
		try(FileWriter fw = new FileWriter("Latency.txt", true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter out = new PrintWriter(bw))
		{
			out.println(latency);
		} catch (IOException e) {
			//exception handling left as an exercise for the reader
		}

	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore,
			StockManager stockManager) throws BookStoreException {
		BookSetGenerator generator = new BookSetGenerator();
		stockManager.addBooks(generator.nextSetOfStockBooks(10000));
	}
}
