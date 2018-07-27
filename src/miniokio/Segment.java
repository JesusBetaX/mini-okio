package miniokio;

/**
 * Un segmento de un buffer.
 *
 * <p>Cada segmento en un búfer es un nodo de lista con enlaces circulares que hace referencia a lo siguiente y
 * segmentos anteriores en el búfer.
 *
 * <p>Cada segmento en el conjunto es un nodo de lista vinculado individualmente que hace referencia al resto de segmentos en el
 * grupo.
 *
 * <p>Las matrices de bytes subyacentes de segmentos se pueden compartir entre búferes y cadenas de bytes. 
 * Cuando la matriz de bytes del segmento se comparte; el segmento no se puede reciclar ni se pueden modificar sus datos de bytes.
 * La única excepción es que el segmento propietario puede agregar al segmento, escribiendo datos en
 * {@code limit} y más. Hay un solo segmento propietario para cada matriz de bytes. Posiciones,
 * los límites, previos y las siguientes referencias no se comparten.
 */
final class Segment {
  /** El tamaño de todos los segmentos en bytes. */
  static final int SIZE = 2048;
  final byte[] data;
  /** El siguiente byte de los datos de aplicación para leer en este segmento. */
  int pos;
  /** El primer byte de datos disponibles listo para ser escrito. */
  int limit;
  /** Verdadero si otros segmentos o cadenas de bytes usan la misma matriz de bytes. */
  boolean shared;
  /** Es cierto si este segmento posee la matriz de bytes y puede agregarla, extendiendo {@code limit}. */
  boolean owner;
  /** Siguiente segmento en una lista vinculada o vinculada circularmente. */
  Segment next;
  /** Segmento anterior en una lista de enlaces circulares. */
  Segment prev;
  Segment() {
    this.data = new byte[SIZE];
    this.owner = true;
    this.shared = false;
  }
  Segment(Segment shareFrom) {
    this(shareFrom.data, shareFrom.pos, shareFrom.limit);
    shareFrom.shared = true;
  }
  Segment(byte[] data, int pos, int limit) {
    this.data = data;
    this.pos = pos;
    this.limit = limit;
    this.owner = false;
    this.shared = true;
  }
  /**
   * Elimina este segmento de una lista de enlaces circulares y devuelve su sucesor.
   * Devuelve nulo si la lista está ahora vacía.
   */
  public Segment pop() {
    Segment result = next != this ? next : null;
    prev.next = next;
    next.prev = prev;
    next = null;
    prev = null;
    return result;
  }
  /**
   * Añade {@code segment} después de este segmento en la lista de enlaces circulares.
   * Devuelve el segmento empujado.
   */
  public Segment push(Segment segment) {
    segment.prev = this;
    segment.next = next;
    next.prev = segment;
    next = segment;
    return segment;
  }
  /**
   * Divide este encabezado de una lista de enlaces circulares en dos segmentos. El primero
   * segmento contiene los datos en {@code [pos..pos + byteCount)}. El segundo
   * segmento contiene los datos en {@code [pos + byteCount..limit)}. Esto puede ser
   * útil cuando se mueven segmentos parciales de un búfer a otro.
   *
   * <p> Devuelve el nuevo encabezado de la lista de enlaces circulares.
   */
  public Segment split(int byteCount) {
    if (byteCount <= 0 || byteCount > limit - pos) throw new IllegalArgumentException();
    Segment prefix = new Segment(this);
    prefix.limit = prefix.pos + byteCount;
    pos += byteCount;
    prev.push(prefix);
    return prefix;
  }
  /**
   * Llamar a esto cuando la cola y su predecesor pueden ser menos de la mitad
   * completo. Esto copiará los datos para que los segmentos puedan reciclarse.
   */
  public void compact() {
    if (prev == this) throw new IllegalStateException();
    if (!prev.owner) return; // Cannot compact: prev isn't writable.
    int byteCount = limit - pos;
    int availableByteCount = SIZE - prev.limit + (prev.shared ? 0 : prev.pos);
    if (byteCount > availableByteCount) return; // Cannot compact: not enough writable space.
    writeTo(prev, byteCount);
    pop();
    SegmentPool.recycle(this);
  }
  /** Mueve {bytes byteCount} de este segmento a {@code sink}. */
  public void writeTo(Segment sink, int byteCount) {
    if (!sink.owner) throw new IllegalArgumentException();
    if (sink.limit + byteCount > SIZE) {
      // We can't fit byteCount bytes at the sink's current position. Shift sink first.
      if (sink.shared) throw new IllegalArgumentException();
      if (sink.limit + byteCount - sink.pos > SIZE) throw new IllegalArgumentException();
      System.arraycopy(sink.data, sink.pos, sink.data, 0, sink.limit - sink.pos);
      sink.limit -= sink.pos;
      sink.pos = 0;
    }
    System.arraycopy(data, pos, sink.data, sink.limit, byteCount);
    sink.limit += byteCount;
    pos += byteCount;
  }
}