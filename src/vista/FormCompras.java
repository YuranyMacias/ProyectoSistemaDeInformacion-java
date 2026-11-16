package vista;

import pruebamysql.ItemCombo;
import pruebamysql.LineaDetalle;
import pruebamysql.Sesion;
import pruebamysql.conectionDB;
import pruebamysql.crudCompras;
import pruebamysql.crudDetalleCompras;
import pruebamysql.crudInventario;
import pruebamysql.crudProveedores;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Compras a proveedores (cabecera + ítems).
 *
 * Flujo para una compra nueva:
 *   1. Elegir proveedor, N° de factura y estado.
 *   2. Agregar los ítems (prenda, cantidad y costo) con "Agregar ítem".
 *   3. Pulsar "Guardar". Si el estado es "Recibido", las cantidades se SUMAN al inventario.
 *
 * Al hacer clic en una compra del historial se muestran sus datos y sus ítems.
 * Editar y Eliminar solo los puede usar el Administrador.
 */
public class FormCompras extends JFrame {

    private JComboBox<ItemCombo> cmbProveedor, cmbItem;
    private JComboBox<String> cmbEstado;
    private JTextField txtFactura, txtCosto;
    private JSpinner spnCantidad;
    private JButton btnAgregar, btnQuitar;
    private JLabel lblModo, lblTotal;
    private JTable tablaItems, tablaHistorial;

    // Ítems que se están mostrando / armando (el "carrito" de la compra)
    private List<LineaDetalle> lineas = new ArrayList<>();
    // 0 = compra nueva; mayor que 0 = id de la compra del historial que está seleccionada
    private int idSeleccionado = 0;

    private final crudCompras crud = new crudCompras();
    private final crudDetalleCompras crudDetalle = new crudDetalleCompras();
    private final crudProveedores crudProv = new crudProveedores();
    private final crudInventario crudInv = new crudInventario();

