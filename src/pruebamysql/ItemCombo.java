package pruebamysql;

/**
 * Elemento para los JComboBox: muestra un texto al usuario
 * pero guarda el id real de la base de datos.
 */
public class ItemCombo {

    private final int id;
    private final String texto;

    public ItemCombo(int id, String texto) {
        this.id = id;
        this.texto = texto;
    }

    public int getId() {
        return id;
    }

    // Lo que ve el usuario en la lista desplegable
    @Override
    public String toString() {
        return texto;
    }
}
