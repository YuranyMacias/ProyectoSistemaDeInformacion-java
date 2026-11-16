package pruebamysql;

import java.security.MessageDigest;

/**
 * Convierte las contraseñas a SHA-256 para no guardarlas en texto plano.
 * Es la misma función que SHA2(texto, 256) de MySQL.
 */
public class Seguridad {

    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b)); // cada byte a 2 letras hexadecimales
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo cifrar la contraseña", ex);
        }
    }
}
