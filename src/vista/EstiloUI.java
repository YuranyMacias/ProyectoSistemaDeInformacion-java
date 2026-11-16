package vista;

import pruebamysql.ItemCombo;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Diseño visual de Estilo Prisma (tema claro / blanco).
 * Aquí están los colores, las fuentes y los componentes (botones, campos, tablas, tarjetas...)
 * que usan TODAS las pantallas, para que se vean iguales y no repetir código.
 * Todo se acomoda solo al cambiar el tamaño de la ventana (usa layouts, no posiciones fijas).
 */
public class EstiloUI {

    // ----- Paleta de colores -----
    public static final Color COLOR_FONDO = new Color(243, 244, 246);       // fondo general (gris muy claro)
    public static final Color COLOR_PANEL = Color.WHITE;                     // tarjetas
    public static final Color COLOR_ACENTO = new Color(128, 0, 32);          // color de marca (vino)
    public static final Color COLOR_ACENTO_SUAVE = new Color(253, 238, 242); // selección / hover
    public static final Color COLOR_TEXTO = new Color(31, 41, 55);
    public static final Color COLOR_TEXTO_DIM = new Color(107, 114, 128);
    public static final Color COLOR_BORDE = new Color(229, 231, 235);
    public static final Color COLOR_GRIS = new Color(229, 231, 235);         // botón secundario
    public static final Color COLOR_VERDE = new Color(22, 163, 74);
    public static final Color COLOR_ROJO = new Color(220, 38, 38);
    public static final Color COLOR_AZUL = new Color(37, 99, 235);
    private static final Color FILA_PAR = new Color(249, 250, 251);

    public static final String FUENTE = "Segoe UI";

    public static Font fuente(int estilo, int tamano) {
        return new Font(FUENTE, estilo, tamano);
    }

