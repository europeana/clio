package eu.europeana.clio.common.persistence;

import java.io.Closeable;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.hibernate.Session;

/**
 * This class encapsulates a result stream that is closable. After the client has finished using
 * the stream, this object is to be closed.
 *
 * @param <T> The type of the data in the result stream.
 */
public class StreamResult<T> implements Closeable, Supplier<Stream<T>> {

  private final Stream<T> resultStream;
  private final Session session;

  StreamResult(Stream<T> resultStream, Session session) {
    this.resultStream = resultStream;
    this.session = session;
  }

  @Override
  public void close() {
    this.session.close();
  }

  @Override
  public Stream<T> get() {
    return resultStream;
  }
}
