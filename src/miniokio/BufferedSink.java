package miniokio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Un receptor que mantiene un buffer internamente para que las llamadas se 
 * puedan escribir pequeñas partes sin una penalización de rendimiento.
 */
public interface BufferedSink extends Sink {
  /** Devuelve el buffer interno de este receptor. */
  Buffer buffer();
  /**
   * Al igual que {@link OutputStream # write (byte[])}, esto escribe una 
   * matriz de bytes completa para este fregadero
   */
  BufferedSink write(byte[] source) throws IOException;
  /**
   * Al igual que {@link OutputStream # write (byte[], int, int)}, esto escribe {@code byteCount}
   * bytes de {@code source}, comenzando en {@code offset}.
   */
  BufferedSink write(byte[] source, int offset, int byteCount) throws IOException;
  /** 
   * Elimina todos los bytes de {@code source} y los agrega a este receptor. 
   * Devuelve el * número de bytes leídos que será 0 si se agota {@code source}. 
   */
  long writeAll(Source source) throws IOException;
  /** Escribe un byte en este receptor. */
  BufferedSink write(int b) throws IOException;

  void write(String str, Charset charset) throws IOException;
}