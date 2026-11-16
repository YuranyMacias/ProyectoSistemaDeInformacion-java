package pruebamysql;

import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla productos (información general de la prenda).
 * Producto = "hijo" de categoría y "padre" de variantes.
 */
public class crudProductos {

    public boolean insertarDato(String nombre_producto, String descripcion, String marca, String genero, int id_categoria) {
        String query = "insert into productos(nombre_producto, descripcion, marca, genero, id_categoria) values(?,?,?,?,?)";
        return conectionDB.ejecutar(query, nombre_producto, conectionDB.nulo(descripcion), conectionDB.nulo(marca), genero, id_categoria);
    }

    // La última columna (id_categoria) se oculta en el formulario; sirve para seleccionar el combo
    public DefaultTableModel consultarDatos() {
        String[] titulos = {"ID", "Producto", "Descripción", "Marca", "Género", "Categoría", "id_categoria"};
        String query = "select p.id_producto, p.nombre_producto, p.descripcion, p.marca, p.genero, c.nombre_categoria, p.id_categoria "
                + "from productos p join categorias c on c.id_categoria = p.id_categoria order by p.nombre_producto";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Lista de productos para el JComboBox de variantes
    public DefaultComboBoxModel<ItemCombo> modeloCombo() {
        String query = "select id_producto, nombre_producto from productos order by nombre_producto";
        return conectionDB.consultarCombo(query);
    }

    public boolean actualizarDatos(int id, String nombre_producto, String descripcion, String marca, String genero, int id_categoria) {
        String query = "update productos set nombre_producto=?, descripcion=?, marca=?, genero=?, id_categoria=? where id_producto=?";
        return conectionDB.ejecutar(query, nombre_producto, conectionDB.nulo(descripcion), conectionDB.nulo(marca), genero, id_categoria, id);
    }

    // Regla: un producto con variantes (hijos) no se puede eliminar
    public String motivoBloqueo(int id) {
        int hijos = conectionDB.contar("select count(*) from variantes_producto where id_producto = ?", id);
        if (hijos > 0) {
            return "El producto tiene " + hijos + " variante(s) (talla/color) asociada(s).\n"
                    + "Primero elimina esas variantes.";
        }
        return null;
    }

    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        return conectionDB.ejecutar("delete from productos where id_producto = ?", id);
    }
}
