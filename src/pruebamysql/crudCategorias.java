package pruebamysql;

import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla categorias.
 * Categoría = "padre" de productos.
 */
public class crudCategorias {

    // CREATE: guarda una categoría nueva. Devuelve true si se guardó.
    public boolean insertarDato(String nombre_categoria) {
        String query = "insert into categorias(nombre_categoria) values(?)";
        return conectionDB.ejecutar(query, nombre_categoria);
    }

    // READ: devuelve todas las categorías para mostrarlas en la tabla
    public DefaultTableModel consultarDatos() {
        String[] titulos = {"ID", "Nombre de la Categoría"};
        String query = "select id_categoria, nombre_categoria from categorias order by nombre_categoria";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Lista de categorías para los JComboBox (por ejemplo en productos)
    public DefaultComboBoxModel<ItemCombo> modeloCombo() {
        String query = "select id_categoria, nombre_categoria from categorias order by nombre_categoria";
        return conectionDB.consultarCombo(query);
    }

    // UPDATE: cambia el nombre de una categoría
    public boolean actualizarDatos(int id, String nombre_categoria) {
        String query = "update categorias set nombre_categoria = ? where id_categoria = ?";
        return conectionDB.ejecutar(query, nombre_categoria, id);
    }

    // Devuelve el motivo por el que NO se puede borrar, o null si se puede borrar.
    // Regla: una categoría con productos (hijos) no se puede eliminar.
    public String motivoBloqueo(int id) {
        int hijos = conectionDB.contar("select count(*) from productos where id_categoria = ?", id);
        if (hijos > 0) {
            return "La categoría tiene " + hijos + " producto(s) asociado(s).\n"
                    + "Primero elimina o cambia de categoría esos productos.";
        }
        return null;
    }

    // DELETE: elimina la categoría solo si no tiene productos
    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        return conectionDB.ejecutar("delete from categorias where id_categoria = ?", id);
    }
}
