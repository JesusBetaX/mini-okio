package miniokio;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * API esencial para trabajar con Java IO - Streams.
 */
public final class IO {

  public static Sink sink(final OutputStream out) {
    if (out == null) throw new IllegalArgumentException("out == null");
    return new Sink() {
      @Override public void write(Buffer source, long byteCount) throws IOException {
        checkOffsetAndCount(source.size, 0, byteCount);
        while (byteCount > 0) {
          Segment head = source.head;
          int toCopy = (int) Math.min(byteCount, head.limit - head.pos);
          out.write(head.data, head.pos, toCopy);
          head.pos += toCopy;
          byteCount -= toCopy;
          source.size -= toCopy;
          if (head.pos == head.limit) {
            source.head = head.pop();
            SegmentPool.recycle(head);
          }
        }
      }
      @Override public void flush() throws IOException {
        out.flush();
      }
      @Override public void close() throws IOException {
        out.close();
      }
    };
  }

  public static BufferedSink buffer(Sink sink) {
    return new RealBufferedSink(sink);
  }

  public static Source source(File value) throws IOException {
    return source(new FileInputStream(value));
  }

  public static Source source(final InputStream in) {
    if (in == null) throw new IllegalArgumentException("in == null");
    return new Source() {
      @Override public InputStream inputStream() {
        return in;
      }
      @Override public long read(Buffer sink, long byteCount) throws IOException {
        if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        if (byteCount == 0) return 0;
        Segment tail = sink.writableSegment(1);
        int maxToCopy = (int) Math.min(byteCount, Segment.SIZE - tail.limit);
        int bytesRead = in.read(tail.data, tail.limit, maxToCopy);
        if (bytesRead == -1) return -1;
        tail.limit += bytesRead;
        sink.size += bytesRead;
        return bytesRead;
      }
      @Override public void close() throws IOException {
        in.close();
      }
    };
  }

  public static void checkOffsetAndCount(long size, long offset, long byteCount) {
    if ((offset | byteCount) < 0 || offset > size || size - offset < byteCount) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("size=%s offset=%s byteCount=%s", size, offset, byteCount));
    }
  }
  
  public static void close(Closeable closeable) {
    try {
      if (closeable != null) closeable.close();
    } catch (IOException ignore) {
      //
    }
  }
}
