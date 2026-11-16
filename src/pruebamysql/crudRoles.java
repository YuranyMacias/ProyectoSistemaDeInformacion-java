package pruebamysql;

import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla roles (1 = Administrador, 2 = Vendedor).
 * No tiene pantalla propia: los permisos del menú dependen de estos dos roles.
 */
public class crudRoles {

    public boolean insertarDato(String nombre_rol, String descripcion) {
        String query = "insert into roles(nombre_rol, descripcion) values(?,?)";
        return conectionDB.ejecutar(query, nombre_rol, conectionDB.nulo(descripcion));
    }

    public DefaultTableModel consultarDatos() {
        String[] titulos = {"ID", "Rol", "Descripción"};
        String query = "select id_roles, nombre_rol, descripcion from roles order by id_roles";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Lista de roles para el JComboBox del formulario de empleados
    public DefaultComboBoxModel<ItemCombo> modeloCombo() {
        String query = "select id_roles, nombre_rol from roles order by id_roles";
        return conectionDB.consultarCombo(query);
    }

    public boolean actualizarDatos(int id, String nombre_rol, String descripcion) {
        String query = "update roles set nombre_rol = ?, descripcion = ? where id_roles = ?";
        return conectionDB.ejecutar(query, nombre_rol, conectionDB.nulo(descripcion), id);
    }

    // Un rol con empleados asignados no se puede eliminar
    public String motivoBloqueo(int id) {
        int hijos = conectionDB.contar("select count(*) from empleados where id_roles = ?", id);
        if (hijos > 0) {
            return "El rol tiene " + hijos + " empleado(s) asociado(s).";
        }
        return null;
    }

    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        return conectionDB.ejecutar("delete from roles where id_roles = ?", id);
    }
}
