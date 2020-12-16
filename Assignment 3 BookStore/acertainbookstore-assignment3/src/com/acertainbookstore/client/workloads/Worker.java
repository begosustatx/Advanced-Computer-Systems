/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
    private WorkloadConfiguration configuration = null;
    private int numSuccessfulFrequentBookStoreInteraction = 0;
    private int numTotalFrequentBookStoreInteraction = 0;

    public Worker(WorkloadConfiguration config) {
	configuration = config;
    }

    /**
     * Run the appropriate interaction while trying to maintain the configured
     * distributions
     * 
     * Updates the counts of total runs and successful runs for customer
     * interaction
     * 
     * @param chooseInteraction
     * @return
     */
    private boolean runInteraction(float chooseInteraction) {
	try {
	    float percentRareStockManagerInteraction = configuration.getPercentRareStockManagerInteraction();
	    float percentFrequentStockManagerInteraction = configuration.getPercentFrequentStockManagerInteraction();

	    if (chooseInteraction < percentRareStockManagerInteraction) {
		runRareStockManagerInteraction();
	    } else if (chooseInteraction < percentRareStockManagerInteraction
		    + percentFrequentStockManagerInteraction) {
		runFrequentStockManagerInteraction();
	    } else {
		numTotalFrequentBookStoreInteraction++;
		runFrequentBookStoreInteraction();
		numSuccessfulFrequentBookStoreInteraction++;
	    }
	} catch (BookStoreException ex) {
	    return false;
	}
	return true;
    }

    /**
     * Run the workloads trying to respect the distributions of the interactions
     * and return result in the end
     */
    public WorkerRunResult call() throws Exception {
	int count = 1;
	long startTimeInNanoSecs = 0;
	long endTimeInNanoSecs = 0;
	int successfulInteractions = 0;
	long timeForRunsInNanoSecs = 0;

	Random rand = new Random();
	float chooseInteraction;

	// Perform the warmup runs
	while (count++ <= configuration.getWarmUpRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    runInteraction(chooseInteraction);
	}

	count = 1;
	numTotalFrequentBookStoreInteraction = 0;
	numSuccessfulFrequentBookStoreInteraction = 0;

	// Perform the actual runs
	startTimeInNanoSecs = System.nanoTime();
	while (count++ <= configuration.getNumActualRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    if (runInteraction(chooseInteraction)) {
		successfulInteractions++;
	    }
	}
	endTimeInNanoSecs = System.nanoTime();
	timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
	return new WorkerRunResult(successfulInteractions, timeForRunsInNanoSecs, configuration.getNumActualRuns(),
		numSuccessfulFrequentBookStoreInteraction, numTotalFrequentBookStoreInteraction);
    }

    /**
     * Runs the new stock acquisition interaction
     * 
     * @throws BookStoreException
     */
    private void runRareStockManagerInteraction() throws BookStoreException {
	// TODO: Add code for New Stock Acquisition Interaction
		// TODO(MARKO wrote): recheck, what to write for num? when throw exception
		/**
		 * invokes getBooks and then gets a random set
		 * of books from an instance of BookSetGenerator by calling nextSetOfStockBooks.
		 * It then checks if the set of ISBNs is in the list of books fetched. Finally, it invokes
		 * addBooks with the set of books not found in the list returned by getBooks.
		 */
		int nextSetOfStockBooks = new Random().nextInt(configuration.getNumActualRuns()); // NOT SURE

		List<StockBook> stockBookList = configuration.getStockManager().getBooks();
		Set<StockBook> books = configuration.getBookSetGenerator().nextSetOfStockBooks(nextSetOfStockBooks);
		Set<StockBook> newBooks = new HashSet<>();
		for (StockBook stockBook: books) {
			if (stockBookList.stream().noneMatch(book -> book.getISBN() == stockBook.getISBN()))
				newBooks.add(stockBook);
		}
		configuration.getStockManager().addBooks(newBooks);
    }

    /**
     * Runs the stock replenishment interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentStockManagerInteraction() throws BookStoreException {
	// TODO: Add code for Stock Replenishment Interaction
		// TODO(MARKO wrote): check k value, how many copies - from config
		/**
		 * invokes getBooks, selects the k books
		 * with smallest quantities in stock, and then invokes addCopies on these books.
		 */
		List<StockBook> stockBookList = configuration.getStockManager().getBooks();
		stockBookList.sort(Comparator.comparing(StockBook::getNumCopies));
		Set<BookCopy> newBookCopies = new HashSet<>();
		for (int i = 0; i < stockBookList.size() && i<configuration.getNumBooksWithLeastCopies(); i++) {
			newBookCopies.add(new BookCopy(stockBookList.get(i).getISBN(), configuration.getNumAddCopies()));
		}
		configuration.getStockManager().addCopies(newBookCopies);
    }

    /**
     * Runs the customer interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentBookStoreInteraction() throws BookStoreException {
	// TODO: Add code for Customer Interaction
		// TODO(MARKO wrote): check numbBooks, subsetBooks, how many copies to buy - from config
		/**
		 * invokes getEditorPicks. It then selects a
		 * subset of the books returned by calling sampleFromSetOfISBNs, and buys the books
		 * selected by calling buyBooks.
		 */

		int subsetBooks = new Random().nextInt(configuration.getNumEditorPicksToGet());

		List<Book> editorPicksBookList = configuration.getBookStore().getEditorPicks(configuration.getNumEditorPicksToGet());
		Set<Integer> isbns = editorPicksBookList.stream().map(Book::getISBN).collect(Collectors.toSet());
		Set<Integer> pickedIsbns = configuration.getBookSetGenerator().sampleFromSetOfISBNs(isbns, subsetBooks);
		Set<BookCopy> buyBooks = new HashSet<>();
		for (int pickedIsbn: pickedIsbns) {
			buyBooks.add(new BookCopy(pickedIsbn, configuration.getNumBooksToBuy()));
		}
		configuration.getBookStore().buyBooks(buyBooks);
    }

}
