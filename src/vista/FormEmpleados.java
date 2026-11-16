package vista;

import pruebamysql.ItemCombo;
import pruebamysql.conectionDB;
import pruebamysql.crudEmpleados;
import pruebamysql.crudRoles;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de Personal (empleados que usan el sistema). Solo el Administrador entra aquí.
 * La contraseña se guarda cifrada. Al editar, si el campo contraseña queda vacío no se cambia.
 */
public class FormEmpleados extends JFrame {

    private JTextField txtNombre, txtTelefono, txtEmail, txtUsuario;
    private JPasswordField txtContrasena;
    private JComboBox<ItemCombo> cmbRol;
    private JTable tabla;
    private final crudEmpleados crud = new crudEmpleados();
    private final crudRoles crudRol = new crudRoles();

    public FormEmpleados() {
        setTitle("Gestión de Personal - Estilo Prisma");
        setSize(1200, 700);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Personal", "Empleados que usan el sistema. Solo el Administrador puede gestionarlos.");
        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        txtNombre = EstiloUI.campo();
        txtTelefono = EstiloUI.campo();
        txtEmail = EstiloUI.campo();
        txtUsuario = EstiloUI.campo();
        cmbRol = EstiloUI.combo();
        cmbRol.setModel(crudRol.modeloCombo());
        txtContrasena = EstiloUI.campoClave();

        campos.campo("Nombre completo", txtNombre);
        campos.dosColumnas("Teléfono", txtTelefono, "Rol", cmbRol);
        campos.campo("Correo electrónico", txtEmail);
        campos.campo("Usuario", txtUsuario);
        campos.campo("Contraseña (vacía = no cambiarla)", txtContrasena);
        JButton[] botones = EstiloUI.botonesCrud(campos); // [0] Guardar, [1] Editar, [2] Eliminar, [3] Limpiar
        campos.relleno();
        botones[0].addActionListener(e -> guardar());
        botones[1].addActionListener(e -> editar());
        botones[2].addActionListener(e -> eliminar());
        botones[3].addActionListener(e -> limpiar());

        // --- TABLA ---
        tabla = new JTable();
        EstiloUI.estilizarTabla(tabla);
        cargarTabla();
        // Al hacer clic en una fila, sus datos pasan a los campos para poder editarlos
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                llenarCampos();
            }
        });

        // --- DISEÑO: formulario a la izquierda, tabla que se estira a la derecha ---
        centro.add(EstiloUI.tarjetaFormulario("Datos del empleado", campos, 330), BorderLayout.WEST);
        centro.add(EstiloUI.panelTabla(tabla, "Listado de empleados"), BorderLayout.CENTER);
    }

    private boolean validar() {
        if (txtNombre.getText().trim().isEmpty() || txtUsuario.getText().trim().isEmpty()) {
            EstiloUI.aviso(this, "El nombre y el usuario son obligatorios.");
            return false;
        }
        if (cmbRol.getSelectedItem() == null) {
            EstiloUI.aviso(this, "Selecciona un rol.");
            return false;
        }
        if (!EstiloUI.emailValido(txtEmail.getText().trim())) {
            EstiloUI.aviso(this, "El correo electrónico no tiene un formato válido.");
            return false;
        }
        return true;
    }

    // GUARDAR: crea un empleado nuevo (la contraseña es obligatoria)
    private void guardar() {
        if (!validar()) {
            return;
        }
        String clave = new String(txtContrasena.getPassword());
        if (clave.length() < 4) {
            EstiloUI.aviso(this, "La contraseña debe tener mínimo 4 caracteres.");
            return;
        }
        if (crud.insertarDato(txtNombre.getText().trim(), txtTelefono.getText(), txtEmail.getText(),
                txtUsuario.getText().trim(), clave, EstiloUI.idSeleccionado(cmbRol))) {
            EstiloUI.exito(this, "¡Empleado guardado exitosamente!");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: actualiza el empleado seleccionado
    private void editar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un empleado de la tabla para editarlo.");
            return;
        }
        if (!validar()) {
            return;
        }
        String clave = new String(txtContrasena.getPassword());
        if (!clave.isEmpty() && clave.length() < 4) {
            EstiloUI.aviso(this, "La contraseña debe tener mínimo 4 caracteres.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (crud.actualizarDatos(id, txtNombre.getText().trim(), txtTelefono.getText(), txtEmail.getText(),
                txtUsuario.getText().trim(), clave, EstiloUI.idSeleccionado(cmbRol))) {
            EstiloUI.exito(this, "Empleado actualizado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra el empleado seleccionado (si no tiene compras/ventas y no es el usuario actual)
    private void eliminar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un empleado en la tabla.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (!EstiloUI.confirmar(this, "¿Eliminar al empleado '" + EstiloUI.datoFila(tabla, 1) + "'?")) {
            return;
        }
        if (crud.eliminarDatos(id)) {
            EstiloUI.exito(this, "Empleado eliminado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    private void limpiar() {
        txtNombre.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        txtUsuario.setText("");
        txtContrasena.setText("");
        if (cmbRol.getItemCount() > 0) {
            cmbRol.setSelectedIndex(0);
        }
        tabla.clearSelection();
    }

    private void llenarCampos() {
        if (tabla.getSelectedRow() < 0) {
            return;
        }
        txtNombre.setText(EstiloUI.datoFila(tabla, 1));
        txtTelefono.setText(EstiloUI.datoFila(tabla, 2));
        txtEmail.setText(EstiloUI.datoFila(tabla, 3));
        txtUsuario.setText(EstiloUI.datoFila(tabla, 4));
        txtContrasena.setText(""); // por seguridad nunca se muestra la contraseña
        // La columna 6 (oculta) guarda el id del rol
        EstiloUI.seleccionarPorId(cmbRol, Integer.parseInt(EstiloUI.datoFila(tabla, 6)));
    }

    private void cargarTabla() {
        tabla.setModel(crud.consultarDatos());
        EstiloUI.ocultarColumna(tabla, 6); // oculta id_roles
    }
}
