package vista;

import pruebamysql.conectionDB;
import pruebamysql.crudProveedores;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de Proveedores (información legal y de contacto).
 * No deja eliminar un proveedor que tenga compras registradas.
 */
public class FormProveedores extends JFrame {

    private JTextField txtNombre, txtNit, txtTelefono, txtEmail, txtDireccion;
    private JTable tabla;
    private final crudProveedores crud = new crudProveedores();

    public FormProveedores() {
        setTitle("Gestión de Proveedores - Estilo Prisma");
        setSize(1200, 700);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Proveedores", "Información legal y de contacto. Un proveedor con compras no se puede eliminar.");
        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        txtNombre = EstiloUI.campo();
        txtNit = EstiloUI.campo();
        txtTelefono = EstiloUI.campo();
        txtEmail = EstiloUI.campo();
        txtDireccion = EstiloUI.campo();

        campos.campo("Razón social / Nombre", txtNombre);
        campos.campo("NIT", txtNit);
        campos.campo("Teléfono", txtTelefono);
        campos.campo("Correo electrónico", txtEmail);
        campos.campo("Dirección", txtDireccion);
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
        centro.add(EstiloUI.tarjetaFormulario("Datos del proveedor", campos, 330), BorderLayout.WEST);
        centro.add(EstiloUI.panelTabla(tabla, "Listado de proveedores"), BorderLayout.CENTER);
    }

    private boolean validar() {
        if (txtNombre.getText().trim().isEmpty() || txtNit.getText().trim().isEmpty()) {
            EstiloUI.aviso(this, "El nombre y el NIT son obligatorios.");
            return false;
        }
        if (!EstiloUI.emailValido(txtEmail.getText().trim())) {
            EstiloUI.aviso(this, "El correo electrónico no tiene un formato válido.");
            return false;
        }
        return true;
    }

    // GUARDAR: registra un proveedor nuevo
    private void guardar() {
        if (!validar()) {
            return;
        }
        if (crud.insertarDato(txtNombre.getText().trim(), txtNit.getText().trim(), txtTelefono.getText(),
                txtEmail.getText(), txtDireccion.getText())) {
            EstiloUI.exito(this, "¡Proveedor guardado exitosamente!");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: actualiza el proveedor seleccionado
    private void editar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un proveedor de la tabla para editarlo.");
            return;
        }
        if (!validar()) {
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (crud.actualizarDatos(id, txtNombre.getText().trim(), txtNit.getText().trim(), txtTelefono.getText(),
                txtEmail.getText(), txtDireccion.getText())) {
            EstiloUI.exito(this, "Proveedor actualizado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra el proveedor seleccionado (si no tiene compras)
    private void eliminar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un proveedor en la tabla.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (!EstiloUI.confirmar(this, "¿Eliminar al proveedor '" + EstiloUI.datoFila(tabla, 1) + "'?")) {
            return;
        }
        if (crud.eliminarDatos(id)) {
            EstiloUI.exito(this, "Proveedor eliminado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    private void limpiar() {
        txtNombre.setText("");
        txtNit.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        txtDireccion.setText("");
        tabla.clearSelection();
    }

    private void llenarCampos() {
        if (tabla.getSelectedRow() < 0) {
            return;
        }
        txtNombre.setText(EstiloUI.datoFila(tabla, 1));
        txtNit.setText(EstiloUI.datoFila(tabla, 2));
        txtTelefono.setText(EstiloUI.datoFila(tabla, 3));
        txtEmail.setText(EstiloUI.datoFila(tabla, 4));
        txtDireccion.setText(EstiloUI.datoFila(tabla, 5));
    }

    private void cargarTabla() {
        tabla.setModel(crud.consultarDatos());
    }
}
