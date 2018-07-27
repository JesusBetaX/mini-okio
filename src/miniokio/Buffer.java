package miniokio;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Una colección de bytes en la memoria.
 *
 * <p> <strong> Mover datos de un búfer a otro es rápido. </strong> En cambio
 * de copiar bytes de un lugar en la memoria a otro, esta clase simplemente cambia
 * propiedad de las matrices de bytes subyacentes.
 *
 * <p> <strong> Este buffer crece con sus datos. </strong> Al igual que ArrayList,
 * cada búfer comienza pequeño. Solo consume la memoria que necesita.
 *
 * <p> <strong> Este buffer agrupa sus arrays de bytes. </strong> Cuando asigna un
 * matriz de bytes en Java, el tiempo de ejecución debe completar el conjunto solicitado antes de cero
 * devolvérselo. Incluso si vas a escribir sobre ese espacio de todos modos.
 * Esta clase evita el llenado cero y la rotación de GC agrupando conjuntos de bytes.
 */
public final class Buffer implements BufferedSink {
  Segment head;
  long size;
  public Buffer() {
  }
  /** Devuelve la cantidad de bytes actualmente en este búfer. */
  public long size() {
    return size;
  }
  @Override public Buffer buffer() {
    return this;
  }
  
  @Override
  public void write(String str, Charset charset) {
    write(str.getBytes(charset));
  }
  @Override public Buffer write(int b) {
    Segment tail = writableSegment(1);
    tail.data[tail.limit++] = (byte) b;
    size += 1;
    return this;
  }
  @Override public Buffer write(byte[] source) {
    if (source == null) throw new IllegalArgumentException("source == null");
    return write(source, 0, source.length);
  }
  @Override public Buffer write(byte[] source, int offset, int byteCount) {
    if (source == null) throw new IllegalArgumentException("source == null");
    IO.checkOffsetAndCount(source.length, offset, byteCount);
    int limit = offset + byteCount;
    while (offset < limit) {
      Segment tail = writableSegment(1);
      int toCopy = Math.min(limit - offset, Segment.SIZE - tail.limit);
      System.arraycopy(source, offset, tail.data, tail.limit, toCopy);
      offset += toCopy;
      tail.limit += toCopy;
    }
    size += byteCount;
    return this;
  }
  @Override public long writeAll(Source source) throws IOException {
    if (source == null) throw new IllegalArgumentException("source == null");
    long totalBytesRead = 0;
    for (long readCount; (readCount = source.read(this, Segment.SIZE)) != -1; ) {
      totalBytesRead += readCount;
    }
    return totalBytesRead;
  }
  @Override
  public void write(Buffer source, long byteCount) throws IOException {
    if (source == null) throw new IllegalArgumentException("source == null");
    if (source == this) throw new IllegalArgumentException("source == this");
    IO.checkOffsetAndCount(source.size, 0, byteCount);
    while (byteCount > 0) {
      // ¿Es un prefijo del segmento principal de la fuente todo lo que necesitamos mover?
      if (byteCount < (source.head.limit - source.head.pos)) {
        Segment tail = head != null ? head.prev : null;
        if (tail != null && tail.owner
            && (byteCount + tail.limit - (tail.shared ? 0 : tail.pos) <= Segment.SIZE)) {
          // Nuestros segmentos existentes son suficientes. Mueva los bytes desde la fuente hasta la cola.
          source.head.writeTo(tail, (int) byteCount);
          source.size -= byteCount;
          size += byteCount;
          return;
        } else {
          // Vamos a necesitar otro segmento. Dividir la cabeza de la fuente
          // segmentar en dos, luego mover el primero de esos dos a este buffer.
          source.head = source.head.split((int) byteCount);
        }
      }
      // Eliminar el segmento de cabeza de la fuente y anexarlo a nuestra cola.
      Segment segmentToMove = source.head;
      long movedByteCount = segmentToMove.limit - segmentToMove.pos;
      source.head = segmentToMove.pop();
      if (head == null) {
        head = segmentToMove;
        head.next = head.prev = head;
      } else {
        Segment tail = head.prev;
        tail = tail.push(segmentToMove);
        tail.compact();
      }
      source.size -= movedByteCount;
      size += movedByteCount;
      byteCount -= movedByteCount;
    }
  }
  
  /**
   * Devuelve un segmento de cola que podemos escribir al menos {@code minimumCapacity}
   * bytes a, crearlo si es necesario.
   */
  Segment writableSegment(int minimumCapacity) {
    if (minimumCapacity < 1 || minimumCapacity > Segment.SIZE) throw new IllegalArgumentException();
    if (head == null) {
      head = SegmentPool.take(); // Adquirir el primer segmento.
      return head.next = head.prev = head;
    }
    Segment tail = head.prev;
    if (tail.limit + minimumCapacity > Segment.SIZE || !tail.owner) {
      tail = tail.push(SegmentPool.take()); // Agregar un nuevo segmento vacío para llenar.
    }
    return tail;
  }

  
  /**
   * Devuelve la cantidad de bytes en segmentos que no se pueden escribir. Este es el
   * cantidad de bytes que se pueden enjuagar inmediatamente a un receptor subyacente
   * sin perjudicar el rendimiento.
   */
  public long completeSegmentByteCount() {
    long result = size;
    if (result == 0) return 0;
    // Omit the tail if it's still writable.
    Segment tail = head.prev;
    if (tail.limit < Segment.SIZE && tail.owner) {
      result -= tail.limit - tail.pos;
    }
    return result;
  }

  public byte[] readByteArray() throws IOException {
    return readByteArray(size);
  }
  public byte[] readByteArray(long byteCount) throws IOException {
    IO.checkOffsetAndCount(size, 0, byteCount);
    if (byteCount > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("byteCount > Integer.MAX_VALUE: " + byteCount);
    }
    byte[] result = new byte[(int) byteCount];
    readFully(result);
    return result;
  }
  
  public void readFully(byte[] sink) throws IOException {
    int offset = 0;
    while (offset < sink.length) {
      int read = read(sink, offset, sink.length - offset);
      if (read == -1) throw new IOException();
      offset += read;
    }
  }
  
  public int read(byte[] sink, int offset, int byteCount) {
    IO.checkOffsetAndCount(sink.length, offset, byteCount);
    Segment s = head;
    if (s == null) return -1;
    int toCopy = Math.min(byteCount, s.limit - s.pos);
    System.arraycopy(s.data, s.pos, sink, offset, toCopy);
    s.pos += toCopy;
    size -= toCopy;
    if (s.pos == s.limit) {
      head = s.pop();
      SegmentPool.recycle(s);
    }
    return toCopy;
  }
  
  @Override public void flush() throws IOException {}

  @Override public void close() throws IOException { }

  public void clear() throws IOException {
    skip(size);
  }
  
  /** Descarta {@code byteCount} bytes del encabezado de este búfer. */
  public void skip(long byteCount) throws IOException {
    while (byteCount > 0) {
      if (head == null) throw new IOException();
      int toSkip = (int) Math.min(byteCount, head.limit - head.pos);
      size -= toSkip;
      byteCount -= toSkip;
      head.pos += toSkip;
      if (head.pos == head.limit) {
        Segment toRecycle = head;
        head = toRecycle.pop();
        SegmentPool.recycle(toRecycle);
      }
    }
  }
}