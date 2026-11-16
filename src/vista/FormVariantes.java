package vista;

import pruebamysql.ItemCombo;
import pruebamysql.conectionDB;
import pruebamysql.crudProductos;
import pruebamysql.crudVariantesProducto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pantalla de Variantes de producto (talla, color, SKU y precio de venta).
 * Al guardar una variante nueva se crea automáticamente su inventario con 0 unidades.
 * No deja eliminar una variante con compras/ventas o con unidades en stock.
 */
public class FormVariantes extends JFrame {

    private JComboBox<ItemCombo> cmbProducto;
    private JTextField txtTalla, txtColor, txtSku, txtPrecio;
    private JTable tabla;
    private final crudVariantesProducto crud = new crudVariantesProducto();
    private final crudProductos crudProd = new crudProductos();

    public FormVariantes() {
        setTitle("Variantes de Producto (Talla / Color) - Estilo Prisma");
        setSize(1180, 680);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Tallas y colores", "Cada variante tiene su SKU, su precio y su propio inventario (se crea en 0 unidades).");
        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        cmbProducto = EstiloUI.combo();
        cmbProducto.setModel(crudProd.modeloCombo());
        txtTalla = EstiloUI.campo();
        txtColor = EstiloUI.campo();
        txtSku = EstiloUI.campo();
        txtPrecio = EstiloUI.campo();

        campos.campo("Producto", cmbProducto);
        campos.dosColumnas("Talla", txtTalla, "Color", txtColor);
        campos.campo("Código SKU (único)", txtSku);
        campos.campo("Precio de venta ($)", txtPrecio);
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
        centro.add(EstiloUI.tarjetaFormulario("Datos de la variante", campos, 330), BorderLayout.WEST);
        centro.add(EstiloUI.panelTabla(tabla, "Listado de variantes"), BorderLayout.CENTER);

        // Actualiza la lista de productos cuando se vuelve a esta ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                EstiloUI.recargarCombo(cmbProducto, crudProd.modeloCombo());
            }
        });
    }

    // Revisa los campos. Devuelve el precio (>= 0) o -1 si hay un error (ya mostró el mensaje).
    private double validar() {
        if (cmbProducto.getSelectedItem() == null) {
            EstiloUI.aviso(this, "Primero crea un producto para poder asignarle variantes.");
            return -1;
        }
        if (txtSku.getText().trim().isEmpty()) {
            EstiloUI.aviso(this, "El código SKU es obligatorio.");
            return -1;
        }
        double precio = EstiloUI.leerDecimal(txtPrecio.getText());
        if (precio < 0) {
            EstiloUI.aviso(this, "Escribe un precio de venta válido (solo números, por ejemplo 95000).");
            return -1;
        }
        return precio;
    }

    // GUARDAR: crea la variante (y su inventario en 0)
    private void guardar() {
        double precio = validar();
        if (precio < 0) {
            return;
        }
        if (crud.insertarDato(EstiloUI.idSeleccionado(cmbProducto), txtTalla.getText(), txtColor.getText(),
                txtSku.getText().trim(), precio)) {
            EstiloUI.exito(this, "Variante guardada. Su inventario inició en 0 unidades;\nregistra una compra para agregar stock.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: actualiza la variante seleccionada
    private void editar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona una variante de la tabla para editarla.");
            return;
        }
        double precio = validar();
        if (precio < 0) {
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (crud.actualizarDatos(id, EstiloUI.idSeleccionado(cmbProducto), txtTalla.getText(), txtColor.getText(),
                txtSku.getText().trim(), precio)) {
            EstiloUI.exito(this, "Variante actualizada.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra la variante (si no tiene movimientos ni stock)
    private void eliminar() {
        if (tabla.getSelectedRow() < 0) {
            EstiloUI.aviso(this, "Primero selecciona una variante en la tabla.");
            return;
        }
        int id = Integer.parseInt(EstiloUI.datoFila(tabla, 0));
        if (!EstiloUI.confirmar(this, "¿Eliminar la variante con SKU '" + EstiloUI.datoFila(tabla, 4) + "'?")) {
            return;
        }
        if (crud.eliminarDatos(id)) {
            EstiloUI.exito(this, "Variante eliminada.");
            limpiar();
            cargarTabla();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    private void limpiar() {
        txtTalla.setText("");
        txtColor.setText("");
        txtSku.setText("");
        txtPrecio.setText("");
        if (cmbProducto.getItemCount() > 0) {
            cmbProducto.setSelectedIndex(0);
        }
        tabla.clearSelection();
    }

    private void llenarCampos() {
        if (tabla.getSelectedRow() < 0) {
            return;
        }
        txtTalla.setText(EstiloUI.datoFila(tabla, 2));
        txtColor.setText(EstiloUI.datoFila(tabla, 3));
        txtSku.setText(EstiloUI.datoFila(tabla, 4));
        txtPrecio.setText(EstiloUI.datoFila(tabla, 5));
        // La columna 6 (oculta) guarda el id del producto
        EstiloUI.seleccionarPorId(cmbProducto, Integer.parseInt(EstiloUI.datoFila(tabla, 6)));
    }

    private void cargarTabla() {
        tabla.setModel(crud.consultarDatos());
        EstiloUI.ocultarColumna(tabla, 6); // oculta id_producto
    }
}
