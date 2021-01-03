package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.acertainbookstore.business.*;

import org.junit.*;

import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** Single lock test */
	private static boolean singleLock = false;

	/** Number of iteration of operations in concurrency threads **/
	private static final int iterations = 1000;
	
	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/** The error for test2 **/
	private static boolean error = false;

	/** The error for test3 **/
	private static boolean thread = false;

	/** The error for test3 **/
	private static int editorsPicks = 0;

	/**
	 * Sets the up before class.
	 */




	static class T1 implements Runnable{
		HashSet<BookCopy> books;

		public T1(HashSet<BookCopy> booksToBuy) {
			books = booksToBuy;
		}

		@Override
		public void run() {
			for(int i = 0; i<iterations; i++){
				try {
					client.buyBooks(books);
					System.out.println("BUY: "+i);
				} catch (BookStoreException e) {
					e.printStackTrace();
					error = true;
					break;
				}
			}
		}
	}

	static class T2 implements Runnable{
		HashSet<BookCopy> books;
		public T2(HashSet<BookCopy> booksToBuy) {
			books = booksToBuy;
		}

		@Override
		public void run() {
			for(int i = 0; i<iterations; i++) {
				try {
					storeManager.addCopies(books);
					System.out.println("COPY: "+i);
				} catch (BookStoreException e) {
					e.printStackTrace();
					error = true;
					break;
				}
			}
		}
	}

	static class T3 implements Runnable{
		HashSet<BookCopy> books;
		public T3(HashSet<BookCopy> booksToBuy) {
			books = booksToBuy;
		}

		@Override
		public void run() {
			while(!thread) {
				try {
					client.buyBooks(books);
					System.out.println("BUY");
				} catch (BookStoreException e) {
					e.printStackTrace();
					error = true;
					break;
				}

				try {
					storeManager.addCopies(books);
					System.out.println("ADD");
				} catch (BookStoreException e) {
					e.printStackTrace();
					error = true;
					break;
				}
			}
		}
	}

	static class T4 implements Runnable{

		@Override
		public void run() {
			for(int i = 0; i<iterations; i++) {
				List<StockBook> booksOnStock = null;
				try {
					booksOnStock = storeManager.getBooks();
					System.out.println("READ: "+i);
				}
				catch (BookStoreException e) {
					e.printStackTrace();
					error = true;
					thread = true;
					break;
				}
				List<Integer> numCopies = new ArrayList<>();
				int numBooks = booksOnStock.get(0).getNumCopies();
					for (StockBook book : booksOnStock) {
						if (numBooks != book.getNumCopies()) {
							//Think about throwing an error
							error = true;
							thread = true;
							break;
						}
						numCopies.add(book.getNumCopies());

					}
				}
			thread = true;
			}
		}


	static class T5 implements Runnable{
		public T5() {}

		@Override
		public void run() {
			for (int i = 0; i < iterations; i++){
				try {
					client.getEditorPicks(iterations);
					System.out.println("GET");
				} catch (BookStoreException e) {
					e.printStackTrace();
					error = true;
					break;
				}
			}
		}
	}

	static class T6 implements Runnable{
		public T6(){}
		@Override
		public void run() {
			for (int i = 0; i < iterations; i++){
				if(error){
					break;
				}
				Set<BookEditorPick> editorPickBook = new HashSet<>();
				editorPickBook.add(new BookEditorPick(TEST_ISBN + i, true));
				try {
					storeManager.updateEditorPicks(editorPickBook);
					System.out.println("UPDATE");
					editorsPicks ++;
				} catch (BookStoreException e) {
					e.printStackTrace();
					error = true;
					break;
				}
			}
		}
	}


	static class T7 implements Runnable{
		Set<Integer> isbns;
		public T7(Set<Integer> booksToRemove) {
			isbns = booksToRemove;
		}

		@Override
		public void run() {
			try {
				storeManager.removeBooks(isbns);
				System.out.println("REMOVE");
			} catch (BookStoreException e) {
				e.printStackTrace();
			}
		}
	}

	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;
			
			String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
			singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

			if (localTest) {
				if (singleLock) {
					SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				} else {
					TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				}
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Two clients C1 and C2, running in di  erent threads, each invoke a fixed
	 * number of operations. C1 calls buyBooks, while C2 calls addCopies on S
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 * @throws InterruptedException
	 * 			   the thread interrupt exception
	 */
	@Test
	public void test1() throws BookStoreException, InterruptedException {

		storeManager.removeAllBooks();
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN, "Java Concurrency in Practice", "Brian Goetz",
				3000f, NUM_COPIES*iterations, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);


		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));

		Thread t1 = new Thread( new T1(booksToBuy));
		Thread t2 = new Thread( new T2(booksToBuy));

		t1.start();
		t2.start();

		t1.join();
		t2.join();

		List<StockBook> booksOnStock = storeManager.getBooks();

		int i = 0;
		for(StockBook book:booksToAdd){
			assertTrue(booksOnStock.get(i).getISBN() ==book.getISBN());
			assertTrue(booksOnStock.get(i).getNumCopies() ==book.getNumCopies());
		}

	}

	/**
	 * Two clients C1 and C2, running in di  erent threads, each invoke a fixed
	 * number of operations. C1 calls buyBooks then addCopies, while C2 calls getBooks,
	 * the snapshots returned by getBooks must be consistent.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 * @throws InterruptedException
	 * 			   the thread interrupt exception
	 */
	@Test
	public void test2() throws BookStoreException, InterruptedException {
		storeManager.removeAllBooks();

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN, "Java Concurrency in Practice", "Brian Goetz",
				3000f, NUM_COPIES*iterations, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));

		Thread t3 = new Thread( new T3(booksToBuy));
		Thread t4 = new Thread( new T4());

		t3.start();
		t4.start();

		t3.join();
		t4.join();

		assertFalse(error);
	}

	/**
	 * Two clients C1 and C2, running in different threads, each invoke a fixed
	 * number of operations. C1 calls addBooks then C2 addCopies
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 * @throws InterruptedException
	 * 			   the thread interrupt exception
	 */
	@Test
	public void test3() throws BookStoreException, InterruptedException {
		storeManager.removeAllBooks();

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		for (int i = 0; i < iterations; i++){
			booksToAdd.add(new ImmutableStockBook(TEST_ISBN+i, "Java Concurrency in Practice "+i, "Brian Goetz",
					3000f, NUM_COPIES, 0, 0, 0, false));
		}

		storeManager.addBooks(booksToAdd);

		Thread t5 = new Thread(new T5());
		Thread t6 = new Thread(new T6());

		t5.start();
		t6.start();

		t5.join();
		t6.join();

		List<Book> editorPicks = client.getEditorPicks(iterations);
		assertTrue(!error && editorPicks.size()==iterations);

	}

	/**
	 * Two clients C1 and C2, running in different threads, each invoke a fixed
	 * number of operations. C1 calls addBooks then addCopies, C2 removeBook.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 * @throws InterruptedException
	 * 			   the thread interrupt exception
	 */
	@Test
	public void test4() throws BookStoreException, InterruptedException {

		storeManager.removeAllBooks();
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));
		Set<Integer> isbns = new HashSet<>();
		isbns.add(TEST_ISBN);

		Thread t2 = new Thread(new T2(booksToBuy));
		Thread t7 = new Thread(new T7(isbns));

		t2.start();
		t7.start();

		t2.join();
		t7.join();
		List<StockBook> booksOnStock = storeManager.getBooks();
		assertTrue(booksOnStock.isEmpty() );

	}
	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
