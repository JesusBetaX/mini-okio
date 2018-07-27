package miniokio;

import java.io.IOException;
import java.nio.charset.Charset;

final class RealBufferedSink implements BufferedSink {
  public final Buffer buffer;
  public final Sink sink;
  private boolean closed;
  
  public RealBufferedSink(Sink sink, Buffer buffer) {
    if (sink == null) throw new IllegalArgumentException("sink == null");
    this.buffer = buffer;
    this.sink = sink;
  }
  public RealBufferedSink(Sink sink) {
    this(sink, new Buffer());
  }
  @Override public Buffer buffer() {
    return buffer;
  }
  @Override
  public void write(String str, Charset charset) throws IOException {
    if (closed) throw new IllegalStateException("closed");
    buffer.write(str, charset);
    emitCompleteSegments();
  }
  @Override public void write(Buffer source, long byteCount) throws IOException {
    if (closed) throw new IllegalStateException("closed");
    buffer.write(source, byteCount);
    emitCompleteSegments();
  }
  @Override public BufferedSink write(byte[] source) throws IOException {
    if (closed) throw new IllegalStateException("closed");
    buffer.write(source);
    return emitCompleteSegments();
  }
  @Override public BufferedSink write(byte[] source, int offset, int byteCount) throws IOException {
    if (closed) throw new IllegalStateException("closed");
    buffer.write(source, offset, byteCount);
    return emitCompleteSegments();
  }
  @Override public long writeAll(Source source) throws IOException {
    if (source == null) throw new IllegalArgumentException("source == null");
    long totalBytesRead = 0;
    for (long readCount; (readCount = source.read(buffer, Segment.SIZE)) != -1; ) {
      totalBytesRead += readCount;
      emitCompleteSegments();
    }
    return totalBytesRead;
  }
  @Override public BufferedSink write(int b) throws IOException {
    if (closed) throw new IllegalStateException("closed");
    buffer.write(b);
    return emitCompleteSegments();
  }
  public BufferedSink emitCompleteSegments() throws IOException {
    if (closed) throw new IllegalStateException("closed");
    long byteCount = buffer.completeSegmentByteCount();
    if (byteCount > 0) sink.write(buffer, byteCount);
    return this;
  }
  @Override public void flush() throws IOException {
    if (closed) throw new IllegalStateException("closed");
    if (buffer.size > 0) {
      sink.write(buffer, buffer.size);
    }
    sink.flush();
  }
  @Override public void close() throws IOException {
    if (closed) return;
    // Emite datos almacenados en el búfer subyacente. Si esto falla, todavía necesitamos
    // para cerrar el fregadero; de lo contrario, corremos el riesgo de perder recursos.
    Throwable thrown = null;
    try {
      if (buffer.size > 0) {
        sink.write(buffer, buffer.size);
      }
    } catch (Throwable e) {
      thrown = e;
    }
    try {
      sink.close();
    } catch (Throwable e) {
      if (thrown == null) thrown = e;
    }
    closed = true;
    if (thrown != null) throw new IOException(thrown);
  }
}