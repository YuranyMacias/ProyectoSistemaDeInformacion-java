package pruebamysql;

/**
 * Consultas rápidas para las tarjetas del Panel de Control (solo lectura).
 */
public class crudResumen {

    // Cantidad de productos registrados
    public int totalProductos() {
        return conectionDB.contar("select count(*) from productos");
    }

    // Suma de todas las unidades que hay en inventario
    public int totalUnidades() {
        return conectionDB.contar("select ifnull(sum(cantidad_actual),0) from inventario");
    }

    // Dinero vendido hoy (sin contar ventas canceladas)
    public double ventasDeHoy() {
        String query = "select ifnull(sum(d.total_pagar),0) from detalle_ventas d "
                + "join ventas v on v.id_venta = d.id_venta "
                + "where date(v.fecha_venta) = curdate() and v.estado <> 'Cancelada'";
        return conectionDB.valorDecimal(query);
    }

    // Cantidad de prendas agotadas o con 5 unidades o menos
    public int variantesStockBajo() {
        return conectionDB.contar("select count(*) from inventario where cantidad_actual <= 5");
    }
}
