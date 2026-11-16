# Urban Store – Sistema de inventario: análisis y mejoras

## 1. Cómo ponerlo a funcionar
1. Abre la carpeta `pruebaMysql` en Apache NetBeans 22 (File > Open Project).
2. El conector `mysql-connector-j` ya está referenciado en `nbproject/project.properties` con tu ruta de Windows. Si NetBeans marca la librería en rojo: clic derecho en el proyecto > Properties > Libraries > Add JAR/Folder y elige tu `.jar`.
3. (Recomendado) Ejecuta **una vez** `mejoras_bd.sql` en MySQL Workbench sobre `sistema_inventario`. Haz antes un respaldo.
4. Ejecuta el proyecto (F6). La clase principal ahora abre el login.
   Usuario de prueba: `acardenas` / `acaedenasprueba` (administrador).

## 2. Qué encontré en el proyecto original
| Hallazgo | Riesgo |
|---|---|
| Ventas, Compras e Inventario eran solo maquetas (datos escritos a mano) | No se registraba nada |
| No se sumaba ni restaba stock | Inventario inexacto |
| Los formularios decían "guardado" aunque el INSERT fallara | Datos perdidos sin aviso |
| Eliminar no revisaba relaciones; `variantes` e `inventario` borraban en CASCADE | Borrado silencioso de datos |
| Cada consulta abría una conexión y nunca la cerraba | Fuga de conexiones |
| Contraseñas en texto plano | Seguridad |
| Faltaban botones Editar; el menú no abría Caja ni Inventario ni Compras | Navegación incompleta |
| `PruebaMysql.main` solo tenía pruebas comentadas | — |
| Cajero y fecha escritos a mano en la caja | Facturas incorrectas |
| Color varchar(10), talla varchar(4), sin UNIQUE en inventario | Datos que no caben / duplicados |

## 3. Reglas de negocio implementadas
**Inventario**
- Compra en estado `Recibido` → **suma** a `cantidad_actual`. `Pendiente`/`Cancelado` no tocan el stock.
- Venta mientras no esté `Cancelada` → **resta** a `cantidad_actual`. Si no alcanza el stock, no se guarda nada.
- Cambiar el estado, eliminar una compra o eliminar una venta **revierte** el stock automáticamente.
- Una compra no se puede cancelar/eliminar si esa mercancía ya se vendió (el stock quedaría negativo).
- Todo se hace en una transacción (todo o nada).

**Eliminaciones bloqueadas (muestran advertencia con el motivo)**
| Registro | Se bloquea si tiene |
|---|---|
| Categoría | productos |
| Producto | variantes |
| Variante | compras/ventas o unidades en stock |
| Inventario | compras/ventas o unidades en stock |
| Cliente | ventas |
| Proveedor | compras |
| Empleado | compras/ventas (y nadie se elimina a sí mismo) |
| Rol | empleados |

**Roles**
- Administrador: todo.
- Vendedor: Caja y ventas (guardar + factura), Compras (solo registrar), Inventario (solo consulta), Clientes (registrar y editar, no eliminar).
  Si tu idea del vendedor era distinta, se cambia en `MenuPrincipal.java` (bloque `if (Sesion.esAdmin())`).

## 4. Qué cambió en el código
- `conectionDB`: ahora tiene herramientas comunes (`ejecutar`, `consultarModelo`, `contar`…), cierra las conexiones y traduce los errores de MySQL a mensajes claros.
- Todas las clases `crud*` conservan los nombres `insertarDato / consultarDatos / actualizarDatos / eliminarDatos`, ahora devuelven `true/false` y las tablas devuelven `DefaultTableModel` no editable. Se añadió `motivoBloqueo(id)` para las eliminaciones.
- `crudCompras` y `crudVentas` guardan cabecera + ítems + stock en una transacción. Los detalles ya no se editan por separado (rompería el stock): se maneja la compra/venta completa.
- Clases nuevas: `Sesion` (quién inició sesión), `Seguridad` (SHA-256), `ItemCombo`, `LineaDetalle`, `crudResumen`.
- Vista nueva: `EstiloUI` (colores y componentes compartidos, mismo diseño oscuro original), `FormVariantes`, `FormFactura`. Se completaron todos los formularios con Guardar / Editar / Eliminar / Limpiar; al hacer clic en una fila se cargan sus datos.
- Menú: abre todos los módulos, panel con tarjetas (productos, unidades, ventas del día, stock bajo) y tabla de alertas.
- Login: usa contraseñas cifradas; las antiguas en texto plano se convierten solas en el primer ingreso.
- Al guardar una variante se crea su fila de inventario con 0 unidades.

## 5. Script `mejoras_bd.sql`
Amplía talla/color, limpia espacios, `UNIQUE` en inventario por variante, `CHECK` stock ≥ 0 y cantidades > 0, nombres de categoría y NIT únicos, cambia los CASCADE de `variantes_producto`/`inventario` a RESTRICT y cifra contraseñas.

## 6. Sugerencias para el futuro
1. Sacar usuario/clave de MySQL de `conectionDB.java` a un archivo `config.properties` y no usar `root`; crear un usuario propio con permisos solo sobre `sistema_inventario`.
2. Agregar a `ventas`/`compras` columnas de impuesto (IVA) y descuento, y un número de factura consecutivo propio.
3. Tabla `movimientos_inventario` (fecha, tipo, cantidad, usuario) para auditar cada cambio de stock.
4. Reportes (ventas por rango de fechas, productos más vendidos) exportables a PDF/Excel.
5. Borrado lógico (columna `activo`) en vez de borrar clientes, productos o empleados.
6. Un pool de conexiones (HikariCP) y pruebas unitarias con JUnit cuando el proyecto crezca.
7. Respaldo automático de la base de datos (`mysqldump` programado).

## 7. Rediseño de la interfaz (versión 2)
Solo cambió la parte visual; la base de datos y la lógica son las mismas.
- Tema claro y profesional: fondo gris suave, tarjetas blancas con esquinas redondeadas, color de marca vino, tipografía Segoe UI.
- Diseño dinámico: se eliminaron las posiciones fijas (`setBounds`). Todo usa layouts, así que formularios, tablas y tarjetas se ajustan al cambiar el tamaño de la ventana. Las ventanas tienen tamaño mínimo.
- Menú lateral blanco agrupado por secciones (Ventas, Inventario, Personas), tarjeta de usuario y panel de control con tarjetas de resumen.
- Login de dos paneles (marca + formulario).
- Todas las tablas: filas alternadas, **buscador** que filtra mientras escribes y orden al hacer clic en el encabezado.
- Caja y Compras: barra superior con los datos, carrito/ítems y historial en paneles con divisor arrastrable.
- Botones redondeados con efecto al pasar el mouse, campos con foco de color, listas desplegables y barras de desplazamiento delgadas, cuadros de mensaje en blanco.
- Todo el estilo vive en `EstiloUI.java`: cambiando allí los colores (`COLOR_ACENTO`, etc.) cambia toda la aplicación.

## 8. Verificación realizada
Se compiló todo el proyecto y se probó la lógica contra una base de datos con tus datos: 63 pruebas (suma/resta de stock, rollback, estados, bloqueos, duplicados, contraseñas, factura) y se renderizaron todas las pantallas para ambos roles. Se probó con MariaDB 10.11 y compilador Java 21 (el proyecto usa Java 22; no se usó ninguna función exclusiva de 22). No se pudo probar con tu MySQL 8.0.33 real ni con el conector 26.7.0, así que si algo falla en tu equipo, avísame el mensaje.
