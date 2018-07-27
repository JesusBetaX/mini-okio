package miniokio;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * Recibe una secuencia de bytes. Use esta interfaz para escribir datos donde sea
 * necesario: a la red, almacenamiento o un búfer en la memoria. Los sumideros pueden estar en capas
 * para transformar datos recibidos, como comprimir, encriptar, acelerar o agregar
 * encuadre de protocolo.
 *
 * <p>La mayoría del código de la aplicación no debería funcionar en un receptor directamente, sino más bien
 * {@link BufferedSink} que es tanto más eficiente como más conveniente. Utilizar
 * {@link Okio # buffer (Sink)} para envolver cualquier receptor con un buffer.
 *
 * <p>Los fregaderos son fáciles de probar: solo use un {@link Buffer} en sus pruebas, y
 * Lea de él para confirmar que recibió los datos que se esperaban.
 *
 * <h3>Comparación con OutputStream</h3>
 * Esta interfaz es funcionalmente equivalente a {@link java.io.OutputStream}.
 *
 * <p>{@code OutputStream} requiere varias capas cuando se emiten datos
 * heterogéneo: un {@code DataOutputStream} para valores primitivos, un {@code
 * BufferedOutputStream} para el almacenamiento en búfer, y {@code OutputStreamWriter} para
 * codificación de conjunto de caracteres. Esta clase usa {@code BufferedSink} para todo lo anterior.
 *
 * <p>Sink también es más fácil de aplicar: no hay {@linkplain
 * java.io.OutputStream # write (int) escritura de un solo byte} método que es incómodo para
 * implementar de manera eficiente.
 *
 * <h3>Interopera con OutputStream</h3>
 * Utilice {@link Okio # sink} para adaptar un {@code OutputStream} a un receptor. Usa {@link
 * BufferedSink # outputStream} para adaptar un receptor a {@code OutputStream}.
 */
public interface Sink extends Closeable, Flushable {
  /** Quita {@code byteCount} bytes de {@code source} y los agrega a esto. */
  void write(Buffer source, long byteCount) throws IOException;
  /** Impulsa todos los bytes almacenados en el búfer a su destino final. */
  @Override void flush() throws IOException;
  /**
   * Impulsa todos los bytes almacenados en el búfer a su destino final y lanza el
   * recursos poseídos por este fregadero. Es un error escribir un sumidero cerrado. Es
   * seguro para cerrar un fregadero más de una vez.
   */
  @Override void close() throws IOException;
}