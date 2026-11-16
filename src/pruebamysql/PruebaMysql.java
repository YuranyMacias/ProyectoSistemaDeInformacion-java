package pruebamysql;

import javax.swing.SwingUtilities;
import vista.EstiloUI;
import vista.LoginTienda;

/**
 * Clase principal del proyecto: al ejecutarla se abre la pantalla de inicio de sesión.
 * (Antes tenía pruebas comentadas; ahora cada formulario ya usa su clase crud.)
 */
public class PruebaMysql {

    public static void main(String[] args) {
        EstiloUI.instalar(); // colores de los cuadros de mensaje
        // Swing debe iniciarse en su propio hilo de eventos
        SwingUtilities.invokeLater(() -> new LoginTienda().setVisible(true));
    }
}
