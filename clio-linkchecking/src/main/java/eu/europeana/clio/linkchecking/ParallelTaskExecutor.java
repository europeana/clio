package eu.europeana.clio.linkchecking;

import eu.europeana.clio.common.exception.ClioException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class provides functionality to perform parallel processing from a stream.
 */
public class ParallelTaskExecutor {

  /**
   * This method executes the given operation on the given data in the given number of threads. The
   * threads continuously (lazily) obtain elements from the stream and execute the operation on
   * them. As soon as the first throws an exception, we stop all remaining processing.
   *
   * @param dataStream The data to process.
   * @param operation The operation to perform.
   * @param numberOfThreads The number of threads we use.
   * @param <T> The type of the data.
   * @throws ClioException In case one of the data causes this exception during execution of the
   * operation.
   */
  public static <T> void executeAndWait(Stream<T> dataStream, Operation<T> operation,
          int numberOfThreads) throws ClioException {

    // Prepare the data for iteration: make available the data in a lazy way. The callers are
    // expected to do this in a thread-safe way, to avoid race conditions between the returned task
    // and the boolean indicating whether there are more tasks.
    final Iterator<T> dataIterator = dataStream.iterator();
    final AtomicBoolean noMoreData = new AtomicBoolean(false);
    final Supplier<T> threadUnsafeNextValueSupplier = () -> {
      if (dataIterator.hasNext()) {
        return dataIterator.next();
      }
      noMoreData.set(true);
      return null;
    };

    // Schedule threads that repeatedly get the next object and perform the operation. Use a
    // completion service to get futures in order of completion (so that if an exception occurs, we
    // know it immediately).
    final IntFunction<Callable<Void>> callableSupplier = index -> (Callable<Void>) () -> {
      while (true) {
        final T data;
        synchronized (threadUnsafeNextValueSupplier) {
          data = threadUnsafeNextValueSupplier.get();
          if (noMoreData.get()) {
            break;
          }
        }
        operation.perform(data);
      }
      return null;
    };
    final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    final CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
    final List<Future<Void>> futures = IntStream.range(0, numberOfThreads)
            .mapToObj(callableSupplier).map(completionService::submit).collect(Collectors.toList());

    // Wait for all futures to be finished, checking whether any one throws an exception.
    try {
      for (int i = 0; i < futures.size(); i++) {
        completionService.take().get();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ClioException("Thread was interrupted.", e);
    } catch (ExecutionException e) {
      final Throwable cause = e.getCause() == null ? e : e.getCause();
      throw new ClioException("Something went wrong during thread pool execution.", cause);
    } finally {
      // In case of errors, there may still be tasks pending. Otherwise, this has no effect.
      futures.forEach(future -> future.cancel(true));
      executor.shutdownNow();
    }
  }

  /**
   * This interface represents an operation on data that can throw an exception.
   *
   * @param <T> The type of the input data.
   */
  @FunctionalInterface
  public interface Operation<T> {

    /**
     * Performs the operation on the given data.
     *
     * @param data The data.
     * @throws ClioException In case a problem occurred while processing the data.
     */
    void perform(T data) throws ClioException;

  }
}
