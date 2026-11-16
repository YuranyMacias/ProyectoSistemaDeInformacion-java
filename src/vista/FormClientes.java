package vista;

import pruebamysql.Sesion;
import pruebamysql.conectionDB;
import pruebamysql.crudClientes;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de Clientes (datos personales).
 * El Vendedor puede registrar y editar clientes; solo el Administrador puede eliminarlos.
 * No deja eliminar un cliente que tenga ventas registradas.
 */
public class FormClientes extends JFrame {

    private JTextField txtNombre, txtDocumento, txtTelefono, txtEmail, txtDireccion;
    private JComboBox<String> cmbTipoDoc;
    private JTable tabla;
    private final crudClientes crud = new crudClientes();

    public FormClientes() {
        setTitle("Gestión de Clientes - Estilo Prisma");
        setSize(1200, 700);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Clientes", "Datos personales de los clientes. Un cliente con ventas no se puede eliminar.");
        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        txtNombre = EstiloUI.campo();
        cmbTipoDoc = EstiloUI.combo();
        cmbTipoDoc.setModel(new DefaultComboBoxModel<>(new String[]{"CC", "CE", "NIT", "TI", "PAS"}));
        txtDocumento = EstiloUI.campo();
        txtTelefono = EstiloUI.campo();
        txtEmail = EstiloUI.campo();
        txtDireccion = EstiloUI.campo();

        campos.campo("Nombre completo", txtNombre);
        campos.dosColumnas("Tipo de documento", cmbTipoDoc, "Número de documento", txtDocumento);
        campos.campo("Teléfono", txtTelefono);
        campos.campo("Correo electrónico", txtEmail);
        campos.campo("Dirección", txtDireccion);
        JButton[] botones = EstiloUI.botonesCrud(campos); // [0] Guardar, [1] Editar, [2] Eliminar, [3] Limpiar
        campos.relleno();
        botones[0].addActionListener(e -> guardar());
        botones[1].addActionListener(e -> editar());
        botones[2].addActionListener(e -> eliminar());
        botones[3].addActionListener(e -> limpiar());

        // Permiso: el vendedor no puede eliminar clientes
        if (!Sesion.esAdmin()) {
            EstiloUI.bloquear(botones[2], "Solo el Administrador puede eliminar clientes");
        }

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
        centro.add(EstiloUI.tarjetaFormulario("Datos del cliente", campos, 330), BorderLayout.WEST);
        centro.add(EstiloUI.panelTabla(tabla, "Listado de clientes"), BorderLayout.CENTER);
    }

    // Revisa los campos obligatorios y el formato del correo
    private boolean validar() {
        if (txtNombre.getText().trim().isEmpty()) {
            EstiloUI.aviso(this, "El nombre del cliente es obligatorio.");
            return false;
        }
        if (txtDocumento.getText().trim().isEmpty()) {
            EstiloUI.aviso(this, "El número de documento es obligatorio.");
            return false;
        }
        if (!EstiloUI.emailValido(txtEmail.getText().trim())) {
            EstiloUI.aviso(this, "El correo electrónico no tiene un formato válido.");
            return false;
        }
        return true;
    }

    // GUARDAR: registra un cliente nuevo
    private void guardar() {
        if (!validar()) {
            return;
        }
        if (crud.insertarDato(txtNombre.getText().trim(), cmbTipoDoc.getSelectedItem().toString(), txtDocumento.getText(),
                txtTelefono.getText(), txtEmail.getText(), txtDireccion.getText())) {
            EstiloUI.exito(this, "¡Cliente guardado exitosamente!");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: actualiza el cliente seleccionado
    private void editar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un cliente de la tabla para editarlo.");
            return;
        }
        if (!validar()) {
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (crud.actualizarDatos(id, txtNombre.getText().trim(), cmbTipoDoc.getSelectedItem().toString(), txtDocumento.getText(),
                txtTelefono.getText(), txtEmail.getText(), txtDireccion.getText())) {
            EstiloUI.exito(this, "Cliente actualizado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra el cliente seleccionado (si no tiene ventas)
    private void eliminar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un cliente en la tabla.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (!EstiloUI.confirmar(this, "¿Eliminar al cliente '" + EstiloUI.datoFila(tabla, 1) + "'?")) {
            return;
        }
        if (crud.eliminarDatos(id)) {
            EstiloUI.exito(this, "Cliente eliminado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    private void limpiar() {
        txtNombre.setText("");
        cmbTipoDoc.setSelectedIndex(0);
        txtDocumento.setText("");
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
        cmbTipoDoc.setSelectedItem(EstiloUI.datoFila(tabla, 2));
        txtDocumento.setText(EstiloUI.datoFila(tabla, 3));
        txtTelefono.setText(EstiloUI.datoFila(tabla, 4));
        txtEmail.setText(EstiloUI.datoFila(tabla, 5));
        txtDireccion.setText(EstiloUI.datoFila(tabla, 6));
    }

    private void cargarTabla() {
        tabla.setModel(crud.consultarDatos());
    }
}
