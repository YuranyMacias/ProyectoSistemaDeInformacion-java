package pruebamysql;

import javax.swing.table.DefaultTableModel;

/**
 * CRUD de la tabla empleados (usuarios del sistema).
 * La contraseña se guarda cifrada (SHA-256), nunca en texto plano.
 */
public class crudEmpleados {

    public boolean insertarDato(String nombre, String telefono, String email, String usuario, String contrasena, int id_roles) {
        String query = "insert into empleados(nombre, telefono, email, usuario, contrasena, id_roles) values(?,?,?,?,?,?)";
        return conectionDB.ejecutar(query, nombre, conectionDB.nulo(telefono), conectionDB.nulo(email),
                usuario, Seguridad.sha256(contrasena), id_roles);
    }

    // La contraseña NO se muestra en la tabla. La última columna (id_roles) se oculta en el formulario.
    public DefaultTableModel consultarDatos() {
        String[] titulos = {"ID", "Nombre", "Teléfono", "Email", "Usuario", "Rol", "id_roles"};
        String query = "select e.id_empleado, e.nombre, e.telefono, e.email, e.usuario, r.nombre_rol, e.id_roles "
                + "from empleados e join roles r on r.id_roles = e.id_roles order by e.nombre";
        return conectionDB.consultarModelo(query, titulos);
    }

    // Si la contraseña llega vacía se deja la que ya tenía; si trae texto se cifra y se cambia
    public boolean actualizarDatos(int id, String nombre, String telefono, String email, String usuario, String contrasena, int id_roles) {
        if (contrasena == null || contrasena.isEmpty()) {
            String query = "update empleados set nombre=?, telefono=?, email=?, usuario=?, id_roles=? where id_empleado=?";
            return conectionDB.ejecutar(query, nombre, conectionDB.nulo(telefono), conectionDB.nulo(email), usuario, id_roles, id);
        }
        String query = "update empleados set nombre=?, telefono=?, email=?, usuario=?, contrasena=?, id_roles=? where id_empleado=?";
        return conectionDB.ejecutar(query, nombre, conectionDB.nulo(telefono), conectionDB.nulo(email),
                usuario, Seguridad.sha256(contrasena), id_roles, id);
    }

    // Regla: un empleado que registró compras o ventas (hijos) no se puede eliminar,
    // y nadie puede eliminarse a sí mismo mientras tiene la sesión abierta.
    public String motivoBloqueo(int id) {
        if (id == Sesion.getIdEmpleado()) {
            return "No puedes eliminar el usuario con el que iniciaste sesión.";
        }
        int compras = conectionDB.contar("select count(*) from compras where id_empleado = ?", id);
        int ventas = conectionDB.contar("select count(*) from ventas where id_empleado = ?", id);
        if (compras > 0 || ventas > 0) {
            return "El empleado tiene " + compras + " compra(s) y " + ventas + " venta(s) registrada(s).";
        }
        return null;
    }

    public boolean eliminarDatos(int id) {
        String motivo = motivoBloqueo(id);
        if (motivo != null) {
            conectionDB.ultimoError = motivo;
            return false;
        }
        return conectionDB.ejecutar("delete from empleados where id_empleado = ?", id);
    }
}
