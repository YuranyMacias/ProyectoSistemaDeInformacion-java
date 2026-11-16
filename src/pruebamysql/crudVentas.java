package pruebamysql;

import java.sql.*;
import java.util.List;
import java.util.Locale;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla ventas (salida de mercancía hacia un cliente).
 *
 * REGLA DE INVENTARIO: el stock RESTA mientras la venta NO esté "Cancelada".
 *   - Pendiente / Pagada     -> las unidades quedan descontadas de inventario.cantidad_actual.
 *   - Cambiar a Cancelada    -> devuelve las unidades al inventario.
 *   - Salir de Cancelada     -> vuelve a descontar (si hay stock suficiente).
 *   - Eliminar una venta     -> devuelve las unidades si no estaba Cancelada.
 * Nunca se vende más de lo que hay: si no alcanza el stock, se cancela todo el guardado.
 */
public class crudVentas {

    private final crudDetalleVentas detalle = new crudDetalleVentas();

    // Id de la última venta guardada con éxito (sirve para mostrar su factura)
    public int ultimoIdVenta = 0;

    // CREATE: guarda la venta + todos sus ítems y descuenta el stock
    public boolean insertarDato(int id_cliente, int id_empleado, String metodo_pago, String estado, List<LineaDetalle> lineas) {
        String query = "insert into ventas(id_cliente, id_empleado, metodo_pago, estado) values(?,?,?,?)";
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false); // inicia la transacción

            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, id_cliente);
            ps.setInt(2, id_empleado);
            ps.setString(3, metodo_pago); // Efectivo, Tarjeta o Transferencia
            ps.setString(4, estado);      // Pendiente, Pagada o Cancelada
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys(); // id de la venta recién creada
            keys.next();
            int idVenta = keys.getInt(1);

            for (LineaDetalle linea : lineas) {
                detalle.insertarDato(con, idVenta, linea);
                // Resta del inventario (falla si no hay suficientes unidades)
                if (!estado.equals("Cancelada")
                        && !crudInventario.restarStock(con, linea.getIdInventario(), linea.getCantidad())) {
                    conectionDB.deshacer(con);
                    conectionDB.ultimoError = "Stock insuficiente para: " + linea.getDescripcion()
                            + "\nSe pidieron " + linea.getCantidad() + " unidad(es).";
                    return false;
                }
            }

            con.commit(); // todo bien: se guarda
            ultimoIdVenta = idVenta;
            return true;
        } catch (SQLException ex) {
            conectionDB.deshacer(con);
            conectionDB.registrarError(ex);
            return false;
        } finally {
            conectionDB.cerrar(con);
        }
    }

    // READ: historial de ventas con el total de cada una
    public DefaultTableModel consultarDatos() {
        String[] titulos = {"N°", "Fecha", "Cliente", "Vendedor", "Método de Pago", "Estado", "Total", "id_cliente"};
        String query = "select v.id_venta, v.fecha_venta, c.nombre, e.nombre, v.metodo_pago, v.estado, "
                + "(select ifnull(sum(d.total_pagar),0) from detalle_ventas d where d.id_venta = v.id_venta), v.id_cliente "
                + "from ventas v "
                + "join clientes c on c.id_cliente = v.id_cliente "
                + "join empleados e on e.id_empleado = v.id_empleado "
                + "order by v.id_venta desc";
        return conectionDB.consultarModelo(query, titulos);
    }

    // UPDATE: cambia cliente, método de pago y estado. Los ítems no se editan (para eso se elimina y se crea de nuevo).
    // Si cambia desde/hacia "Cancelada", ajusta el stock.
    public boolean actualizarDatos(int id, int id_cliente, String metodo_pago, String nuevoEstado) {
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false);

            PreparedStatement psEstado = con.prepareStatement("select estado from ventas where id_venta = ? for update");
            psEstado.setInt(1, id);
            ResultSet rs = psEstado.executeQuery();
            if (!rs.next()) {
                conectionDB.deshacer(con);
                conectionDB.ultimoError = "La venta ya no existe.";
                return false;
            }
            String estadoAnterior = rs.getString(1);

            PreparedStatement ps = con.prepareStatement("update ventas set id_cliente=?, metodo_pago=?, estado=? where id_venta=?");
            ps.setInt(1, id_cliente);
            ps.setString(2, metodo_pago);
            ps.setString(3, nuevoEstado);
            ps.setInt(4, id);
            ps.executeUpdate();

            boolean estabaCancelada = estadoAnterior.equals("Cancelada");
            boolean estaCancelada = nuevoEstado.equals("Cancelada");
            if (estabaCancelada != estaCancelada) {
                // Si se cancela: devuelve (suma). Si se reactiva: vuelve a descontar (resta).
                boolean devolver = estaCancelada;
                if (!cambiarStock(con, id, devolver)) {
                    conectionDB.deshacer(con);
                    conectionDB.ultimoError = "No se puede reactivar la venta: ya no hay stock suficiente de alguna prenda.";
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

    // DELETE: elimina la venta y sus ítems. Si no estaba Cancelada, devuelve las unidades al inventario.
    public boolean eliminarDatos(int id) {
        Connection con = null;
        try {
            con = conectionDB.conectar();
            con.setAutoCommit(false);

            PreparedStatement psEstado = con.prepareStatement("select estado from ventas where id_venta = ? for update");
            psEstado.setInt(1, id);
            ResultSet rs = psEstado.executeQuery();
            if (!rs.next()) {
                conectionDB.deshacer(con);
                conectionDB.ultimoError = "La venta ya no existe.";
                return false;
            }

            if (!rs.getString(1).equals("Cancelada")) {
                cambiarStock(con, id, true); // devolver unidades (sumar nunca falla)
            }

            PreparedStatement ps1 = con.prepareStatement("delete from detalle_ventas where id_venta = ?");
            ps1.setInt(1, id);
            ps1.executeUpdate();
            PreparedStatement ps2 = con.prepareStatement("delete from ventas where id_venta = ?");
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

    // Devuelve (devolver = true -> suma) o descuenta (devolver = false -> resta) todos los ítems de una venta.
    // Devuelve false si al restar no alcanza el stock.
    private boolean cambiarStock(Connection con, int id_venta, boolean devolver) throws SQLException {
        for (LineaDetalle linea : detalle.consultarLineas(con, id_venta)) {
            if (devolver) {
                crudInventario.sumarStock(con, linea.getIdInventario(), linea.getCantidad());
            } else if (!crudInventario.restarStock(con, linea.getIdInventario(), linea.getCantidad())) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================
    // FACTURA
    // =====================================================================

    // Convierte un número a dinero: 190000 -> "$ 190,000"
    private String dinero(double valor) {
        return String.format(Locale.US, "$ %,.0f", valor);
    }

    // Arma el texto de la factura de una venta (para mostrarla o imprimirla)
    public String generarFactura(int id_venta) {
        String query = "select v.fecha_venta, c.nombre, c.tipo_documento, c.numero_documento, e.nombre, v.metodo_pago, v.estado "
                + "from ventas v "
                + "join clientes c on c.id_cliente = v.id_cliente "
                + "join empleados e on e.id_empleado = v.id_empleado "
                + "where v.id_venta = ?";
        try (Connection con = conectionDB.conectar();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, id_venta);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return "La venta N° " + id_venta + " no existe.";
            }

            String linea = "------------------------------------------------------------\n";
            StringBuilder f = new StringBuilder();
            f.append("                    URBAN STORE\n");
            f.append("               Tienda de ropa - Factura de venta\n");
            f.append(linea);
            f.append(String.format("Factura N°: %06d%n", id_venta));
            f.append("Fecha:      ").append(rs.getString(1).substring(0, 19)).append("\n");
            f.append("Cliente:    ").append(rs.getString(2)).append("\n");
            f.append("Documento:  ").append(rs.getString(3)).append(" ").append(rs.getString(4) == null ? "" : rs.getString(4)).append("\n");
            f.append("Atendió:    ").append(rs.getString(5)).append("\n");
            f.append("Pago:       ").append(rs.getString(6)).append("  (").append(rs.getString(7)).append(")\n");
            f.append(linea);
            f.append(String.format("%-28s %4s %12s %12s%n", "Producto", "Cant", "Precio", "Subtotal"));
            f.append(linea);

            double total = 0;
            for (LineaDetalle item : detalle.consultarLineas(con, id_venta)) {
                String nombre = item.getDescripcion();
                if (nombre.length() > 28) {
                    nombre = nombre.substring(0, 28); // recorta para que quepa en la columna
                }
                f.append(String.format("%-28s %4d %12s %12s%n", nombre, item.getCantidad(),
                        dinero(item.getPrecio()), dinero(item.getSubtotal())));
                total += item.getSubtotal();
            }
            f.append(linea);
            f.append(String.format("%-28s %30s%n", "TOTAL A PAGAR:", dinero(total)));
            f.append(linea);
            f.append("            ¡Gracias por su compra!\n");
            return f.toString();
        } catch (SQLException ex) {
            conectionDB.registrarError(ex);
            return "No se pudo generar la factura: " + conectionDB.ultimoError;
        }
    }
}
