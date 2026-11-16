package vista;

import pruebamysql.Seguridad;
import pruebamysql.Sesion;
import pruebamysql.conectionDB;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginTienda extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtContrasena;

    public LoginTienda() {
        setTitle("Sistema de Inventario - Estilo Prisma");
        setSize(900, 580);
        setMinimumSize(new Dimension(780, 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));

        // --- LADO IZQUIERDO: marca (degradado vino) ---
        JPanel panelMarca = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(150, 10, 45), getWidth(), getHeight(), new Color(64, 0, 20)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        JPanel textosMarca = new JPanel(new GridLayout(3, 1, 0, 6));
        textosMarca.setOpaque(false);
        JLabel lblMarca = new JLabel("ESTILO PRISMA");
        lblMarca.setForeground(Color.WHITE);
        lblMarca.setFont(EstiloUI.fuente(Font.BOLD, 38));
        JLabel lblLema = new JLabel("Gestión de inventario, compras y ventas");
        lblLema.setForeground(new Color(255, 255, 255, 210));
        lblLema.setFont(EstiloUI.fuente(Font.PLAIN, 15));
        JLabel lblLinea = new JLabel("Tienda de ropa");
        lblLinea.setForeground(new Color(255, 255, 255, 150));
        lblLinea.setFont(EstiloUI.fuente(Font.PLAIN, 13));
        textosMarca.add(lblMarca);
        textosMarca.add(lblLema);
        textosMarca.add(lblLinea);
        panelMarca.add(textosMarca);
        add(panelMarca);

        // --- LADO DERECHO: formulario blanco ---
        JPanel panelFondo = new JPanel(new GridBagLayout());
        panelFondo.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(340, 380));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        JLabel lblTitulo = new JLabel("Iniciar sesión");
        lblTitulo.setFont(EstiloUI.fuente(Font.BOLD, 28));
        lblTitulo.setForeground(EstiloUI.COLOR_TEXTO);
        c.gridy = 0;
        form.add(lblTitulo, c);

        c.gridy = 1;
        c.insets = new Insets(4, 0, 30, 0);
        form.add(EstiloUI.etiquetaPequena("Ingresa con tu usuario y contraseña"), c);

        c.gridy = 2;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(EstiloUI.etiquetaPequena("Usuario"), c);
        txtUsuario = EstiloUI.campo();
        c.gridy = 3;
        c.insets = new Insets(6, 0, 18, 0);
        form.add(txtUsuario, c);

        c.gridy = 4;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(EstiloUI.etiquetaPequena("Contraseña"), c);
        txtContrasena = EstiloUI.campoClave();
        c.gridy = 5;
        c.insets = new Insets(6, 0, 30, 0);
        form.add(txtContrasena, c);

        JButton btnIngresar = EstiloUI.boton("Ingresar", EstiloUI.COLOR_ACENTO);
        btnIngresar.setPreferredSize(new Dimension(100, 46));
        btnIngresar.setFont(EstiloUI.fuente(Font.BOLD, 14));
        c.gridy = 6;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(btnIngresar, c);

        // EVENTO DE LOGIN (con el botón o presionando Enter)
        btnIngresar.addActionListener(e -> validarIngreso());
        txtContrasena.addActionListener(e -> validarIngreso());
        txtUsuario.addActionListener(e -> txtContrasena.requestFocus());

        panelFondo.add(form);
        add(panelFondo);
    }

    private void validarIngreso() {
        String usuario = txtUsuario.getText().trim();
        String pass = new String(txtContrasena.getPassword());

        if (usuario.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa tu usuario y contraseña", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Se busca solo por usuario; la contraseña se compara aparte (cifrada)
        String sql = "SELECT id_empleado, nombre, id_roles, contrasena FROM empleados WHERE usuario = ?";
        try (Connection con = conectionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && contrasenaCorrecta(rs.getInt("id_empleado"), pass, rs.getString("contrasena"))) {
                int idEmpleado = rs.getInt("id_empleado");
                String nombreEmpleado = rs.getString("nombre");
                int idRol = rs.getInt("id_roles");

                // Guarda quién inició sesión para que lo use todo el sistema
                Sesion.iniciar(idEmpleado, nombreEmpleado, idRol);

                JOptionPane.showMessageDialog(this, "Bienvenido(a) " + nombreEmpleado);

                // Abre el menú principal pasando el nombre y rol
                MenuPrincipal menu = new MenuPrincipal(nombreEmpleado, idRol);
                menu.setVisible(true);

                // Cierra la ventana de login
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Credenciales incorrectas", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión: " + ex.getMessage());
        }
    }

    // Compara la contraseña escrita con la guardada.
    // Las contraseñas nuevas están cifradas (SHA-256). Si la guardada todavía está en texto plano
    // (usuarios antiguos) y coincide, se deja entrar y se cifra automáticamente para la próxima vez.
    private boolean contrasenaCorrecta(int idEmpleado, String escrita, String guardada) {
        String cifrada = Seguridad.sha256(escrita);
        if (cifrada.equals(guardada)) {
            return true;
        }
        if (escrita.equals(guardada)) {
            conectionDB.ejecutar("update empleados set contrasena = ? where id_empleado = ?", cifrada, idEmpleado);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        EstiloUI.instalar();
        SwingUtilities.invokeLater(() -> new LoginTienda().setVisible(true));
    }
}
