package pruebamysql;

import java.sql.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla variantes_producto (talla, color, SKU y precio de cada producto).
 * Cada variante tiene exactamente UNA fila en inventario (donde se cuentan las unidades).
 */
public class crudVariantesProducto {

    // CREATE: guarda la variante y, en la MISMA transacción, crea su fila de inventario con 0 unidades.
    // Si algo falla, se deshace todo (rollback) para no dejar datos a medias.
    public boolean insertarDato(int id_producto, String talla, String color, String codigo_sku, double precio_venta) {
        String queryVariante = "insert into variantes_producto(id_producto, talla, color, codigo_sku, precio_venta) values(?,?,?,?,?)";
        String queryInventario = "insert into inventario(id_variante, cantidad_actual) values(?, 0)";
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false); // inicia la transacción

            PreparedStatement ps = con.prepareStatement(queryVariante, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, id_producto);
            ps.setString(2, conectionDB.nulo(talla));
            ps.setString(3, conectionDB.nulo(color));
            ps.setString(4, conectionDB.nulo(codigo_sku));
            ps.setDouble(5, precio_venta);
            ps.executeUpdate();

            // Obtenemos el id que MySQL le dio a la variante nueva
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            int idVariante = keys.getInt(1);

            PreparedStatement ps2 = con.prepareStatement(queryInventario);
            ps2.setInt(1, idVariante);
            ps2.executeUpdate();

            con.commit(); // todo bien: se guardan los dos registros
            return true;
        } catch (SQLException ex) {
            conectionDB.deshacer(con);
            conectionDB.registrarError(ex);
            return false;
        } finally {
            conectionDB.cerrar(con);
        }
    }

    // La última columna (id_producto) se oculta en el formulario; sirve para seleccionar el combo
    public DefaultTableModel consultarDatos() {
        String[] titulos = {"ID", "Producto", "Talla", "Color", "SKU", "Precio Venta", "id_producto"};
        String query = "select v.id_variante, p.nombre_producto, v.talla, v.color, v.codigo_sku, v.precio_venta, v.id_producto "
                + "from variantes_producto v join productos p on p.id_producto = v.id_producto "
                + "order by p.nombre_producto, v.talla";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Variantes que todavía NO tienen fila en inventario (para el formulario de inventario)
    public DefaultComboBoxModel<ItemCombo> modeloComboSinInventario() {
        String query = "select v.id_variante, concat(ifnull(v.codigo_sku,'sin SKU'), ' - ', p.nombre_producto, ' (', ifnull(v.talla,'-'), '/', ifnull(v.color,'-'), ')') "
                + "from variantes_producto v join productos p on p.id_producto = v.id_producto "
                + "where v.id_variante not in (select id_variante from inventario) order by p.nombre_producto";
        return conectionDB.consultarCombo(query);
    }

    public boolean actualizarDatos(int id, int id_producto, String talla, String color, String codigo_sku, double precio_venta) {
        String query = "update variantes_producto set id_producto=?, talla=?, color=?, codigo_sku=?, precio_venta=? where id_variante=?";
        return conectionDB.ejecutar(query, id_producto, conectionDB.nulo(talla), conectionDB.nulo(color),
                conectionDB.nulo(codigo_sku), precio_venta, id);
    }

    // Regla: la variante no se puede eliminar si ya tuvo compras/ventas o si todavía tiene unidades en inventario.
    // (La fila de inventario vacía y sin movimientos sí se borra junto con la variante.)
    public String motivoBloqueo(int id) {
        int compras = conectionDB.contar("select count(*) from detalle_compras d join inventario i on i.id_inventario = d.id_inventario where i.id_variante = ?", id);
        int ventas = conectionDB.contar("select count(*) from detalle_ventas d join inventario i on i.id_inventario = d.id_inventario where i.id_variante = ?", id);
        int stock = conectionDB.contar("select ifnull(sum(cantidad_actual),0) from inventario where id_variante = ?", id);
        if (compras > 0 || ventas > 0) {
            return "La variante tiene " + compras + " compra(s) y " + ventas + " venta(s) registrada(s).";
        }
        if (stock > 0) {
            return "La variante todavía tiene " + stock + " unidad(es) en inventario.";
        }
        return null;
    }

    // DELETE: primero borra su inventario (vacío) y luego la variante, en una sola transacción
    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false);
            PreparedStatement ps1 = con.prepareStatement("delete from inventario where id_variante = ?");
            ps1.setInt(1, id);
            ps1.executeUpdate();
            PreparedStatement ps2 = con.prepareStatement("delete from variantes_producto where id_variante = ?");
            ps2.setInt(1, id);
            ps2.executeUpdate();
            con.commit();
            return true;
        } catch (SQLException ex) {
            conectionDB.deshacer(con);
            conectionDB.registrarError(ex);
            return false;
        } finally {
            conectionDB.cerrar(con);
        }
    }
}
