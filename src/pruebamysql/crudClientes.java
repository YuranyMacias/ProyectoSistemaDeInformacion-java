package pruebamysql;

import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla clientes.
 * Un cliente con ventas registradas no se puede eliminar.
 */
public class crudClientes {

    public boolean insertarDato(String nombre, String tipo_documento, String numero_documento, String telefono, String email, String direccion) {
        String query = "insert into clientes(nombre, tipo_documento, numero_documento, telefono, email, direccion) values(?,?,?,?,?,?)";
        return conectionDB.ejecutar(query, nombre, tipo_documento, conectionDB.nulo(numero_documento),
                conectionDB.nulo(telefono), conectionDB.nulo(email), conectionDB.nulo(direccion));
    }

    public DefaultTableModel consultarDatos() {
        String[] titulos = {"ID", "Nombre", "Tipo Doc.", "Documento", "Teléfono", "Email", "Dirección"};
        String query = "select id_cliente, nombre, tipo_documento, numero_documento, telefono, email, direccion "
                + "from clientes order by nombre";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Lista de clientes para el JComboBox de ventas: "documento - nombre"
    public DefaultComboBoxModel<ItemCombo> modeloCombo() {
        String query = "select id_cliente, concat(ifnull(numero_documento,'sin doc.'), ' - ', nombre) "
                + "from clientes order by nombre";
        return conectionDB.consultarCombo(query);
    }

    public boolean actualizarDatos(int id, String nombre, String tipo_documento, String numero_documento, String telefono, String email, String direccion) {
        String query = "update clientes set nombre=?, tipo_documento=?, numero_documento=?, telefono=?, email=?, direccion=? where id_cliente=?";
        return conectionDB.ejecutar(query, nombre, tipo_documento, conectionDB.nulo(numero_documento),
                conectionDB.nulo(telefono), conectionDB.nulo(email), conectionDB.nulo(direccion), id);
    }

    // Regla: un cliente con ventas (hijos) no se puede eliminar
    public String motivoBloqueo(int id) {
        int hijos = conectionDB.contar("select count(*) from ventas where id_cliente = ?", id);
        if (hijos > 0) {
            return "El cliente tiene " + hijos + " venta(s) registrada(s).";
        }
        return null;
    }

    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        return conectionDB.ejecutar("delete from clientes where id_cliente = ?", id);
    }
}
