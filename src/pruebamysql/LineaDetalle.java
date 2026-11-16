package pruebamysql;

/**
 * Una línea (ítem) de una compra o de una venta:
 * qué prenda de inventario, cuántas unidades y a qué precio.
 */
public class LineaDetalle {

    private final int idInventario;
    private final String sku;
    private final String descripcion;
    private int cantidad;
    private final double precio;       // costo unitario (compras) o precio de venta (ventas)
    private int stockDisponible = 0;   // solo se llena cuando se busca por SKU

    public LineaDetalle(int idInventario, String sku, String descripcion, int cantidad, double precio) {
        this.idInventario = idInventario;
        this.sku = sku;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public int getIdInventario() { return idInventario; }
    public String getSku() { return sku; }
    public String getDescripcion() { return descripcion; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getPrecio() { return precio; }
    public int getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(int stock) { this.stockDisponible = stock; }

    // cantidad x precio
    public double getSubtotal() {
        return cantidad * precio;
    }
}
