#  Sistema de Información y Gestión de Ventas - Tienda Estilo Prisma

> Aplicación de escritorio desarrollada en **Java Swing**  y persistencia en **MySQL** para la gestión
> integral de inventario, compras, ventas y control de acceso por roles.

---

##  Descripción General

Este proyecto consiste en un sistema de información diseñado para optimizar los procesos operativos de la tienda de ropa
**Estilo Prisma**. El sistema permite administrar de forma centralizada el catálogo de productos con sus variaciones, 
gestionar proveedores, registrar órdenes de compra, procesar ventas mediante facturación e
implementar control de acceso según el rol del usuario (Administrador y Vendedor).

---

##  Funcionalidades Principales

*  **Autenticación y Control de Acceso (RBAC):**
  * Inicio de sesión seguro con diferenciación de roles (*Administrador* y *Vendedor*).
  * Vistas y permisos adaptados según el nivel de acceso del usuario.
*  **Gestión de Inventario y Productos (CRUD):**
  * Alta, baja, modificación y consulta de prendas de vestir.
  * Control de stock, categorías y variantes de productos.
*  **Gestión de Compras y Proveedores:**
  * Registro de proveedores y seguimiento de órdenes de compra para reabastecimiento.
*  **Módulo de Facturación y Ventas:**
  * Registro de ventas en tiempo real con cálculo automático de totales.

---

## Tecnologías Utilizadas

* **Lenguaje de Programación:** Java (JDK 17)
* **Interfaz Gráfica (GUI):** Java Swing (JFrame)
* **Base de Datos:** MySQL
* **Conector de BD:** JDBC 
* **Entorno de Desarrollo (IDE):** NetBeans 
* **Control de Versiones:** Git & GitHub

---

##  Arquitectura del Proyecto

El proyecto está estructurado bajo el patrón de diseño **MVC (Modelo-Vista-Controlador)** 
para garantizar la separación de responsabilidades y la escalabilidad del código:

```text
src/
├── modelo/         # Clases de entidad (Producto, Venta, Usuario, Proveedor)
├── vista/          # Formularios e interfaces gráficas en Java Swing (JFrames)
├── controlador/    # Lógica de negocio y manejadores de eventos
└── conexion/       # Gestión de la conexión a MySQL vía JDBC

