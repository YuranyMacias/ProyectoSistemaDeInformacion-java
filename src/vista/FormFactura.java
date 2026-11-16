package vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;

/**
 * Ventana que muestra la factura de una venta y permite imprimirla.
 */
public class FormFactura extends JDialog {

    private final JTextArea txtFactura;

    public FormFactura(Frame padre, String textoFactura) {
        super(padre, "Factura de venta - Estilo Prisma", true);
        setSize(560, 680);
        setMinimumSize(new Dimension(500, 500));
        setLocationRelativeTo(padre);
        getContentPane().setBackground(EstiloUI.COLOR_FONDO);
        setLayout(new BorderLayout(0, 14));

        JLabel lblTitulo = new JLabel("Factura de venta");
        lblTitulo.setFont(EstiloUI.fuente(Font.BOLD, 22));
        lblTitulo.setForeground(EstiloUI.COLOR_TEXTO);
        lblTitulo.setBorder(new EmptyBorder(20, 24, 0, 24));
        add(lblTitulo, BorderLayout.NORTH);

        // Hoja de papel: letra monoespaciada para que las columnas queden alineadas
        txtFactura = new JTextArea(textoFactura);
        txtFactura.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtFactura.setEditable(false);
        txtFactura.setBackground(Color.WHITE);
        txtFactura.setForeground(EstiloUI.COLOR_TEXTO);
        txtFactura.setBorder(new EmptyBorder(20, 20, 20, 20));
        txtFactura.setCaretPosition(0);

        JScrollPane scroll = EstiloUI.conScroll(new JTable()); // solo para reutilizar el estilo del borde
        scroll.setViewportView(txtFactura);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(new EmptyBorder(0, 24, 0, 24));
        contenedor.add(scroll, BorderLayout.CENTER);
        add(contenedor, BorderLayout.CENTER);

        // Botones inferiores
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotones.setOpaque(false);
        panelBotones.setBorder(new EmptyBorder(0, 24, 18, 14));
        JButton btnCerrar = EstiloUI.boton("Cerrar", EstiloUI.COLOR_GRIS);
        JButton btnImprimir = EstiloUI.boton("Imprimir", EstiloUI.COLOR_ACENTO);
        btnCerrar.setPreferredSize(new Dimension(120, 40));
        btnImprimir.setPreferredSize(new Dimension(120, 40));
        btnImprimir.addActionListener(e -> imprimir());
        btnCerrar.addActionListener(e -> dispose());
        panelBotones.add(btnCerrar);
        panelBotones.add(btnImprimir);
        add(panelBotones, BorderLayout.SOUTH);
    }

    // Abre el cuadro de impresión del sistema
    private void imprimir() {
        try {
            txtFactura.print();
        } catch (PrinterException ex) {
            EstiloUI.error(this, "No se pudo imprimir: " + ex.getMessage());
        }
    }
}