    public FormCompras() {
        setTitle("Compras a Proveedores - Estilo Prisma");
        setSize(1340, 800);
        setMinimumSize(new Dimension(1080, 700));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel centro = EstiloUI.pagina(this, "Compras",
                "Mercancía que llega de los proveedores. Solo el estado 'Recibido' suma unidades al inventario.");

        // =================== BARRA SUPERIOR (datos de la compra) ===================
        cmbProveedor = EstiloUI.combo();
        cmbProveedor.setModel(crudProv.modeloCombo());
        txtFactura = EstiloUI.campo();
        cmbEstado = EstiloUI.combo();
        cmbEstado.setModel(new DefaultComboBoxModel<>(new String[]{"Recibido", "Pendiente", "Cancelado"}));
        cmbEstado.setPreferredSize(new Dimension(150, 38));
        JComponent barraDatos = EstiloUI.barraCampos(
                new String[]{"Proveedor", "N° de factura del proveedor", "Estado"},
                new JComponent[]{cmbProveedor, txtFactura, cmbEstado},
                new double[]{2, 1, 0});

        // =================== PANEL IZQUIERDO (agregar ítem + acciones) ===================
        cmbItem = EstiloUI.combo();
        cmbItem.setModel(crudInv.modeloCombo());
        spnCantidad = EstiloUI.spinner(1, 1, 100000);
        txtCosto = EstiloUI.campo();
        btnAgregar = EstiloUI.boton("Agregar ítem", EstiloUI.COLOR_VERDE);
        btnQuitar = EstiloUI.boton("Quitar ítem", EstiloUI.COLOR_GRIS);
        btnAgregar.addActionListener(e -> agregarItem());
        btnQuitar.addActionListener(e -> quitarItem());

        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        campos.campo("Prenda (SKU - producto)", cmbItem);
        campos.dosColumnas("Cantidad", spnCantidad, "Costo unitario ($)", txtCosto);
        campos.botones(btnAgregar, btnQuitar);
        campos.seccion("Acciones");
        // Botones: [0] Guardar, [1] Editar, [2] Eliminar, [3] Limpiar
        JButton[] botones = EstiloUI.botonesCrud(campos);
        campos.relleno();
        botones[0].addActionListener(e -> guardar());
        botones[1].addActionListener(e -> editar());
        botones[2].addActionListener(e -> eliminar());
        botones[3].addActionListener(e -> limpiar());

        // Permiso: el vendedor solo registra compras nuevas
        if (!Sesion.esAdmin()) {
            EstiloUI.bloquear(botones[1], "Solo el Administrador puede editar compras");
            EstiloUI.bloquear(botones[2], "Solo el Administrador puede eliminar compras");
        }

        // =================== ZONA DERECHA (ítems + historial) ===================
        // Tarjeta de ítems
        EstiloUI.Tarjeta tarjetaItems = EstiloUI.tarjeta(new BorderLayout(0, 12), 20);
        lblModo = EstiloUI.titulo("Ítems de la compra nueva");
        tarjetaItems.add(lblModo, BorderLayout.NORTH);
        tablaItems = new JTable();
        EstiloUI.estilizarTabla(tablaItems);
        tarjetaItems.add(EstiloUI.conScroll(tablaItems), BorderLayout.CENTER);
        lblTotal = new JLabel("TOTAL: $ 0", SwingConstants.RIGHT);
        lblTotal.setForeground(EstiloUI.COLOR_ACENTO);
        lblTotal.setFont(EstiloUI.fuente(Font.BOLD, 22));
        tarjetaItems.add(lblTotal, BorderLayout.SOUTH);

        // Tarjeta del historial
        tablaHistorial = new JTable();
        EstiloUI.estilizarTabla(tablaHistorial);
        refrescarItems();
        cargarHistorial();
        tablaHistorial.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarCompra();
            }
        });
        JComponent tarjetaHistorial = EstiloUI.panelTabla(tablaHistorial, "Historial de compras (clic para ver el detalle)");

        JPanel derecha = new JPanel(new BorderLayout(0, 16));
        derecha.setOpaque(false);
        derecha.add(barraDatos, BorderLayout.NORTH);
        derecha.add(EstiloUI.divisor(tarjetaItems, tarjetaHistorial), BorderLayout.CENTER);

        centro.add(EstiloUI.tarjetaFormulario("Agregar ítem", campos, 330), BorderLayout.WEST);
        centro.add(derecha, BorderLayout.CENTER);

        // Al volver a esta ventana se actualizan las listas (por si se creó un proveedor o variante nueva)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                EstiloUI.recargarCombo(cmbProveedor, crudProv.modeloCombo());
                EstiloUI.recargarCombo(cmbItem, crudInv.modeloCombo());
            }
        });
    }

    // =====================================================================
    // ÍTEMS (carrito)
    // =====================================================================

    // AGREGAR ÍTEM: pasa la prenda, cantidad y costo a la lista de ítems de la compra
    private void agregarItem() {
        if (idSeleccionado != 0) {
            EstiloUI.aviso(this, "Estás viendo una compra ya guardada. Pulsa 'Limpiar' para empezar una nueva.");
            return;
        }
        if (cmbItem.getSelectedItem() == null) {
            EstiloUI.aviso(this, "No hay prendas en el inventario. Crea primero productos y variantes.");
            return;
        }
        double costo = EstiloUI.leerDecimal(txtCosto.getText());
        if (costo <= 0) {
            EstiloUI.aviso(this, "Escribe un costo unitario válido (mayor que 0).");
            return;
        }
        int idInventario = EstiloUI.idSeleccionado(cmbItem);
        for (LineaDetalle l : lineas) {
            if (l.getIdInventario() == idInventario) {
                EstiloUI.aviso(this, "Esa prenda ya está en la lista. Quítala y agrégala de nuevo con la cantidad total.");
                return;
            }
        }
        LineaDetalle base = crudInv.buscarPorId(idInventario); // trae SKU y descripción
        if (base == null) {
            EstiloUI.error(this, "No se encontró esa prenda en el inventario.");
            return;
        }
        int cantidad = (Integer) spnCantidad.getValue();
        lineas.add(new LineaDetalle(idInventario, base.getSku(), base.getDescripcion(), cantidad, costo));
        txtCosto.setText("");
        spnCantidad.setValue(1);
        refrescarItems();
    }

    // QUITAR ÍTEM: saca de la lista el ítem seleccionado
    private void quitarItem() {
        if (idSeleccionado != 0) {
            EstiloUI.aviso(this, "Los ítems de una compra guardada no se pueden quitar.\nElimina la compra y vuelve a registrarla.");
            return;
        }
        int fila = tablaItems.getSelectedRow();
        if (fila < 0) {
            EstiloUI.aviso(this, "Selecciona en la tabla el ítem que quieres quitar.");
            return;
        }
        lineas.remove(fila);
        refrescarItems();
    }

    // Dibuja la lista de ítems en la tabla y calcula el total
    private void refrescarItems() {
        String[] titulos = {"SKU", "Producto", "Cantidad", "Costo Unit.", "Subtotal"};
        DefaultTableModel modelo = new DefaultTableModel(null, titulos) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        double total = 0;
        for (LineaDetalle l : lineas) {
            modelo.addRow(new Object[]{l.getSku(), l.getDescripcion(), l.getCantidad(),
                EstiloUI.dinero(l.getPrecio()), EstiloUI.dinero(l.getSubtotal())});
            total += l.getSubtotal();
        }
        tablaItems.setModel(modelo);
        lblTotal.setText("TOTAL: " + EstiloUI.dinero(total));
    }

    // =====================================================================
    // GUARDAR / EDITAR / ELIMINAR / LIMPIAR
    // =====================================================================

    // GUARDAR: registra la compra completa
    private void guardar() {
        if (idSeleccionado != 0) {
            EstiloUI.aviso(this, "Esta compra ya está guardada. Usa 'Editar' para cambiarla o 'Limpiar' para crear una nueva.");
            return;
        }
        if (cmbProveedor.getSelectedItem() == null) {
            EstiloUI.aviso(this, "Primero registra un proveedor.");
            return;
        }
        if (lineas.isEmpty()) {
            EstiloUI.aviso(this, "Agrega al menos un ítem a la compra.");
            return;
        }
        String estado = cmbEstado.getSelectedItem().toString();
        if (crud.insertarDato(EstiloUI.idSeleccionado(cmbProveedor), Sesion.getIdEmpleado(),
                txtFactura.getText(), estado, lineas)) {
            String nota = estado.equals("Recibido")
                    ? "\nSe sumaron las unidades al inventario."
                    : "\nEl inventario NO cambia hasta que la compra pase a 'Recibido'.";
            EstiloUI.exito(this, "¡Compra guardada exitosamente!" + nota);
            limpiar();
            cargarHistorial();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: cambia proveedor, factura o estado de la compra seleccionada
    private void editar() {
        if (idSeleccionado == 0) {
            EstiloUI.aviso(this, "Primero selecciona una compra del historial para editarla.");
            return;
        }
        if (crud.actualizarDatos(idSeleccionado, EstiloUI.idSeleccionado(cmbProveedor),
                txtFactura.getText(), cmbEstado.getSelectedItem().toString())) {
            EstiloUI.exito(this, "Compra actualizada (el inventario se ajustó si cambió el estado).");
            limpiar();
            cargarHistorial();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra la compra seleccionada (y revierte el stock si estaba Recibida)
    private void eliminar() {
        if (idSeleccionado == 0) {
            EstiloUI.aviso(this, "Primero selecciona una compra del historial para eliminarla.");
            return;
        }
        if (!EstiloUI.confirmar(this, "¿Eliminar la compra N° " + idSeleccionado + "?\n"
                + "Si estaba 'Recibida' se restarán sus unidades del inventario.")) {
            return;
        }
        if (crud.eliminarDatos(idSeleccionado)) {
            EstiloUI.exito(this, "Compra eliminada.");
            limpiar();
            cargarHistorial();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    // LIMPIAR: vuelve al modo "compra nueva"
    private void limpiar() {
        idSeleccionado = 0;
        lineas = new ArrayList<>();
        txtFactura.setText("");
        txtCosto.setText("");
        spnCantidad.setValue(1);
        cmbEstado.setSelectedIndex(0);
        if (cmbProveedor.getItemCount() > 0) {
            cmbProveedor.setSelectedIndex(0);
        }
        tablaHistorial.clearSelection();
        btnAgregar.setEnabled(true);
        btnQuitar.setEnabled(true);
        lblModo.setText("Ítems de la compra nueva");
        refrescarItems();
    }

    // =====================================================================
    // HISTORIAL
    // =====================================================================

    private void cargarHistorial() {
        tablaHistorial.setModel(crud.consultarDatos());
        EstiloUI.ocultarColumna(tablaHistorial, 7); // oculta id_proveedor
    }

    // Al hacer clic en una compra del historial: muestra sus datos y sus ítems
    private void seleccionarCompra() {
        if (tablaHistorial.getSelectedRow() < 0) {
            return;
        }
        idSeleccionado = Integer.parseInt(EstiloUI.datoFila(tablaHistorial, 0));
        EstiloUI.seleccionarPorId(cmbProveedor, Integer.parseInt(EstiloUI.datoFila(tablaHistorial, 7)));
        txtFactura.setText(EstiloUI.datoFila(tablaHistorial, 3));
        cmbEstado.setSelectedItem(EstiloUI.datoFila(tablaHistorial, 5));
        lineas = new ArrayList<>(crudDetalle.consultarLineas(idSeleccionado));
        btnAgregar.setEnabled(false); // una compra guardada no cambia de ítems
        btnQuitar.setEnabled(false);
        lblModo.setText("Ítems de la compra N° " + idSeleccionado);
        refrescarItems();
    }
}
