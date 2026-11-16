package pruebamysql;

import java.sql.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla inventario (cantidad exacta de unidades de cada variante)
 * y operaciones de stock que usan las compras y las ventas.
 */
public class crudInventario {

    // Texto que describe una variante: "Producto Talla Color"
    private static final String DESCRIPCION =
            "concat(p.nombre_producto, ' ', ifnull(v.talla,''), ' ', ifnull(v.color,''))";

    public boolean insertarDato(int id_variante, int cantidad_actual) {
        String query = "insert into inventario(id_variante, cantidad_actual) values(?,?)";
        return conectionDB.ejecutar(query, id_variante, cantidad_actual);
    }

    // READ: muestra el inventario. "filtro" busca por SKU o nombre del producto ("" = todo)
    public DefaultTableModel consultarDatos(String filtro) {
        String[] titulos = {"ID", "SKU", "Producto", "Talla", "Color", "Stock", "Estado", "id_variante"};
        String query = "select i.id_inventario, v.codigo_sku, p.nombre_producto, v.talla, v.color, i.cantidad_actual, "
                + "case when i.cantidad_actual = 0 then 'Agotado' when i.cantidad_actual <= 5 then 'Stock bajo' else 'Disponible' end, "
                + "i.id_variante "
                + "from inventario i "
                + "join variantes_producto v on v.id_variante = i.id_variante "
                + "join productos p on p.id_producto = v.id_producto "
                + "where v.codigo_sku like ? or p.nombre_producto like ? "
                + "order by p.nombre_producto, v.talla";
        String patron = "%" + filtro.trim() + "%";
        return conectionDB.consultarModelo(query, titulos, patron, patron);
    }

    public DefaultTableModel consultarDatos() {
        return consultarDatos("");
    }

    // Prendas agotadas o con pocas unidades (5 o menos). Se muestra en el panel de control.
    public DefaultTableModel consultarStockBajo() {
        String[] titulos = {"SKU", "Producto", "Talla", "Color", "Stock"};
        String query = "select v.codigo_sku, p.nombre_producto, v.talla, v.color, i.cantidad_actual "
                + "from inventario i "
                + "join variantes_producto v on v.id_variante = i.id_variante "
                + "join productos p on p.id_producto = v.id_producto "
                + "where i.cantidad_actual <= 5 order by i.cantidad_actual, p.nombre_producto";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Lista de ítems de inventario para el JComboBox de compras: "SKU - Producto (talla/color)"
    public DefaultComboBoxModel<ItemCombo> modeloCombo() {
        String query = "select i.id_inventario, concat(ifnull(v.codigo_sku,'sin SKU'), ' - ', p.nombre_producto, ' (', ifnull(v.talla,'-'), '/', ifnull(v.color,'-'), ')') "
                + "from inventario i "
                + "join variantes_producto v on v.id_variante = i.id_variante "
                + "join productos p on p.id_producto = v.id_producto "
                + "order by p.nombre_producto, v.talla";
        return conectionDB.consultarCombo(query);
    }

    // Ajuste manual de la cantidad (solo administrador): por ejemplo después de un conteo físico
    public boolean actualizarDatos(int id, int id_variante, int cantidad_actual) {
        String query = "update inventario set id_variante=?, cantidad_actual=? where id_inventario=?";
        return conectionDB.ejecutar(query, id_variante, cantidad_actual, id);
    }

    // Regla: no se elimina un inventario que tiene compras/ventas registradas o unidades en stock
    public String motivoBloqueo(int id) {
        int compras = conectionDB.contar("select count(*) from detalle_compras where id_inventario = ?", id);
        int ventas = conectionDB.contar("select count(*) from detalle_ventas where id_inventario = ?", id);
        int stock = conectionDB.contar("select cantidad_actual from inventario where id_inventario = ?", id);
        if (compras > 0 || ventas > 0) {
            return "Este inventario tiene " + compras + " compra(s) y " + ventas + " venta(s) registrada(s).";
        }
        if (stock > 0) {
            return "Este inventario todavía tiene " + stock + " unidad(es) en stock.";
        }
        return null;
    }

    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        return conectionDB.ejecutar("delete from inventario where id_inventario = ?", id);
    }

    // =====================================================================
    // OPERACIONES DE STOCK (se usan dentro de la transacción de compras y ventas)
    // =====================================================================

    // COMPRA: suma unidades a cantidad_actual
    public static void sumarStock(Connection con, int idInventario, int cantidad) throws SQLException {
        String query = "update inventario set cantidad_actual = cantidad_actual + ? where id_inventario = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idInventario);
            ps.executeUpdate();
        }
    }

    // VENTA: resta unidades a cantidad_actual, pero SOLO si alcanzan.
    // Devuelve false si no hay suficiente stock (no se cambia nada).
    public static boolean restarStock(Connection con, int idInventario, int cantidad) throws SQLException {
        String query = "update inventario set cantidad_actual = cantidad_actual - ? "
                + "where id_inventario = ? and cantidad_actual >= ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idInventario);
            ps.setInt(3, cantidad);
            return ps.executeUpdate() == 1;
        }
    }

    // =====================================================================
    // BÚSQUEDAS PARA ARMAR LAS LÍNEAS DE COMPRA / VENTA
    // =====================================================================

    // Busca un ítem por su SKU (lo usa el punto de venta). Devuelve null si no existe.
    // En "precio" viene el precio de venta y en "stockDisponible" las unidades que hay.
    public LineaDetalle buscarPorSku(String sku) {
        String query = "select i.id_inventario, v.codigo_sku, " + DESCRIPCION + ", v.precio_venta, i.cantidad_actual "
                + "from inventario i "
                + "join variantes_producto v on v.id_variante = i.id_variante "
                + "join productos p on p.id_producto = v.id_producto "
                + "where v.codigo_sku = ?";
        return buscarLinea(query, sku);
    }

    // Igual que la anterior pero por id de inventario (lo usa el formulario de compras)
    public LineaDetalle buscarPorId(int idInventario) {
        String query = "select i.id_inventario, v.codigo_sku, " + DESCRIPCION + ", v.precio_venta, i.cantidad_actual "
                + "from inventario i "
                + "join variantes_producto v on v.id_variante = i.id_variante "
                + "join productos p on p.id_producto = v.id_producto "
                + "where i.id_inventario = ?";
        return buscarLinea(query, idInventario);
    }

    private LineaDetalle buscarLinea(String query, Object parametro) {
        try (Connection con = conectionDB.conectar();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setObject(1, parametro);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LineaDetalle linea = new LineaDetalle(rs.getInt(1), rs.getString(2), rs.getString(3).trim(), 1, rs.getDouble(4));
                linea.setStockDisponible(rs.getInt(5));
                return linea;
            }
        } catch (SQLException ex) {
            conectionDB.registrarError(ex);
        }
        return null;
    }
}
