package vista;

import pruebamysql.conectionDB;
import pruebamysql.crudCategorias;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de Categorías: Guardar, Editar, Eliminar y Limpiar.
 * No deja eliminar una categoría que tenga productos asociados.
 */
public class FormCategorias extends JFrame {

    // Variables globales para capturar los datos
    private JTextField txtNombre;
    private JTable tabla;
    private final crudCategorias crud = new crudCategorias();

    public FormCategorias() {
        setTitle("Gestión de Categorías - Estilo Prisma");
        setSize(980, 540);
        setMinimumSize(new Dimension(780, 460));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Categorías", "Categorías principales de las prendas. Una categoría con productos no se puede eliminar.");
        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        txtNombre = EstiloUI.campo();
        campos.campo("Nombre de la categoría", txtNombre);
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
        centro.add(EstiloUI.tarjetaFormulario("Datos de la categoría", campos, 330), BorderLayout.WEST);
        centro.add(EstiloUI.panelTabla(tabla, "Listado de categorías"), BorderLayout.CENTER);
    }

    // GUARDAR: crea una categoría nueva
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            EstiloUI.aviso(this, "Por favor escribe un nombre para la categoría.");
            return;
        }
        if (crud.insertarDato(nombre)) {
            EstiloUI.exito(this, "¡Categoría '" + nombre + "' guardada exitosamente!");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: cambia el nombre de la categoría seleccionada
    private void editar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona una categoría de la tabla para editarla.");
            return;
        }
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            EstiloUI.aviso(this, "El nombre no puede estar vacío.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (crud.actualizarDatos(id, nombre)) {
            EstiloUI.exito(this, "Categoría actualizada.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra la categoría seleccionada (si no tiene productos)
    private void eliminar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona una categoría de la tabla para eliminarla.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        String nombre = EstiloUI.datoFila(tabla, 1);
        if (!EstiloUI.confirmar(this, "¿Eliminar la categoría '" + nombre + "'?")) {
            return;
        }
        if (crud.eliminarDatos(id)) {
            EstiloUI.exito(this, "Categoría eliminada.");
            limpiar();
            cargarTabla();
        } else {
            // La categoría tiene productos (hijos): se muestra la advertencia
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    // LIMPIAR: vacía los campos y quita la selección
    private void limpiar() {
        txtNombre.setText("");
        tabla.clearSelection();
    }

    // Pasa los datos de la fila seleccionada a los campos
    private void llenarCampos() {
        if (tabla.getSelectedRow() >= 0) {
            txtNombre.setText(EstiloUI.datoFila(tabla, 1));
        }
    }

    // Refresca la tabla desde MySQL
    private void cargarTabla() {
        tabla.setModel(crud.consultarDatos());
    }
}
