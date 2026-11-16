package vista;

import pruebamysql.ItemCombo;
import pruebamysql.LineaDetalle;
import pruebamysql.Sesion;
import pruebamysql.conectionDB;
import pruebamysql.crudClientes;
import pruebamysql.crudDetalleVentas;
import pruebamysql.crudInventario;
import pruebamysql.crudVentas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto de Venta (POS): caja, carrito, cobro y factura.
 *
 * Flujo para una venta nueva:
 *   1. Elegir el cliente y el método de pago.
 *   2. Escribir el SKU de la prenda y la cantidad, y pulsar "Añadir al carrito".
 *   3. Pulsar "PROCESAR VENTA" (Guardar). Las unidades se RESTAN del inventario.
 *      Si no hay stock suficiente la venta no se guarda.
 *
 * Al hacer clic en una venta del historial se muestran sus datos y se puede ver su factura.
 * Editar y Eliminar solo los puede usar el Administrador.
 */
public class FormVentas extends JFrame {

    private JComboBox<ItemCombo> cmbCliente;
    private JComboBox<String> cmbPago, cmbEstado;
    private JTextField txtSku;
    private JSpinner spnCantidad;
    private JButton btnAnadir, btnQuitar;
    private JLabel lblModo, lblTotal, lblInfo;
    private JTable tablaCarrito, tablaHistorial;

    // Ítems que se están mostrando / armando (el carrito)
    private List<LineaDetalle> lineas = new ArrayList<>();
    // 0 = venta nueva; mayor que 0 = id de la venta del historial que está seleccionada
    private int idSeleccionado = 0;

    private final crudVentas crud = new crudVentas();
    private final crudDetalleVentas crudDetalle = new crudDetalleVentas();
    private final crudClientes crudCli = new crudClientes();
    private final crudInventario crudInv = new crudInventario();

