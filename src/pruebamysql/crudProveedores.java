package pruebamysql;

import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla proveedores.
 * Un proveedor con compras registradas no se puede eliminar.
 */
public class crudProveedores {

    public boolean insertarDato(String nombre, String nit, String telefono, String email, String direccion) {
        String query = "insert into proveedores(nombre, nit, telefono, email, direccion) values(?,?,?,?,?)";
        return conectionDB.ejecutar(query, nombre, nit, conectionDB.nulo(telefono),
                conectionDB.nulo(email), conectionDB.nulo(direccion));
    }

    public DefaultTableModel consultarDatos() {
        String[] titulos = {"ID", "Nombre", "NIT", "Teléfono", "Email", "Dirección"};
        String query = "select id_proveedor, nombre, nit, telefono, email, direccion from proveedores order by nombre";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Lista de proveedores para el JComboBox de compras: "nombre (NIT)"
    public DefaultComboBoxModel<ItemCombo> modeloCombo() {
        String query = "select id_proveedor, concat(nombre, ' (', nit, ')') from proveedores order by nombre";
        return conectionDB.consultarCombo(query);
    }

    public boolean actualizarDatos(int id, String nombre, String nit, String telefono, String email, String direccion) {
        String query = "update proveedores set nombre=?, nit=?, telefono=?, email=?, direccion=? where id_proveedor=?";
        return conectionDB.ejecutar(query, nombre, nit, conectionDB.nulo(telefono),
                conectionDB.nulo(email), conectionDB.nulo(direccion), id);
    }

    // Regla: un proveedor con compras (hijos) no se puede eliminar
    public String motivoBloqueo(int id) {
        int hijos = conectionDB.contar("select count(*) from compras where id_proveedor = ?", id);
        if (hijos > 0) {
            return "El proveedor tiene " + hijos + " compra(s) registrada(s).";
        }
        return null;
    }

    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        return conectionDB.ejecutar("delete from proveedores where id_proveedor = ?", id);
    }
}
