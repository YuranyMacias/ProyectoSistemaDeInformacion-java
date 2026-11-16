package pruebamysql;

/**
 * Guarda quién inició sesión. Así cualquier formulario sabe
 * el nombre, el id y el rol del empleado actual.
 */
public class Sesion {

    public static final int ROL_ADMINISTRADOR = 1;
    public static final int ROL_VENDEDOR = 2;

    private static int idEmpleado = 0;
    private static String nombre = "";
    private static int idRol = 0;

    public static void iniciar(int id, String nombreEmpleado, int rol) {
        idEmpleado = id;
        nombre = nombreEmpleado;
        idRol = rol;
    }

    public static void cerrar() {
        idEmpleado = 0;
        nombre = "";
        idRol = 0;
    }

    public static int getIdEmpleado() { return idEmpleado; }
    public static String getNombre() { return nombre; }
    public static int getIdRol() { return idRol; }

    // true si el usuario actual es Administrador (acceso total)
    public static boolean esAdmin() {
        return idRol == ROL_ADMINISTRADOR;
    }
}
