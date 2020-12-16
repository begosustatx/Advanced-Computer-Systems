package com.acertainbookstore.client.workloads;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	public BookSetGenerator() {}

	/**
	 * Returns num randomly selected isbns from the input set
	 *
	 * selects a given number n of unique ISBNs out of a given
	 * input set at random using a uniform distribution. This function is used in the
	 * customer interaction to select books to be bought.
	 *
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {

		if (num>isbns.size()) {
			return isbns;
		}
		Random rand = new Random();
		Set<Integer> result = new HashSet<>();
		Integer[] isbnsArray = isbns.toArray(new Integer[isbns.size()]);
		while (result.size() < num) {
			result.add(isbnsArray[rand.nextInt(isbns.size())]);
		}
		return result;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 *
	 * generates a set of ImmutableStockBooks of size n with
	 * random values. This function is used in the new stock acquisition interaction to
	 * generate candidate books for insertion.
	 *
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {

		Set<StockBook> stockBooksSet = new HashSet<>();
		for (int i = 0; i < num; i++) {
			stockBooksSet.add(generateImmutableStockBook());
		}
		return stockBooksSet;
	}

	private ImmutableStockBook generateImmutableStockBook() {
		// TODO(MARKO wrote): generate more sense in data, const numbers :D
		Random random = new Random();
		return new ImmutableStockBook(random.nextInt(40)+1,
				                      randomNameGenerator(random.nextInt(100)+1),
				                      randomNameGenerator(random.nextInt(100)+1),
				                 random.nextFloat()*random.nextInt(40),
				                      random.nextInt(40)+1,
				                      random.nextInt(40),
                      		     	  random.nextInt(10),
				             random.nextLong()*random.nextInt(5),
				                      random.nextBoolean());
	}

	private String randomNameGenerator(int length) {
		// create a string of all characters
		String alphabet = " ABCDEFGHI JKLMNOPQ RSTUVWXYZ ";
		// create random string builder
		StringBuilder sb = new StringBuilder();
		// create an object of Random class
		Random random = new Random();

		// specify length of random string
		for(int i = 0; i < length; i++) {
			// generate random index number
			int index = random.nextInt(alphabet.length());
			// get character specified by index
			// from the string
			char randomChar = alphabet.charAt(index);
			// append the character to string builder
			sb.append(randomChar);
		}
		return sb.toString();
	}

}
