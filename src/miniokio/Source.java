package miniokio;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Proporciona una secuencia de bytes. Usa esta interfaz para leer datos de donde sea
 * está ubicado: desde la red, el almacenamiento o un búfer en la memoria. Las fuentes pueden
 * estar en capas para transformar los datos suministrados, como para descomprimir, descifrar o
 * eliminar el encuadre de protocolo.
 *
 * <p> La mayoría de las aplicaciones no deberían operar en una fuente directamente, sino más bien
 * {@link BufferedSource} que es tanto más eficiente como más conveniente. Utilizar
 * {@link Okio # buffer (Source)} para envolver cualquier fuente con un búfer.
 *
 * <p> Las fuentes son fáciles de probar: solo use un {@link Buffer} en sus pruebas, y
 * complétalo con los datos que tu aplicación debe leer.
 *
 * <h3>Comparación con InputStream</h3>
 * Esta interfaz es funcionalmente equivalente a {@link java.io.InputStream}.
 *
 * <p>{@code InputStream} requiere varias capas cuando se consumen datos
 * heterogéneo: un {@code DataInputStream} para valores primitivos, un {@code
 * BufferedInputStream} para el almacenamiento en búfer, y {@code InputStreamReader} para
 * cadenas. Esta clase usa {@code BufferedSource} para todo lo anterior.
 *
 * <p>Source evita lo imposible de implementar {@linkplain
 * java.io.InputStream # método available available ()}. En cambio, los llamantes especifican
 * cuántos bytes requieren {{link BufferedSource # require}.
 *
 * <p>Source omite la marca {@linkplain java.io.InputStream # insegura para componer
 * marque y restablezca} estado al que {@code InputStream} rastrea; llamadores en su lugar
 * simplemente guarde lo que necesitan.
 *
 * <p>Al implementar una fuente, no debes preocuparte por la {@linkplain
 * java.io.InputStream # leer lectura de byte único} método que es incómodo para
 * implementar de manera eficiente y que devuelve uno de los 257 valores posibles.
 *
 * <p>Y la fuente tiene un método más fuerte {@code skip}: {@link BufferedSource # skip}
 * no regresará prematuramente.
 *
 * <h3>Interopera con InputStream</h3>
 * Use {@link Okio # source} para adaptar un {@code InputStream} a una fuente. Utilizar
 * {@link BufferedSource # inputStream} para adaptar una fuente a un código {@
 * Flujo de entrada}.
 */
public interface Source extends Closeable {
  InputStream inputStream();
  /**
   * Quita al menos 1, y hasta {@code byteCount} bytes de esto y anexa
   * ellos a {@code sink}. Devuelve la cantidad de bytes leídos, o -1 si esto
   * la fuente está agotada.
   */
  long read(Buffer sink, long byteCount) throws IOException;
  /**
   * Cierra esta fuente y libera los recursos que posee esta fuente. Es un
   * error para leer una fuente cerrada. Es seguro cerrar una fuente más de una vez.
   */
  @Override void close() throws IOException;
}