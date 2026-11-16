-- =====================================================================
--  URBAN STORE - MEJORAS A LA BASE DE DATOS  (sistema_inventario)
--  Ejecutar UNA SOLA VEZ sobre la base de datos que ya tienes.
--  MySQL Workbench: abrir este archivo -> botón del rayo (Execute).
--  Haz una copia (Server > Data Export) antes de ejecutarlo.
--  La aplicación también funciona sin este script, pero con él la base
--  de datos queda protegida aunque alguien la use fuera del programa.
-- =====================================================================

USE sistema_inventario;

-- 1. Columnas muy cortas: una talla como "XXL" cabe, pero un color como
--    "Verde Militar" (13 letras) no cabía en varchar(10).
ALTER TABLE variantes_producto
    MODIFY talla VARCHAR(10) DEFAULT NULL,
    MODIFY color VARCHAR(30) DEFAULT NULL;

-- 2. Quita espacios sobrantes (ej.: ' camisas' tenía un espacio al inicio)
UPDATE categorias SET nombre_categoria = TRIM(nombre_categoria);
UPDATE clientes   SET nombre = TRIM(nombre), direccion = TRIM(direccion);

-- 3. Cada variante tiene UNA sola fila de inventario (evita conteos duplicados)
ALTER TABLE inventario
    ADD UNIQUE KEY uq_inventario_variante (id_variante);

-- 4. El stock nunca puede quedar en negativo
ALTER TABLE inventario
    ADD CONSTRAINT chk_stock_no_negativo CHECK (cantidad_actual >= 0);

-- 5. Las cantidades de compras y ventas deben ser mayores que 0
ALTER TABLE detalle_compras
    ADD CONSTRAINT chk_detalle_compra_cantidad CHECK (cantidad > 0);
ALTER TABLE detalle_ventas
    ADD CONSTRAINT chk_detalle_venta_cantidad CHECK (cantidad > 0);

-- 6. Sin categorías ni proveedores repetidos
ALTER TABLE categorias  ADD UNIQUE KEY uq_nombre_categoria (nombre_categoria);
ALTER TABLE proveedores ADD UNIQUE KEY uq_nit_proveedor (nit);

-- 7. PROTECCIÓN DE ELIMINACIONES: antes, borrar un producto borraba en cascada
--    sus variantes y su inventario sin avisar. Ahora la base de datos RECHAZA
--    borrar un registro "padre" que tenga "hijos" (la aplicación ya muestra la
--    advertencia, esto es la segunda barrera de seguridad).
ALTER TABLE variantes_producto DROP FOREIGN KEY fk_variante_producto;
ALTER TABLE variantes_producto
    ADD CONSTRAINT fk_variante_producto FOREIGN KEY (id_producto)
    REFERENCES productos (id_producto) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inventario DROP FOREIGN KEY fk_inventario_variante;
ALTER TABLE inventario
    ADD CONSTRAINT fk_inventario_variante FOREIGN KEY (id_variante)
    REFERENCES variantes_producto (id_variante) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 8. Contraseñas cifradas (SHA-256) en lugar de texto plano.
--    (La aplicación también las cifra sola la primera vez que el usuario inicia sesión.)
UPDATE empleados SET contrasena = SHA2(contrasena, 256) WHERE CHAR_LENGTH(contrasena) <> 64;
