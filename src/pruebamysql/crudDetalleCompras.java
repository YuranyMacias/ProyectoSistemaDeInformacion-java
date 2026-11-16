package pruebamysql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * Tabla detalle_compras: los ítems de cada compra.
 * Los detalles se guardan, consultan y eliminan JUNTO con la compra (ver crudCompras),
 * porque cada cambio en un ítem debe reflejarse también en el stock del inventario.
 */
public class crudDetalleCompras {

    // CREATE: guarda un ítem de la compra. Usa la conexión de la transacción de crudCompras.
    public void insertarDato(Connection con, int id_compra, LineaDetalle linea) throws SQLException {
        String query = "insert into detalle_compras(id_compra, cantidad, costo_unitario, total_pagar, id_inventario) values(?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, id_compra);
            ps.setInt(2, linea.getCantidad());
            ps.setDouble(3, linea.getPrecio());
            ps.setDouble(4, linea.getSubtotal()); // total = cantidad x costo
            ps.setInt(5, linea.getIdInventario());
            ps.executeUpdate();
        }
    }

    // READ: ítems de una compra en forma de tabla
    public DefaultTableModel consultarDatos(int id_compra) {
        String[] titulos = {"SKU", "Producto", "Cantidad", "Costo Unit.", "Total"};
        String query = "select v.codigo_sku, concat(p.nombre_producto, ' ', ifnull(v.talla,''), ' ', ifnull(v.color,'')), "
                + "d.cantidad, d.costo_unitario, d.total_pagar "
                + "from detalle_compras d "
                + "join inventario i on i.id_inventario = d.id_inventario "
                + "join variantes_producto v on v.id_variante = i.id_variante "
                + "join productos p on p.id_producto = v.id_producto "
                + "where d.id_compra = ?";
        return conectionDB.consultarModelo(query, titulos, id_compra);
    }

    // Ítems de una compra como lista de objetos (usando una conexión ya abierta)
    public List<LineaDetalle> consultarLineas(Connection con, int id_compra) throws SQLException {
        List<LineaDetalle> lista = new ArrayList<>();
        String query = "select d.id_inventario, v.codigo_sku, concat(p.nombre_producto, ' ', ifnull(v.talla,''), ' ', ifnull(v.color,'')), "
                + "d.cantidad, d.costo_unitario "
                + "from detalle_compras d "
                + "join inventario i on i.id_inventario = d.id_inventario "
                + "join variantes_producto v on v.id_variante = i.id_variante "
                + "join productos p on p.id_producto = v.id_producto "
                + "where d.id_compra = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, id_compra);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new LineaDetalle(rs.getInt(1), rs.getString(2), rs.getString(3).trim(), rs.getInt(4), rs.getDouble(5)));
            }
        }
        return lista;
    }

    // Igual, pero abriendo su propia conexión (para mostrar una compra en el formulario)
    public List<LineaDetalle> consultarLineas(int id_compra) {
        try (Connection con = conectionDB.conectar()) {
            return consultarLineas(con, id_compra);
        } catch (SQLException ex) {
            conectionDB.registrarError(ex);
            return new ArrayList<>();
        }
    }
}
