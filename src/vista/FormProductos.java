package vista;

import pruebamysql.ItemCombo;
import pruebamysql.conectionDB;
import pruebamysql.crudCategorias;
import pruebamysql.crudProductos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pantalla de Productos (información general de la prenda).
 * No deja eliminar un producto que tenga variantes (talla/color) asociadas.
 */
public class FormProductos extends JFrame {

    private JTextField txtNombre, txtDescripcion, txtMarca;
    private JComboBox<String> cmbGenero;
    private JComboBox<ItemCombo> cmbCategoria;
    private JTable tabla;
    private final crudProductos crud = new crudProductos();
    private final crudCategorias crudCat = new crudCategorias();

    public FormProductos() {
        setTitle("Gestión de Productos - Estilo Prisma");
        setSize(1180, 680);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Productos", "Información general de cada prenda. Un producto con variantes (talla/color) no se puede eliminar.");
        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        txtNombre = EstiloUI.campo();
        txtDescripcion = EstiloUI.campo();
        txtMarca = EstiloUI.campo();
        cmbGenero = EstiloUI.combo();
        cmbGenero.setModel(new DefaultComboBoxModel<>(new String[]{"Femenino", "Masculino", "Unisex"}));
        cmbCategoria = EstiloUI.combo();
        cmbCategoria.setModel(crudCat.modeloCombo());

        campos.campo("Nombre del producto", txtNombre);
        campos.campo("Descripción", txtDescripcion);
        campos.dosColumnas("Marca", txtMarca, "Género", cmbGenero);
        campos.campo("Categoría", cmbCategoria);
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
        centro.add(EstiloUI.tarjetaFormulario("Datos del producto", campos, 330), BorderLayout.WEST);
        centro.add(EstiloUI.panelTabla(tabla, "Listado de productos"), BorderLayout.CENTER);

        // Si en otra ventana se crea una categoría nueva, aquí se actualiza la lista al volver
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                EstiloUI.recargarCombo(cmbCategoria, crudCat.modeloCombo());
            }
        });
    }

    // Revisa los campos obligatorios. Devuelve true si todo está bien.
    private boolean validar() {
        if (txtNombre.getText().trim().isEmpty()) {
            EstiloUI.aviso(this, "El nombre del producto es obligatorio.");
            return false;
        }
        if (cmbCategoria.getSelectedItem() == null) {
            EstiloUI.aviso(this, "Primero crea una categoría para poder asignarla al producto.");
            return false;
        }
        return true;
    }

    // GUARDAR: crea un producto nuevo
    private void guardar() {
        if (!validar()) {
            return;
        }
        int idCategoria = EstiloUI.idSeleccionado(cmbCategoria);
        if (crud.insertarDato(txtNombre.getText().trim(), txtDescripcion.getText(), txtMarca.getText(),
                cmbGenero.getSelectedItem().toString(), idCategoria)) {
            EstiloUI.exito(this, "¡Producto guardado en la Base de Datos!");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: actualiza el producto seleccionado
    private void editar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un producto de la tabla para editarlo.");
            return;
        }
        if (!validar()) {
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        int idCategoria = EstiloUI.idSeleccionado(cmbCategoria);
        if (crud.actualizarDatos(id, txtNombre.getText().trim(), txtDescripcion.getText(), txtMarca.getText(),
                cmbGenero.getSelectedItem().toString(), idCategoria)) {
            EstiloUI.exito(this, "Producto actualizado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra el producto seleccionado (si no tiene variantes)
    private void eliminar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona un producto en la tabla.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (!EstiloUI.confirmar(this, "¿Eliminar el producto '" + EstiloUI.datoFila(tabla, 1) + "'?")) {
            return;
        }
        if (crud.eliminarDatos(id)) {
            EstiloUI.exito(this, "Producto eliminado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    private void limpiar() {
        txtNombre.setText("");
        txtDescripcion.setText("");
        txtMarca.setText("");
        cmbGenero.setSelectedIndex(0);
        if (cmbCategoria.getItemCount() > 0) {
            cmbCategoria.setSelectedIndex(0);
        }
        tabla.clearSelection();
    }

    // Pasa los datos de la fila seleccionada a los campos
    private void llenarCampos() {
        if (tabla.getSelectedRow() < 0) {
            return;
        }
        txtNombre.setText(EstiloUI.datoFila(tabla, 1));
        txtDescripcion.setText(EstiloUI.datoFila(tabla, 2));
        txtMarca.setText(EstiloUI.datoFila(tabla, 3));
        cmbGenero.setSelectedItem(EstiloUI.datoFila(tabla, 4));
        // La columna 6 (oculta) guarda el id de la categoría
        EstiloUI.seleccionarPorId(cmbCategoria, Integer.parseInt(EstiloUI.datoFila(tabla, 6)));
    }

    private void cargarTabla() {
        tabla.setModel(crud.consultarDatos());
        EstiloUI.ocultarColumna(tabla, 6); // oculta id_categoria
    }
}
