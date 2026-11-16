package pruebamysql;

import java.sql.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * Clase que maneja la conexión con MySQL y contiene las herramientas
 * que usan todas las clases crud (para no repetir código).
 */
public class conectionDB {

    static String usuario = "root";
    static String password = "yumacc123";
    static String url = "jdbc:mysql://localhost:3306/sistema_inventario";

    // Guarda el último mensaje de error (ya redactado para el usuario).
    // Los formularios lo muestran cuando una operación devuelve false.
    public static String ultimoError = "";

    // =====================================================================
    // CONEXIÓN
    // =====================================================================

    // Abre una conexión nueva. Si falla lanza SQLException (cada crud la captura).
    // IMPORTANTE: quien la pide debe cerrarla (se usa try-with-resources).
    public static Connection conectar() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("No se encontró el driver de MySQL. Agrega el .jar del conector a las Libraries del proyecto.");
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    // =====================================================================
    // HERRAMIENTAS PARA CONSULTAS SIMPLES
    // =====================================================================

    // Pone los parámetros (?, ?, ...) de una consulta preparada
    private static void ponerParametros(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    // Ejecuta un INSERT, UPDATE o DELETE. Devuelve true si todo salió bien.
    public static boolean ejecutar(String query, Object... params) {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(query)) {
            ponerParametros(ps, params);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            registrarError(ex);
            return false;
        }
    }

    // Ejecuta un SELECT y devuelve el resultado listo para un JTable.
    // La cantidad de columnas es la cantidad de títulos (el SELECT debe traer ese mismo número).
    public static DefaultTableModel consultarModelo(String query, String[] titulos, Object... params) {
        // Modelo que NO deja editar las celdas con doble clic
        DefaultTableModel modelo = new DefaultTableModel(null, titulos) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(query)) {
            ponerParametros(ps, params);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String[] fila = new String[titulos.length];
                for (int i = 0; i < titulos.length; i++) {
                    fila[i] = rs.getString(i + 1);
                    // Algunas versiones del conector muestran las fechas con ".0" al final: se quita
                    if (fila[i] != null && fila[i].matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.0")) {
                        fila[i] = fila[i].substring(0, 19);
                    }
                }
                modelo.addRow(fila);
            }
        } catch (SQLException ex) {
            registrarError(ex);
        }
        return modelo;
    }

    // Ejecuta un SELECT de 2 columnas (id, texto) y lo devuelve listo para un JComboBox
    public static DefaultComboBoxModel<ItemCombo> consultarCombo(String query, Object... params) {
        DefaultComboBoxModel<ItemCombo> modelo = new DefaultComboBoxModel<>();
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(query)) {
            ponerParametros(ps, params);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelo.addElement(new ItemCombo(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException ex) {
            registrarError(ex);
        }
        return modelo;
    }

    // Ejecuta un SELECT COUNT(*) (o similar) y devuelve el número. Si falla devuelve 0.
    public static int contar(String query, Object... params) {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(query)) {
            ponerParametros(ps, params);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            registrarError(ex);
        }
        return 0;
    }

    // Igual que contar pero devuelve un número decimal (para sumas de dinero)
    public static double valorDecimal(String query, Object... params) {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(query)) {
            ponerParametros(ps, params);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            registrarError(ex);
        }
        return 0;
    }

    // Convierte un texto vacío en null (para columnas opcionales o UNIQUE como el documento)
    public static String nulo(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        return texto.trim();
    }

    // =====================================================================
    // TRANSACCIONES (las usan compras y ventas)
    // =====================================================================

    // Deshace todos los cambios de la transacción si algo falló
    public static void deshacer(Connection con) {
        try {
            if (con != null) {
                con.rollback();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Cierra la conexión sin lanzar errores
    public static void cerrar(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // =====================================================================
    // MENSAJES DE ERROR
    // =====================================================================

    // Convierte el error técnico de MySQL en un mensaje entendible
    public static void registrarError(SQLException ex) {
        ex.printStackTrace(); // el detalle técnico queda en la consola de NetBeans
        switch (ex.getErrorCode()) {
            case 1062: // clave duplicada
                ultimoError = "Ya existe un registro con ese dato (documento, SKU, usuario o nombre repetido).";
                break;
            case 1451: // intenta borrar un padre que tiene hijos
                ultimoError = "No se puede eliminar este registro porque está asociado a otros registros.";
                break;
            case 1452: // intenta guardar un hijo sin padre
                ultimoError = "El registro relacionado no existe (revisa la selección).";
                break;
            case 3819: // restricción CHECK (stock negativo)
                ultimoError = "La cantidad no puede quedar en negativo.";
                break;
            case 1406: // dato muy largo
                ultimoError = "Uno de los textos es demasiado largo para la base de datos.";
                break;
            default:
                ultimoError = "Error de base de datos: " + ex.getMessage();
        }
    }
}
