package vista;

import pruebamysql.ItemCombo;
import pruebamysql.Sesion;
import pruebamysql.conectionDB;
import pruebamysql.crudInventario;
import pruebamysql.crudVariantesProducto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pantalla de Inventario: cantidad exacta de unidades de cada variante.
 *  - Todos los usuarios pueden CONSULTAR y buscar.
 *  - Solo el Administrador puede crear, ajustar o eliminar filas de inventario.
 * Las cantidades normalmente cambian solas: las compras suman y las ventas restan.
 */
public class FormInventario extends JFrame {

    private JTextField txtCantidad;
    private JComboBox<ItemCombo> cmbVariante;
    private JTable tabla;
    private final crudInventario crud = new crudInventario();
    private final crudVariantesProducto crudVar = new crudVariantesProducto();

    public FormInventario() {
        setTitle("Inventario - Estilo Prisma");
        setSize(1250, 640);
        setMinimumSize(new Dimension(940, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Inventario", "Unidades exactas de cada prenda. Las compras suman y las ventas restan automáticamente.");
        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        cmbVariante = EstiloUI.combo();
        cmbVariante.setModel(crudVar.modeloComboSinInventario());
        txtCantidad = EstiloUI.campo();

        campos.campo("Variante", cmbVariante);
        campos.campo("Cantidad actual (unidades)", txtCantidad);
        JButton[] botones = EstiloUI.botonesCrud(campos); // [0] Guardar, [1] Editar, [2] Eliminar, [3] Limpiar
        campos.relleno();
        botones[0].addActionListener(e -> guardar());
        botones[1].addActionListener(e -> editar());
        botones[2].addActionListener(e -> eliminar());
        botones[3].addActionListener(e -> limpiar());

        // Permiso: el vendedor solo consulta
        if (!Sesion.esAdmin()) {
            String motivo = "Solo el Administrador puede modificar el inventario";
            EstiloUI.bloquear(botones[0], motivo);
            EstiloUI.bloquear(botones[1], motivo);
            EstiloUI.bloquear(botones[2], motivo);
            cmbVariante.setEnabled(false);
            txtCantidad.setEditable(false);
            setTitle("Consulta de Inventario - Urban Store");
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
        centro.add(EstiloUI.tarjetaFormulario("Ajuste de inventario", campos, 330), BorderLayout.WEST);
        centro.add(EstiloUI.panelTabla(tabla, "Existencias"), BorderLayout.CENTER);

        // Al volver a esta ventana (después de una compra o venta) se refresca el stock
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                if (tabla.getSelectedRow() < 0) {
                    cargarTabla();
                }
            }
        });
    }

    // GUARDAR: crea el inventario de una variante que todavía no lo tiene
    private void guardar() {
        if (cmbVariante.getSelectedItem() == null || tabla.getSelectedRow() >= 0) {
            EstiloUI.aviso(this, "Pulsa 'Limpiar' y elige una variante que aún no tenga inventario.\n"
                    + "(Si no hay ninguna en la lista, todas las variantes ya tienen inventario.)");
            return;
        }
        int cantidad = EstiloUI.leerEntero(txtCantidad.getText());
        if (cantidad < 0) {
            EstiloUI.aviso(this, "Escribe una cantidad válida (número entero mayor o igual a 0).");
            return;
        }
        if (crud.insertarDato(EstiloUI.idSeleccionado(cmbVariante), cantidad)) {
            EstiloUI.exito(this, "Inventario creado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: ajusta la cantidad de la fila seleccionada (por ejemplo tras un conteo físico)
    private void editar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona una fila de la tabla para ajustar su cantidad.");
            return;
        }
        int cantidad = EstiloUI.leerEntero(txtCantidad.getText());
        if (cantidad < 0) {
            EstiloUI.aviso(this, "Escribe una cantidad válida (número entero mayor o igual a 0).");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        int idVariante = Integer.parseInt(EstiloUI.datoFila(tabla, 7)); // columna oculta
        if (crud.actualizarDatos(id, idVariante, cantidad)) {
            EstiloUI.exito(this, "Cantidad actualizada.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra la fila (solo si está en 0 y sin compras/ventas)
    private void eliminar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona una fila de la tabla.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (!EstiloUI.confirmar(this, "¿Eliminar el inventario de '" + EstiloUI.datoFila(tabla, 1) + "'?")) {
            return;
        }
        if (crud.eliminarDatos(id)) {
            EstiloUI.exito(this, "Inventario eliminado.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    // LIMPIAR: vuelve al modo "crear" (lista de variantes sin inventario)
    private void limpiar() {
        txtCantidad.setText("");
        tabla.clearSelection();
        cmbVariante.setModel(crudVar.modeloComboSinInventario());
        cmbVariante.setEnabled(Sesion.esAdmin());
    }

    // Al seleccionar una fila, el combo muestra su variante (bloqueado) y el campo su cantidad
    private void llenarCampos() {
        if (tabla.getSelectedRow() < 0) {
            return;
        }
        int idVariante = Integer.parseInt(EstiloUI.datoFila(tabla, 7));
        String texto = EstiloUI.datoFila(tabla, 1) + " - " + EstiloUI.datoFila(tabla, 2);
        cmbVariante.setModel(new DefaultComboBoxModel<>(new ItemCombo[]{new ItemCombo(idVariante, texto)}));
        cmbVariante.setEnabled(false);
        txtCantidad.setText(EstiloUI.datoFila(tabla, 5));
    }

    private void cargarTabla() {
        tabla.setModel(crud.consultarDatos());
        EstiloUI.ocultarColumna(tabla, 7); // oculta id_variante
    }
}
