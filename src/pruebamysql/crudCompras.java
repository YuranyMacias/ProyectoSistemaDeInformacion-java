package pruebamysql;

import java.sql.*;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla compras (entrada de mercancía de un proveedor).
 *
 * REGLA DE INVENTARIO: el stock SUMA solo cuando la compra está en estado "Recibido".
 *   - Pendiente / Cancelado  -> no tocan el inventario.
 *   - Cambiar a Recibido     -> suma las cantidades a inventario.cantidad_actual.
 *   - Salir de Recibido      -> resta (revierte) esas cantidades.
 *   - Eliminar una Recibida  -> también revierte las cantidades.
 * Todo se hace en una transacción: o se guarda todo, o no se guarda nada.
 */
public class crudCompras {

    private final crudDetalleCompras detalle = new crudDetalleCompras();

    // CREATE: guarda la compra + todos sus ítems (+ suma stock si ya está Recibida)
    public boolean insertarDato(int id_proveedor, int id_empleado, String numero_factura_proveedor, String estado, List<LineaDetalle> lineas) {
        String query = "insert into compras(id_proveedor, id_empleado, numero_factura_proveedor, estado) values(?,?,?,?)";
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false); // inicia la transacción

            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, id_proveedor);
            ps.setInt(2, id_empleado);
            ps.setString(3, conectionDB.nulo(numero_factura_proveedor));
            ps.setString(4, estado);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys(); // id de la compra recién creada
            keys.next();
            int idCompra = keys.getInt(1);

            for (LineaDetalle linea : lineas) {
                detalle.insertarDato(con, idCompra, linea);
                if (estado.equals("Recibido")) {
                    crudInventario.sumarStock(con, linea.getIdInventario(), linea.getCantidad());
                }
            }

            con.commit(); // todo bien: se guarda
            return true;
        } catch (SQLException ex) {
            conectionDB.deshacer(con); // algo falló: se deshace todo
            conectionDB.registrarError(ex);
            return false;
        } finally {
            conectionDB.cerrar(con);
        }
    }

    // READ: historial de compras con el total de cada una
    public DefaultTableModel consultarDatos() {
        String[] titulos = {"N°", "Fecha", "Proveedor", "Factura Prov.", "Registrada por", "Estado", "Total", "id_proveedor"};
        String query = "select c.id_compra, c.fecha_compra, p.nombre, c.numero_factura_proveedor, e.nombre, c.estado, "
                + "(select ifnull(sum(d.total_pagar),0) from detalle_compras d where d.id_compra = c.id_compra), c.id_proveedor "
                + "from compras c "
                + "join proveedores p on p.id_proveedor = c.id_proveedor "
                + "join empleados e on e.id_empleado = c.id_empleado "
                + "order by c.id_compra desc";
        return conectionDB.consultarModelo(query, titulos);
    }

    // UPDATE: cambia proveedor, factura y estado. Los ítems no se editan (para eso se elimina y se crea de nuevo).
    // Si cambia el estado desde/hacia "Recibido", ajusta el stock.
    public boolean actualizarDatos(int id, int id_proveedor, String numero_factura_proveedor, String nuevoEstado) {
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false);

            // 1. Estado actual (con FOR UPDATE se bloquea la fila mientras dura la transacción)
            PreparedStatement psEstado = con.prepareStatement("select estado from compras where id_compra = ? for update");
            psEstado.setInt(1, id);
            ResultSet rs = psEstado.executeQuery();
            if (!rs.next()) {
                conectionDB.deshacer(con);
                conectionDB.ultimoError = "La compra ya no existe.";
                return false;
            }
            String estadoAnterior = rs.getString(1);

            // 2. Actualiza los datos de la compra
            PreparedStatement ps = con.prepareStatement("update compras set id_proveedor=?, numero_factura_proveedor=?, estado=? where id_compra=?");
            ps.setInt(1, id_proveedor);
            ps.setString(2, conectionDB.nulo(numero_factura_proveedor));
            ps.setString(3, nuevoEstado);
            ps.setInt(4, id);
            ps.executeUpdate();

            // 3. Ajusta el inventario solo si cambió "estar Recibido"
            boolean eraRecibida = estadoAnterior.equals("Recibido");
            boolean esRecibida = nuevoEstado.equals("Recibido");
            if (eraRecibida != esRecibida) {
                if (!cambiarStock(con, id, esRecibida)) {
                    conectionDB.deshacer(con);
                    conectionDB.ultimoError = "No se puede cambiar el estado: parte de esta mercancía ya se vendió\n"
                            + "y el stock actual no alcanza para revertir la compra.";
                    return false;
                }
            }

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

    // DELETE: elimina la compra y sus ítems. Si estaba Recibida, primero revierte el stock.
    public boolean eliminarDatos(int id) {
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false);

            PreparedStatement psEstado = con.prepareStatement("select estado from compras where id_compra = ? for update");
            psEstado.setInt(1, id);
            ResultSet rs = psEstado.executeQuery();
            if (!rs.next()) {
                conectionDB.deshacer(con);
                conectionDB.ultimoError = "La compra ya no existe.";
                return false;
            }

            if (rs.getString(1).equals("Recibido") && !cambiarStock(con, id, false)) {
                conectionDB.deshacer(con);
                conectionDB.ultimoError = "No se puede eliminar: parte de esta mercancía ya se vendió\n"
                        + "y el stock actual no alcanza para revertir la compra.";
                return false;
            }

            // Primero los ítems (hijos) y luego la compra (padre)
            PreparedStatement ps1 = con.prepareStatement("delete from detalle_compras where id_compra = ?");
            ps1.setInt(1, id);
            ps1.executeUpdate();
            PreparedStatement ps2 = con.prepareStatement("delete from compras where id_compra = ?");
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

    // Suma (sumar = true) o resta (sumar = false) al inventario todos los ítems de una compra.
    // Devuelve false si al restar no alcanza el stock.
    private boolean cambiarStock(Connection con, int id_compra, boolean sumar) throws SQLException {
        for (LineaDetalle linea : detalle.consultarLineas(con, id_compra)) {
            if (sumar) {
                crudInventario.sumarStock(con, linea.getIdInventario(), linea.getCantidad());
            } else if (!crudInventario.restarStock(con, linea.getIdInventario(), linea.getCantidad())) {
                return false;
            }
        }
        return true;
    }
}