    public FormVentas() {
        setTitle("Punto de Venta (POS) - Estilo Prisma");
        setSize(1340, 820);
        setMinimumSize(new Dimension(1080, 720));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // El cajero sale de la sesión y la fecha del reloj del equipo
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JPanel centro = EstiloUI.pagina(this, "Caja y ventas",
                "Cajero: " + Sesion.getNombre() + "   •   Fecha: " + hoy);

        // =================== BARRA SUPERIOR (datos de la venta) ===================
        cmbCliente = EstiloUI.combo();
        cmbCliente.setModel(crudCli.modeloCombo());
        JButton btnNuevoCliente = EstiloUI.boton("+ Nuevo", EstiloUI.COLOR_GRIS);
        btnNuevoCliente.setPreferredSize(new Dimension(90, 38));
        btnNuevoCliente.addActionListener(e -> new FormClientes().setVisible(true));
        cmbPago = EstiloUI.combo();
        cmbPago.setModel(new DefaultComboBoxModel<>(new String[]{"Efectivo", "Tarjeta", "Transferencia"}));
        cmbPago.setPreferredSize(new Dimension(160, 38));
        cmbEstado = EstiloUI.combo();
        cmbEstado.setModel(new DefaultComboBoxModel<>(new String[]{"Pagada", "Pendiente", "Cancelada"}));
        cmbEstado.setPreferredSize(new Dimension(140, 38));
        JComponent barraDatos = EstiloUI.barraCampos(
                new String[]{"Cliente", null, "Método de pago", "Estado"},
                new JComponent[]{cmbCliente, btnNuevoCliente, cmbPago, cmbEstado},
                new double[]{1, 0, 0, 0});

        // =================== PANEL IZQUIERDO (agregar producto + acciones) ===================
        txtSku = EstiloUI.campo();
        txtSku.addActionListener(e -> anadirAlCarrito()); // Enter = añadir
        spnCantidad = EstiloUI.spinner(1, 1, 1000);
        btnAnadir = EstiloUI.boton("Añadir al carrito", EstiloUI.COLOR_VERDE);
        btnQuitar = EstiloUI.boton("Quitar", EstiloUI.COLOR_GRIS);
        btnAnadir.addActionListener(e -> anadirAlCarrito());
        btnQuitar.addActionListener(e -> quitarDelCarrito());
        lblInfo = new JLabel(" ");
        lblInfo.setForeground(EstiloUI.COLOR_TEXTO_DIM);
        lblInfo.setFont(EstiloUI.fuente(Font.PLAIN, 12));
        JButton btnFactura = EstiloUI.boton("Ver / imprimir factura", EstiloUI.COLOR_AZUL);
        btnFactura.addActionListener(e -> verFactura());

        EstiloUI.PanelCampos campos = new EstiloUI.PanelCampos();
        campos.campo("Código SKU (Enter para añadir)", txtSku);
        campos.campo("Cantidad", spnCantidad);
        campos.botones(btnAnadir, btnQuitar);
        campos.completo(lblInfo, 8);
        campos.seccion("Acciones");
        // Botones: [0] Guardar, [1] Editar, [2] Eliminar, [3] Limpiar
        JButton[] botones = EstiloUI.botonesCrud(campos);
        campos.completo(btnFactura, 8);
        campos.relleno();
        botones[0].addActionListener(e -> guardar());
        botones[1].addActionListener(e -> editar());
        botones[2].addActionListener(e -> eliminar());
        botones[3].addActionListener(e -> limpiar());

        // Permiso: el vendedor registra ventas y genera facturas, pero no edita ni elimina
        if (!Sesion.esAdmin()) {
            EstiloUI.bloquear(botones[1], "Solo el Administrador puede editar ventas");
            EstiloUI.bloquear(botones[2], "Solo el Administrador puede eliminar ventas");
        }

        // =================== ZONA DERECHA (carrito + historial) ===================
        EstiloUI.Tarjeta tarjetaCarrito = EstiloUI.tarjeta(new BorderLayout(0, 12), 20);
        lblModo = EstiloUI.titulo("Carrito de la venta nueva");
        tarjetaCarrito.add(lblModo, BorderLayout.NORTH);
        tablaCarrito = new JTable();
        EstiloUI.estilizarTabla(tablaCarrito);
        tarjetaCarrito.add(EstiloUI.conScroll(tablaCarrito), BorderLayout.CENTER);

        // Barra de total y botón de cobro
        JPanel panelTotales = new JPanel(new BorderLayout());
        panelTotales.setOpaque(false);
        JLabel lblTotalTxt = EstiloUI.etiquetaPequena("TOTAL A PAGAR");
        lblTotal = new JLabel("$ 0");
        lblTotal.setForeground(EstiloUI.COLOR_ACENTO);
        lblTotal.setFont(EstiloUI.fuente(Font.BOLD, 30));
        JPanel textosTotal = new JPanel(new GridLayout(2, 1));
        textosTotal.setOpaque(false);
        textosTotal.add(lblTotalTxt);
        textosTotal.add(lblTotal);
        JButton btnCobrar = EstiloUI.boton("PROCESAR VENTA", EstiloUI.COLOR_ACENTO);
        btnCobrar.setFont(EstiloUI.fuente(Font.BOLD, 15));
        btnCobrar.setPreferredSize(new Dimension(230, 54));
        btnCobrar.addActionListener(e -> guardar()); // hace lo mismo que el botón Guardar
        panelTotales.add(textosTotal, BorderLayout.WEST);
        panelTotales.add(btnCobrar, BorderLayout.EAST);
        tarjetaCarrito.add(panelTotales, BorderLayout.SOUTH);

        tablaHistorial = new JTable();
        EstiloUI.estilizarTabla(tablaHistorial);
        refrescarCarrito();
        cargarHistorial();
        tablaHistorial.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarVenta();
            }
        });
        JComponent tarjetaHistorial = EstiloUI.panelTabla(tablaHistorial, "Historial de ventas (clic para ver el detalle)");

        JPanel derecha = new JPanel(new BorderLayout(0, 16));
        derecha.setOpaque(false);
        derecha.add(barraDatos, BorderLayout.NORTH);
        derecha.add(EstiloUI.divisor(tarjetaCarrito, tarjetaHistorial), BorderLayout.CENTER);

        centro.add(EstiloUI.tarjetaFormulario("Agregar producto", campos, 330), BorderLayout.WEST);
        centro.add(derecha, BorderLayout.CENTER);

        // Al volver a esta ventana se actualiza la lista de clientes (por si se creó uno nuevo)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                EstiloUI.recargarCombo(cmbCliente, crudCli.modeloCombo());
            }
        });
    }


    // =====================================================================
    // CARRITO
    // =====================================================================

    // AÑADIR AL CARRITO: busca la prenda por SKU y la agrega si hay stock suficiente
    private void anadirAlCarrito() {
        if (idSeleccionado != 0) {
            EstiloUI.aviso(this, "Estás viendo una venta ya guardada. Pulsa 'Limpiar' para empezar una nueva.");
            return;
        }
        String sku = txtSku.getText().trim();
        if (sku.isEmpty()) {
            EstiloUI.aviso(this, "Escribe el código SKU de la prenda.");
            return;
        }
        LineaDetalle encontrada = crudInv.buscarPorSku(sku);
        if (encontrada == null) {
            EstiloUI.aviso(this, "No existe ninguna prenda con el SKU '" + sku + "'.");
            return;
        }

        int cantidad = (Integer) spnCantidad.getValue();

        // ¿Esa prenda ya está en el carrito? Entonces se suman las cantidades
        LineaDetalle existente = null;
        for (LineaDetalle l : lineas) {
            if (l.getIdInventario() == encontrada.getIdInventario()) {
                existente = l;
            }
        }
        int enCarrito = (existente == null) ? 0 : existente.getCantidad();

        // Validación de stock (la base de datos vuelve a validarlo al guardar)
        if (enCarrito + cantidad > encontrada.getStockDisponible()) {
            EstiloUI.aviso(this, "Stock insuficiente de " + encontrada.getDescripcion() + ".\n"
                    + "Disponible: " + encontrada.getStockDisponible() + "  |  Ya en el carrito: " + enCarrito);
            return;
        }

        if (existente != null) {
            existente.setCantidad(enCarrito + cantidad);
        } else {
            encontrada.setCantidad(cantidad);
            lineas.add(encontrada);
        }
        lblInfo.setText("<html>" + encontrada.getDescripcion() + "<br>Stock disponible: "
                + encontrada.getStockDisponible() + "</html>");
        txtSku.setText("");
        spnCantidad.setValue(1);
        txtSku.requestFocus();
        refrescarCarrito();
    }

    // QUITAR: saca del carrito el ítem seleccionado
    private void quitarDelCarrito() {
        if (idSeleccionado != 0) {
            EstiloUI.aviso(this, "Los ítems de una venta guardada no se pueden quitar.\nElimina la venta y vuelve a registrarla.");
            return;
        }
        int fila = tablaCarrito.getSelectedRow();
        if (fila < 0) {
            EstiloUI.aviso(this, "Selecciona en la tabla el ítem que quieres quitar.");
            return;
        }
        lineas.remove(fila);
        refrescarCarrito();
    }

    // Dibuja el carrito en la tabla y calcula el total
    private void refrescarCarrito() {
        String[] titulos = {"SKU", "Descripción", "Cant.", "Precio Unit.", "Subtotal"};
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
        tablaCarrito.setModel(modelo);
        lblTotal.setText(EstiloUI.dinero(total));
    }

    // =====================================================================
    // GUARDAR / EDITAR / ELIMINAR / LIMPIAR / FACTURA
    // =====================================================================

    // GUARDAR (PROCESAR VENTA): registra la venta completa y resta el stock
    private void guardar() {
        if (idSeleccionado != 0) {
            EstiloUI.aviso(this, "Esta venta ya está guardada. Usa 'Editar' para cambiarla o 'Limpiar' para crear una nueva.");
            return;
        }
        if (cmbCliente.getSelectedItem() == null) {
            EstiloUI.aviso(this, "Primero registra un cliente (botón '+ Nuevo').");
            return;
        }
        if (lineas.isEmpty()) {
            EstiloUI.aviso(this, "El carrito está vacío. Añade al menos una prenda.");
            return;
        }
        if (crud.insertarDato(EstiloUI.idSeleccionado(cmbCliente), Sesion.getIdEmpleado(),
                cmbPago.getSelectedItem().toString(), cmbEstado.getSelectedItem().toString(), lineas)) {
            int idVenta = crud.ultimoIdVenta;
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¡Venta N° " + idVenta + " guardada! Se descontó el stock del inventario.\n¿Quieres ver la factura?",
                    "Venta guardada", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarHistorial();
            if (respuesta == JOptionPane.YES_OPTION) {
                new FormFactura(this, crud.generarFactura(idVenta)).setVisible(true);
            }
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // EDITAR: cambia cliente, método de pago o estado de la venta seleccionada
    private void editar() {
        if (idSeleccionado == 0) {
            EstiloUI.aviso(this, "Primero selecciona una venta del historial para editarla.");
            return;
        }
        if (cmbCliente.getSelectedItem() == null) {
            EstiloUI.aviso(this, "Selecciona un cliente.");
            return;
        }
        if (crud.actualizarDatos(idSeleccionado, EstiloUI.idSeleccionado(cmbCliente),
                cmbPago.getSelectedItem().toString(), cmbEstado.getSelectedItem().toString())) {
            EstiloUI.exito(this, "Venta actualizada (el inventario se ajustó si cambió a/desde 'Cancelada').");
            limpiar();
            cargarHistorial();
        } else {
            EstiloUI.error(this, conectionDB.ultimoError);
        }
    }

    // ELIMINAR: borra la venta seleccionada (y devuelve las unidades al inventario)
    private void eliminar() {
        if (idSeleccionado == 0) {
            EstiloUI.aviso(this, "Primero selecciona una venta del historial para eliminarla.");
            return;
        }
        if (!EstiloUI.confirmar(this, "¿Eliminar la venta N° " + idSeleccionado + "?\n"
                + "Las unidades vuelven al inventario (si la venta no estaba cancelada).")) {
            return;
        }
        if (crud.eliminarDatos(idSeleccionado)) {
            EstiloUI.exito(this, "Venta eliminada.");
            limpiar();
            cargarHistorial();
        } else {
            EstiloUI.bloqueado(this, conectionDB.ultimoError);
        }
    }

    // FACTURA: muestra la factura de la venta seleccionada en el historial
    private void verFactura() {
        if (idSeleccionado == 0) {
            EstiloUI.aviso(this, "Selecciona una venta del historial para ver su factura.");
            return;
        }
        new FormFactura(this, crud.generarFactura(idSeleccionado)).setVisible(true);
    }

    // LIMPIAR: vuelve al modo "venta nueva"
    private void limpiar() {
        idSeleccionado = 0;
        lineas = new ArrayList<>();
        txtSku.setText("");
        spnCantidad.setValue(1);
        cmbPago.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
        if (cmbCliente.getItemCount() > 0) {
            cmbCliente.setSelectedIndex(0);
        }
        tablaHistorial.clearSelection();
        btnAnadir.setEnabled(true);
        btnQuitar.setEnabled(true);
        lblInfo.setText(" ");
        lblModo.setText("Carrito de la venta nueva");
        refrescarCarrito();
    }

    // =====================================================================
    // HISTORIAL
    // =====================================================================

    private void cargarHistorial() {
        tablaHistorial.setModel(crud.consultarDatos());
        EstiloUI.ocultarColumna(tablaHistorial, 7); // oculta id_cliente
    }

    // Al hacer clic en una venta del historial: muestra sus datos y sus ítems
    private void seleccionarVenta() {
        if (tablaHistorial.getSelectedRow() < 0) {
            return;
        }
        idSeleccionado = Integer.parseInt(EstiloUI.datoFila(tablaHistorial, 0));
        EstiloUI.seleccionarPorId(cmbCliente, Integer.parseInt(EstiloUI.datoFila(tablaHistorial, 7)));
        cmbPago.setSelectedItem(EstiloUI.datoFila(tablaHistorial, 4));
        cmbEstado.setSelectedItem(EstiloUI.datoFila(tablaHistorial, 5));
        lineas = new ArrayList<>(crudDetalle.consultarLineas(idSeleccionado));
        btnAnadir.setEnabled(false); // una venta guardada no cambia de ítems
        btnQuitar.setEnabled(false);
        lblModo.setText("Ítems de la venta N° " + idSeleccionado);
        refrescarCarrito();
    }
}
