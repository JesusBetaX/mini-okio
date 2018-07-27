package miniokio;

/**
 * Una colección de segmentos no utilizados, necesarios para evitar la rotación de GC y el relleno cero.
 * Este grupo es un singleton estático seguro para hilos.
 */
final class SegmentPool {
  /** El número máximo de bytes para agrupar. */
  // TODO: ¿Es 64 KiB un buen tamaño máximo? ¿Alguna vez tenemos tantos segmentos inactivos?
  static final long MAX_SIZE = 64 * 1024; // 64 KiB.
  /** Lista de segmentos enlazados individualmente. */
  static Segment next;
  /** Total de bytes en este grupo. */
  static long byteCount;
  private SegmentPool() {
  }
  static Segment take() {
    synchronized (SegmentPool.class) {
      if (next != null) {
        Segment result = next;
        next = result.next;
        result.next = null;
        byteCount -= Segment.SIZE;
        return result;
      }
    }
    return new Segment(); // El grupo está vacío. No llene a cero mientras mantiene un bloqueo.
  }
  static void recycle(Segment segment) {
    if (segment.next != null || segment.prev != null) throw new IllegalArgumentException();
    if (segment.shared) return; // Este segmento no puede ser reciclado.
    synchronized (SegmentPool.class) {
      if (byteCount + Segment.SIZE > MAX_SIZE) return; // Pool is full.
      byteCount += Segment.SIZE;
      segment.next = next;
      segment.pos = segment.limit = 0;
      next = segment;
    }
  }
}