package vista;

import pruebamysql.Sesion;
import pruebamysql.crudInventario;
import pruebamysql.crudResumen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Menú principal con navegación a todos los módulos y panel de control.
 *
 * Permisos por rol:
 *   - Administrador: ve todos los módulos.
 *   - Vendedor: solo Caja y ventas, Compras, Inventario (consulta) y Clientes.
 *
 * El diseño es dinámico: al cambiar el tamaño de la ventana las tarjetas y la tabla se ajustan.
 */
public class MenuPrincipal extends JFrame {

    // Etiquetas de las tarjetas del panel de control (se actualizan al volver a esta ventana)
    private JLabel lblProductos, lblUnidades, lblVentasHoy, lblStockBajo;
    private JTable tablaStockBajo;

    private final crudResumen resumen = new crudResumen();
    private final crudInventario crudInv = new crudInventario();

    // Constructor que recibe quién inició sesión
    public MenuPrincipal(String nombreUsuario, int idRol) {
        setTitle("Estilo Prisma - Sistema de Inventario");
        setSize(1240, 780);
        setMinimumSize(new Dimension(1020, 660));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(EstiloUI.COLOR_FONDO);
        setLayout(new BorderLayout());

        // =====================================================================
        // PANEL LATERAL (blanco)
        // =====================================================================
        JPanel panelLateral = new JPanel(new BorderLayout());
        panelLateral.setBackground(Color.WHITE);
        panelLateral.setPreferredSize(new Dimension(260, 0));
        panelLateral.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, EstiloUI.COLOR_BORDE));

        // Logo
        JPanel panelLogo = new JPanel(new BorderLayout(12, 0));
        panelLogo.setOpaque(false);
        panelLogo.setBorder(new EmptyBorder(26, 24, 18, 20));
        panelLogo.add(crearInsignia(), BorderLayout.WEST);
        JPanel textoLogo = new JPanel(new GridLayout(2, 1));
        textoLogo.setOpaque(false);
        JLabel lblLogo = new JLabel("ESTILO PRISMA");
        lblLogo.setFont(EstiloUI.fuente(Font.BOLD, 17));
        lblLogo.setForeground(EstiloUI.COLOR_TEXTO);
        textoLogo.add(lblLogo);
        textoLogo.add(EstiloUI.etiquetaPequena("Sistema de inventario"));
        panelLogo.add(textoLogo, BorderLayout.CENTER);
        panelLateral.add(panelLogo, BorderLayout.NORTH);

        // Botones de navegación agrupados por secciones
        JPanel panelNav = new JPanel();
        panelNav.setLayout(new BoxLayout(panelNav, BoxLayout.Y_AXIS));
        panelNav.setOpaque(false);
        panelNav.setBorder(new EmptyBorder(4, 14, 10, 14));

        // Módulos para todos los roles
        agregarSeccion(panelNav, "VENTAS");
        agregarBoton(panelNav, "Caja y ventas", () -> new FormVentas().setVisible(true));
        agregarBoton(panelNav, "Compras", () -> new FormCompras().setVisible(true));
        agregarSeccion(panelNav, "INVENTARIO");
        agregarBoton(panelNav, "Existencias", () -> new FormInventario().setVisible(true));
        // Módulos solo para el Administrador (id_roles = 1)
        if (Sesion.esAdmin()) {
            agregarBoton(panelNav, "Productos", () -> new FormProductos().setVisible(true));
            agregarBoton(panelNav, "Tallas y colores", () -> new FormVariantes().setVisible(true));
            agregarBoton(panelNav, "Categorías", () -> new FormCategorias().setVisible(true));
        }
        agregarSeccion(panelNav, "PERSONAS");
        agregarBoton(panelNav, "Clientes", () -> new FormClientes().setVisible(true));
        if (Sesion.esAdmin()) {
            agregarBoton(panelNav, "Proveedores", () -> new FormProveedores().setVisible(true));
            agregarBoton(panelNav, "Personal", () -> new FormEmpleados().setVisible(true));
        }

        JScrollPane scrollNav = new JScrollPane(panelNav, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollNav.setBorder(null);
        EstiloUI.scrollFino(scrollNav);
        scrollNav.getViewport().setBackground(Color.WHITE);
        panelLateral.add(scrollNav, BorderLayout.CENTER);

        // Parte baja: usuario y cerrar sesión
        JPanel panelUsuario = new JPanel(new BorderLayout(0, 10));
        panelUsuario.setOpaque(false);
        panelUsuario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloUI.COLOR_BORDE),
                new EmptyBorder(16, 20, 18, 20)));
        String rolText = (idRol == Sesion.ROL_ADMINISTRADOR) ? "Administrador" : "Vendedor";
        JLabel lblNombre = new JLabel(nombreUsuario);
        lblNombre.setFont(EstiloUI.fuente(Font.BOLD, 14));
        lblNombre.setForeground(EstiloUI.COLOR_TEXTO);
        JPanel datosUsuario = new JPanel(new GridLayout(2, 1));
        datosUsuario.setOpaque(false);
        datosUsuario.add(lblNombre);
        datosUsuario.add(EstiloUI.etiquetaPequena(rolText));
        panelUsuario.add(datosUsuario, BorderLayout.NORTH);
        JButton btnSalir = EstiloUI.boton("Cerrar sesión", EstiloUI.COLOR_GRIS);
        btnSalir.addActionListener(e -> cerrarSesion());
        panelUsuario.add(btnSalir, BorderLayout.SOUTH);
        panelLateral.add(panelUsuario, BorderLayout.SOUTH);

        // =====================================================================
        // ZONA CENTRAL (panel de control)
        // =====================================================================
        JPanel zonaDerecha = new JPanel();
        JPanel centro = EstiloUI.pagina(zonaDerecha, "Panel de control", "Bienvenido(a), " + nombreUsuario + ". Este es el resumen de tu tienda.");
        add(panelLateral, BorderLayout.WEST);
        add(zonaDerecha, BorderLayout.CENTER);
        centro.setLayout(new BorderLayout(0, 20));

        // Tarjetas de resumen (se reparten el ancho disponible)
        JPanel panelTarjetas = new JPanel(new GridLayout(1, 4, 16, 0));
        panelTarjetas.setOpaque(false);
        panelTarjetas.setPreferredSize(new Dimension(0, 112));
        lblProductos = new JLabel("0");
        lblUnidades = new JLabel("0");
        lblVentasHoy = new JLabel("$ 0");
        lblStockBajo = new JLabel("0");
        panelTarjetas.add(crearTarjeta("Productos registrados", lblProductos, new Color(37, 99, 235)));
        panelTarjetas.add(crearTarjeta("Unidades en inventario", lblUnidades, new Color(22, 163, 74)));
        panelTarjetas.add(crearTarjeta("Ventas de hoy", lblVentasHoy, new Color(124, 58, 237)));
        panelTarjetas.add(crearTarjeta("Prendas con stock bajo", lblStockBajo, new Color(220, 38, 38)));
        centro.add(panelTarjetas, BorderLayout.NORTH);

        // Tabla de alertas
        EstiloUI.Tarjeta tarjetaAlertas = EstiloUI.tarjeta(new BorderLayout(0, 12), 20);
        tarjetaAlertas.add(EstiloUI.titulo("Prendas agotadas o con 5 unidades o menos"), BorderLayout.NORTH);
        tablaStockBajo = new JTable();
        EstiloUI.estilizarTabla(tablaStockBajo);
        tarjetaAlertas.add(EstiloUI.conScroll(tablaStockBajo), BorderLayout.CENTER);
        centro.add(tarjetaAlertas, BorderLayout.CENTER);

        // Cada vez que se vuelve al menú (después de vender o comprar) se actualizan las tarjetas
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                actualizarResumen();
            }
        });
        actualizarResumen();
    }

    // Vuelve a consultar los datos de las tarjetas y de la tabla de stock bajo
    private void actualizarResumen() {
        lblProductos.setText(String.valueOf(resumen.totalProductos()));
        lblUnidades.setText(String.valueOf(resumen.totalUnidades()));
        lblVentasHoy.setText(EstiloUI.dinero(resumen.ventasDeHoy()));
        lblStockBajo.setText(String.valueOf(resumen.variantesStockBajo()));
        tablaStockBajo.setModel(crudInv.consultarStockBajo());
    }

    // Cierra la sesión: cierra todas las ventanas abiertas y vuelve al login
    private void cerrarSesion() {
        Sesion.cerrar();
        for (Window ventana : Window.getWindows()) {
            ventana.dispose();
        }
        new LoginTienda().setVisible(true);
    }

    // =====================================================================
    // COMPONENTES DEL MENÚ
    // =====================================================================

    // Cuadro de color con la inicial de la marca
    private JComponent crearInsignia() {
        JComponent insignia = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(EstiloUI.COLOR_ACENTO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(Color.WHITE);
                g2.setFont(EstiloUI.fuente(Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("U", (getWidth() - fm.stringWidth("U")) / 2, (getHeight() + fm.getAscent()) / 2 - 3);
                g2.dispose();
            }
        };
        insignia.setPreferredSize(new Dimension(42, 42));
        return insignia;
    }

    // Título pequeño de grupo de botones
    private void agregarSeccion(JPanel panel, String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(EstiloUI.fuente(Font.BOLD, 11));
        lbl.setForeground(new Color(156, 163, 175));
        lbl.setBorder(new EmptyBorder(14, 12, 4, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
    }

    // Botón de navegación: al hacer clic ejecuta la acción (abre el formulario)
    private void agregarBoton(JPanel panel, String texto, Runnable accion) {
        JButton boton = new JButton(texto) {
            private boolean encima = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        encima = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        encima = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (encima) {
                    g2.setColor(EstiloUI.COLOR_ACENTO_SUAVE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    setForeground(EstiloUI.COLOR_ACENTO);
                } else {
                    setForeground(EstiloUI.COLOR_TEXTO);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        boton.setFont(EstiloUI.fuente(Font.PLAIN, 14));
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setBorder(new EmptyBorder(0, 14, 0, 0));
        boton.setContentAreaFilled(false);
        boton.setFocusPainted(false);
        boton.setOpaque(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        boton.setPreferredSize(new Dimension(200, 38));
        boton.addActionListener(e -> accion.run());
        panel.add(boton);
    }

    // Tarjeta del panel de control: título pequeño arriba y número grande abajo
    private JPanel crearTarjeta(String titulo, JLabel valor, Color colorAcento) {
        EstiloUI.Tarjeta tarjeta = EstiloUI.tarjeta(new GridLayout(2, 1, 0, 4), 20);

        String hex = String.format("#%02x%02x%02x", colorAcento.getRed(), colorAcento.getGreen(), colorAcento.getBlue());
        JLabel lblTitulo = new JLabel("<html><font color='" + hex + "'>●</font>&nbsp; " + titulo + "</html>");
        lblTitulo.setForeground(EstiloUI.COLOR_TEXTO_DIM);
        lblTitulo.setFont(EstiloUI.fuente(Font.PLAIN, 13));

        valor.setForeground(EstiloUI.COLOR_TEXTO);
        valor.setFont(EstiloUI.fuente(Font.BOLD, 30));

        tarjeta.add(lblTitulo);
        tarjeta.add(valor);
        return tarjeta;
    }
}