    // Se llama una vez al iniciar el programa: pinta de blanco los cuadros de mensaje
    public static void instalar() {
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", COLOR_TEXTO);
        UIManager.put("OptionPane.messageFont", fuente(Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", fuente(Font.BOLD, 13));
        UIManager.put("ToolTip.font", fuente(Font.PLAIN, 12));
    }

    // =====================================================================
    // BORDE REDONDEADO
    // =====================================================================
    private static class BordeRedondo extends AbstractBorder {
        private final Color color;
        private final int arco;

        BordeRedondo(Color color, int arco) {
            this.color = color;
            this.arco = arco;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, arco, arco);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }
    }

    private static CompoundBorder bordeCampo(Color color) {
        return new CompoundBorder(new BordeRedondo(color, 10), new EmptyBorder(7, 11, 7, 11));
    }

    // =====================================================================
    // TARJETA (panel blanco con esquinas redondeadas)
    // =====================================================================
    public static class Tarjeta extends JPanel {
        public Tarjeta(LayoutManager layout, int margen) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(margen, margen, margen, margen));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_PANEL);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(COLOR_BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Tarjeta tarjeta(LayoutManager layout, int margen) {
        return new Tarjeta(layout, margen);
    }

    // =====================================================================
    // PÁGINA (encabezado + zona central) y FORMULARIO
    // =====================================================================

    // Prepara una ventana: fondo gris, título arriba y devuelve el panel central donde van las tarjetas
    public static JPanel pagina(JFrame ventana, String titulo, String subtitulo) {
        ventana.getContentPane().setBackground(COLOR_FONDO);
        ventana.setLayout(new BorderLayout());
        return pagina(ventana.getContentPane(), titulo, subtitulo);
    }

    // Igual, pero dentro de cualquier panel (el menú principal lo usa para la zona derecha)
    public static JPanel pagina(Container ventana, String titulo, String subtitulo) {
        ventana.setBackground(COLOR_FONDO);
        ventana.setLayout(new BorderLayout());

        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 2));
        encabezado.setOpaque(false);
        encabezado.setBorder(new EmptyBorder(22, 28, 6, 28));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(fuente(Font.BOLD, 24));
        lblTitulo.setForeground(COLOR_TEXTO);
        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(fuente(Font.PLAIN, 13));
        lblSub.setForeground(COLOR_TEXTO_DIM);
        encabezado.add(lblTitulo);
        encabezado.add(lblSub);
        ventana.add(encabezado, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(20, 0));
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(10, 28, 24, 28));
        ventana.add(centro, BorderLayout.CENTER);
        return centro;
    }

    // Panel que acomoda etiquetas y campos uno debajo de otro (se estira con la ventana)
    public static class PanelCampos extends JPanel {
        private int fila = 0;

        public PanelCampos() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        private GridBagConstraints celda(int x, int ancho, int arriba) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = x;
            c.gridy = fila;
            c.gridwidth = ancho;
            c.weightx = ancho == 2 ? 1 : 0.5;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            int izq = (ancho == 1 && x == 1) ? 5 : 0;
            int der = (ancho == 1 && x == 0) ? 5 : 0;
            c.insets = new Insets(arriba, izq, 0, der);
            return c;
        }

        // Título de una sección dentro del formulario
        public void seccion(String texto) {
            JLabel lbl = new JLabel(texto);
            lbl.setFont(fuente(Font.BOLD, 16));
            lbl.setForeground(COLOR_TEXTO);
            add(lbl, celda(0, 2, fila == 0 ? 0 : 22));
            fila++;
        }

        // Etiqueta pequeña + componente a todo el ancho
        public void campo(String etiqueta, JComponent componente) {
            add(etiquetaPequena(etiqueta), celda(0, 2, 11));
            fila++;
            add(componente, celda(0, 2, 5));
            fila++;
        }

        // Dos campos lado a lado
        public void dosColumnas(String etiqueta1, JComponent c1, String etiqueta2, JComponent c2) {
            add(etiquetaPequena(etiqueta1), celda(0, 1, 11));
            add(etiquetaPequena(etiqueta2), celda(1, 1, 11));
            fila++;
            add(c1, celda(0, 1, 5));
            add(c2, celda(1, 1, 5));
            fila++;
        }

        // Componente suelto a todo el ancho (botón grande, mensaje...)
        public void completo(JComponent componente, int arriba) {
            add(componente, celda(0, 2, arriba));
            fila++;
        }

        // Botones en 2 columnas
        public void botones(JButton... botones) {
            for (int i = 0; i < botones.length; i += 2) {
                int arriba = (i == 0) ? 20 : 8;
                if (i + 1 < botones.length) {
                    add(botones[i], celda(0, 1, arriba));
                    add(botones[i + 1], celda(1, 1, arriba));
                } else {
                    add(botones[i], celda(0, 2, arriba));
                }
                fila++;
            }
        }

        // Empuja todo hacia arriba cuando sobra espacio
        public void relleno() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = fila;
            c.gridwidth = 2;
            c.weighty = 1;
            c.fill = GridBagConstraints.VERTICAL;
            JPanel vacio = new JPanel();
            vacio.setOpaque(false);
            add(vacio, c);
            fila++;
        }
    }

    // Tarjeta horizontal con campos en fila (etiqueta arriba de cada componente).
    // "pesos" indica cuánto se estira cada columna al agrandar la ventana (0 = ancho fijo).
    public static JComponent barraCampos(String[] etiquetas, JComponent[] componentes, double[] pesos) {
        Tarjeta tarjeta = new Tarjeta(new GridBagLayout(), 18);
        for (int i = 0; i < componentes.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.weightx = pesos[i];
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(0, i == 0 ? 0 : 12, 0, 0);
            if (etiquetas[i] != null) {
                c.gridy = 0;
                tarjeta.add(etiquetaPequena(etiquetas[i]), c);
            }
            c.gridy = 1;
            c.insets = new Insets(5, i == 0 ? 0 : 12, 0, 0);
            tarjeta.add(componentes[i], c);
        }
        return tarjeta;
    }

    // Dos paneles uno sobre otro con una línea divisoria que se puede arrastrar
    public static JSplitPane divisor(JComponent arriba, JComponent abajo) {
        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.setResizeWeight(0.5);
        sp.setDividerSize(16);
        sp.setContinuousLayout(true);
        sp.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(COLOR_FONDO); // divisor liso, sin puntitos
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });
        sp.setBorder(null);
        return sp;
    }

    // Mete un PanelCampos en una tarjeta blanca de ancho fijo (con scroll si la ventana es muy baja)
    public static JComponent tarjetaFormulario(String titulo, PanelCampos campos, int ancho) {
        Tarjeta tarjeta = new Tarjeta(new BorderLayout(0, 4), 22);
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(fuente(Font.BOLD, 17));
        lbl.setForeground(COLOR_TEXTO);
        tarjeta.add(lbl, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(campos, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUI(new ScrollFino());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        tarjeta.add(scroll, BorderLayout.CENTER);
        tarjeta.setPreferredSize(new Dimension(ancho, 0));
        return tarjeta;
    }

    // =====================================================================
    // COMPONENTES
    // =====================================================================

    public static JLabel etiquetaPequena(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(fuente(Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXTO_DIM);
        return lbl;
    }

    // Etiqueta de texto normal
    public static JLabel etiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(fuente(Font.PLAIN, 13));
        lbl.setForeground(COLOR_TEXTO);
        return lbl;
    }

    // Título de sección (negrita)
    public static JLabel titulo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(fuente(Font.BOLD, 17));
        lbl.setForeground(COLOR_TEXTO);
        return lbl;
    }

    private static void estilizarCampo(JTextField txt) {
        txt.setBackground(Color.WHITE);
        txt.setForeground(COLOR_TEXTO);
        txt.setCaretColor(COLOR_TEXTO);
        txt.setFont(fuente(Font.PLAIN, 14));
        txt.setBorder(bordeCampo(COLOR_BORDE));
        txt.setDisabledTextColor(COLOR_TEXTO_DIM);
        // el borde se pone de color al hacer clic en el campo
        txt.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txt.setBorder(bordeCampo(COLOR_ACENTO));
            }

            @Override
            public void focusLost(FocusEvent e) {
                txt.setBorder(bordeCampo(COLOR_BORDE));
            }
        });
    }

    public static JTextField campo() {
        JTextField txt = new JTextField();
        estilizarCampo(txt);
        return txt;
    }

    public static JPasswordField campoClave() {
        JPasswordField txt = new JPasswordField();
        estilizarCampo(txt);
        return txt;
    }

    // Caja de búsqueda con texto de ayuda
    public static JTextField campoBusqueda(String ayuda) {
        JTextField txt = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(156, 163, 175));
                    g2.setFont(getFont());
                    g2.drawString(ayuda, getInsets().left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                    g2.dispose();
                }
            }
        };
        estilizarCampo(txt);
        txt.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txt.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                txt.repaint();
            }
        });
        return txt;
    }

    // Lista desplegable. Sirve para textos simples (String) o para ItemCombo.
    public static <T> JComboBox<T> combo() {
        JComboBox<T> cmb = new JComboBox<>();
        cmb.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(Color.WHITE); // sin el fondo gris por defecto
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected JButton createArrowButton() {
                // flecha dibujada a mano para que se vea limpia
                JButton flecha = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(COLOR_TEXTO_DIM);
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        g2.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
                        g2.dispose();
                    }
                };
                flecha.setBorder(null);
                flecha.setContentAreaFilled(false);
                flecha.setFocusPainted(false);
                flecha.setPreferredSize(new Dimension(28, 28));
                return flecha;
            }
        });
        cmb.setBackground(Color.WHITE);
        cmb.setForeground(COLOR_TEXTO);
        cmb.setFont(fuente(Font.PLAIN, 14));
        cmb.setBorder(new CompoundBorder(new BordeRedondo(COLOR_BORDE, 10), new EmptyBorder(2, 4, 2, 0)));
        cmb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(list, value, index, seleccionado, foco);
                setBorder(new EmptyBorder(7, 10, 7, 10));
                setBackground(seleccionado ? COLOR_ACENTO_SUAVE : Color.WHITE);
                setForeground(seleccionado ? COLOR_ACENTO : COLOR_TEXTO);
                return this;
            }
        });
        cmb.setPreferredSize(new Dimension(100, 38));
        return cmb;
    }

    // Selector numérico (cantidad)
    public static JSpinner spinner(int inicial, int minimo, int maximo) {
        JSpinner spn = new JSpinner(new SpinnerNumberModel(inicial, minimo, maximo, 1));
        spn.setFont(fuente(Font.PLAIN, 14));
        spn.setBorder(new BordeRedondo(COLOR_BORDE, 10));
        JTextField interno = ((JSpinner.DefaultEditor) spn.getEditor()).getTextField();
        interno.setBorder(new EmptyBorder(7, 10, 7, 6));
        interno.setFont(fuente(Font.PLAIN, 14));
        spn.setPreferredSize(new Dimension(100, 38));
        return spn;
    }

    // Botón redondeado con efecto al pasar el mouse
    public static class BotonRedondo extends JButton {
        private final Color base;
        private boolean encima = false;

        BotonRedondo(String texto, Color base) {
            super(texto);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(fuente(Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setForeground(esClaro(base) ? COLOR_TEXTO : Color.WHITE);
            setPreferredSize(new Dimension(100, 40));
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
            Color color = base;
            if (!isEnabled()) {
                color = new Color(236, 238, 241);
            } else if (getModel().isPressed()) {
                color = mezclar(base, Color.BLACK, 0.15f);
            } else if (encima) {
                color = esClaro(base) ? mezclar(base, Color.BLACK, 0.07f) : mezclar(base, Color.WHITE, 0.15f);
            }
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static boolean esClaro(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 190;
    }

    private static Color mezclar(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static JButton boton(String texto, Color color) {
        return new BotonRedondo(texto, color);
    }

    // Crea los 4 botones estándar de un formulario CRUD y los pone en el panel:
    // devuelve {Guardar, Editar, Eliminar, Limpiar}
    public static JButton[] botonesCrud(PanelCampos panel) {
        JButton guardar = boton("Guardar", COLOR_ACENTO);
        JButton editar = boton("Editar", COLOR_AZUL);
        JButton eliminar = boton("Eliminar", COLOR_ROJO);
        JButton limpiar = boton("Limpiar", COLOR_GRIS);
        panel.botones(guardar, editar, eliminar, limpiar);
        return new JButton[]{guardar, editar, eliminar, limpiar};
    }

    // Desactiva un botón (se usa para los permisos por rol)
    public static void bloquear(JButton boton, String motivo) {
        boton.setEnabled(false);
        boton.setToolTipText(motivo);
    }

    // =====================================================================
    // TABLAS
    // =====================================================================

    private static class RenderCelda extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, false, fila, columna);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(fuente(Font.PLAIN, 13));
            if (seleccionada) {
                setBackground(COLOR_ACENTO_SUAVE);
                setForeground(COLOR_ACENTO);
            } else {
                setBackground(fila % 2 == 0 ? Color.WHITE : FILA_PAR);
                setForeground(COLOR_TEXTO);
            }
            return this;
        }
    }

    private static class RenderEncabezado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            setBackground(FILA_PAR);
            setForeground(COLOR_TEXTO_DIM);
            setFont(fuente(Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new CompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDE),
                    new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    // Aplica el estilo claro a una tabla
    public static void estilizarTabla(JTable tabla) {
        tabla.setBackground(Color.WHITE);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setSelectionBackground(COLOR_ACENTO_SUAVE);
        tabla.setSelectionForeground(COLOR_ACENTO);
        tabla.setRowHeight(38);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.setDefaultRenderer(Object.class, new RenderCelda());
        tabla.getTableHeader().setDefaultRenderer(new RenderEncabezado());
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));
        // Cada vez que se carga un modelo nuevo, las columnas se ajustan al contenido
        tabla.addPropertyChangeListener("model", e -> SwingUtilities.invokeLater(() -> ajustarColumnas(tabla)));
    }

    // Da a cada columna un ancho acorde a su contenido (el resto lo reparte el tamaño de la ventana)
    private static void ajustarColumnas(JTable tabla) {
        for (int c = 0; c < tabla.getColumnCount(); c++) {
            int largo = String.valueOf(tabla.getColumnName(c)).length();
            for (int f = 0; f < Math.min(tabla.getRowCount(), 40); f++) {
                Object v = tabla.getValueAt(f, c);
                if (v != null) {
                    largo = Math.max(largo, v.toString().length());
                }
            }
            tabla.getColumnModel().getColumn(c).setPreferredWidth(Math.max(60, Math.min(largo * 8 + 34, 320)));
        }
    }

    // Barras de desplazamiento delgadas para cualquier JScrollPane
    public static void scrollFino(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new ScrollFino());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.getHorizontalScrollBar().setUI(new ScrollFino());
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
    }

    // Tabla dentro de un JScrollPane sin bordes pesados
    public static JScrollPane conScroll(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDE, 1));
        scroll.getVerticalScrollBar().setUI(new ScrollFino());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.getHorizontalScrollBar().setUI(new ScrollFino());
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        return scroll;
    }

    // Tarjeta con título, buscador y tabla (el buscador filtra todas las columnas mientras se escribe)
    public static JComponent panelTabla(JTable tabla, String titulo) {
        Tarjeta tarjeta = new Tarjeta(new BorderLayout(0, 14), 20);

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        barra.add(titulo(titulo), BorderLayout.WEST);
        JTextField txtBuscar = campoBusqueda("Buscar en la tabla...");
        txtBuscar.setPreferredSize(new Dimension(240, 38));
        barra.add(txtBuscar, BorderLayout.EAST);
        tarjeta.add(barra, BorderLayout.NORTH);

        // Permite ordenar haciendo clic en el encabezado y filtrar con el buscador
        tabla.setAutoCreateRowSorter(true);
        Runnable filtrar = () -> {
            if (tabla.getRowSorter() instanceof TableRowSorter) {
                String texto = txtBuscar.getText().trim();
                @SuppressWarnings("unchecked")
                TableRowSorter<javax.swing.table.TableModel> sorter = (TableRowSorter<javax.swing.table.TableModel>) tabla.getRowSorter();
                sorter.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
            }
        };
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar.run(); }
            public void removeUpdate(DocumentEvent e) { filtrar.run(); }
            public void changedUpdate(DocumentEvent e) { filtrar.run(); }
        });
        // Cuando la tabla se recarga desde MySQL, se vuelve a aplicar el filtro escrito
        tabla.addPropertyChangeListener("model", e -> {
            filtrar.run();
            SwingUtilities.invokeLater(filtrar);
        });

        tarjeta.add(conScroll(tabla), BorderLayout.CENTER);
        return tarjeta;
    }

    // Barra de desplazamiento delgada y redondeada
    private static class ScrollFino extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(209, 213, 219);
            trackColor = Color.WHITE;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return botonVacio();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return botonVacio();
        }

        private JButton botonVacio() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 8, 8);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    // Esconde una columna de la tabla (sus datos siguen en el modelo, solo no se ven).
    // Sirve para guardar ids (por ejemplo id_categoria) sin mostrarlos.
    public static void ocultarColumna(JTable tabla, int indiceColumna) {
        tabla.removeColumn(tabla.getColumnModel().getColumn(indiceColumna));
    }

    // Lee un dato de la fila seleccionada (por posición de columna del modelo).
    // Funciona aunque la tabla esté ordenada o filtrada.
    public static String datoFila(JTable tabla, int columna) {
        int fila = tabla.convertRowIndexToModel(tabla.getSelectedRow());
        Object valor = tabla.getModel().getValueAt(fila, columna);
        return valor == null ? "" : valor.toString();
    }

    // =====================================================================
    // COMBOS
    // =====================================================================

    // Selecciona en el combo el ítem que tenga ese id
    public static void seleccionarPorId(JComboBox<ItemCombo> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    // Devuelve el id del ítem seleccionado (o -1 si el combo está vacío)
    public static int idSeleccionado(JComboBox<ItemCombo> combo) {
        ItemCombo item = (ItemCombo) combo.getSelectedItem();
        return item == null ? -1 : item.getId();
    }

    // Recarga las opciones de un combo conservando lo que estaba seleccionado
    public static void recargarCombo(JComboBox<ItemCombo> combo, DefaultComboBoxModel<ItemCombo> nuevoModelo) {
        int idAnterior = idSeleccionado(combo);
        combo.setModel(nuevoModelo);
        if (idAnterior != -1) {
            seleccionarPorId(combo, idAnterior);
        }
    }

    // =====================================================================
    // MENSAJES Y VALIDACIONES
    // =====================================================================

    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    public static void exito(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Listo", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Mensaje cuando una eliminación está bloqueada por registros relacionados
    public static void bloqueado(Component padre, String motivo) {
        JOptionPane.showMessageDialog(padre,
                "No se puede eliminar este registro.\n\n" + motivo,
                "Eliminación bloqueada", JOptionPane.WARNING_MESSAGE);
    }

    // Pregunta Sí/No antes de eliminar
    public static boolean confirmar(Component padre, String pregunta) {
        int r = JOptionPane.showConfirmDialog(padre, pregunta, "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    // Convierte un texto a número decimal. Devuelve -1 si no es un número válido.
    public static double leerDecimal(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    // Convierte un texto a número entero. Devuelve -1 si no es válido.
    public static int leerEntero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    // Revisión simple de un correo (solo si lo escribieron)
    public static boolean emailValido(String email) {
        return email.isEmpty() || email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    // Convierte un número a dinero: 190000 -> "$ 190,000"
    public static String dinero(double valor) {
        return String.format(Locale.US, "$ %,.0f", valor);
    }
}
