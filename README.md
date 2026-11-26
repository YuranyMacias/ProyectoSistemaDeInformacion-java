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
   Inicio de sesión seguro con diferenciación de roles (*Administrador* y *Vendedor*).
   Vistas y permisos adaptados según el nivel de acceso del usuario.
*  **Gestión de Inventario y Productos (CRUD):**
   Alta, baja, modificación y consulta de prendas de vestir.
   Control de stock, categorías y variantes de productos.
*  **Gestión de Compras y Proveedores:**
   Registro de proveedores y seguimiento de órdenes de compra para reabastecimiento.
*  **Módulo de Facturación y Ventas:**
   Registro de ventas en tiempo real con cálculo automático de totales.

---

## Tecnologías Utilizadas

* **Lenguaje de Programación:** Java (JDK 17)
* **Interfaz Gráfica (GUI):** Java Swing (JFrame)
* **Base de Datos:** MySQL
* **Conector de BD:** JDBC `mysql-connector-j`
* **Entorno de Desarrollo (IDE):** NetBeans 
* **Control de Versiones:** Git & GitHub

---

##  Arquitectura del Proyecto

El proyecto está organizado bajo la separación de interfaz gráfica (`vista`) y lógica con persistencia en el paquete principal (`pruebamysql`):

```text
src/
├── pruebamysql/             # Lógica de negocio, conexión JDBC y operaciones CRUD
│   ├── Pruebamysql.java     # Clase principal (Main) / Punto de entrada
│   ├── conectionDB.java     # Gestión de conexión a la base de datos MySQL
│   ├── Sesion.java          # Control de sesión de usuario en ejecución
│   ├── Seguridad.java       # Manejo de autenticación y permisos
│   ├── ItemCombo.java       # Helper para mapear objetos en JComboBox
│   ├── LineaDetalle.java    # Estructura de detalle para compras y ventas
│   └── crud*.java           # Módulos de persistencia (Clientes, Productos, Ventas, etc.)
└── vista/                   # Capa de presentación (Formularios Java Swing)
    ├── EstiloUI.java        # Configuración de estilos y diseño visual
    └── Form*.java           # Interfaces gráficas (FormProductos, FormFactura, etc.)

Instalación y Configuración
Requisitos Previos
Antes de ejecutar la aplicación, asegúrate de contar con las siguientes herramientas instaladas en tu equipo:
Java Development Kit (JDK): Versión 17 o superior.
Servidor de Base de Datos: MySQL Server (gestionado a través de MySQL Workbench, XAMPP o WampServer).
Entorno de Desarrollo (IDE): NetBeans IDE (versión recomendada) o Visual Studio Code con extensión para Java.
Controlador JDBC: Conector oficial mysql-connector-j.jar.

 Pasos para la Configuración y Ejecución
1. Clonar el repositorio
Abre la terminal o PowerShell y clona el proyecto en tu máquina local:

2. Configurar e Importar la Base de Datos
Inicia tu servicio de MySQL (por ejemplo, desde el panel de XAMPP o MySQL Workbench).
Abre tu gestor de MySQL y crea una nueva base de datos para el proyecto.
Importa el archivo con extensión .sql ubicado dentro del repositorio para generar la estructura de tablas y datos iniciales.

3. Configurar la Conexión JDBC
Abre el proyecto en tu IDE (NetBeans).
Dirígete a la clase conectionDB.java ubicada en la ruta src/pruebamysql/conectionDB.java.
Revisa y ajusta las credenciales de la cadena de conexión según los datos de tu servidor local:

```text
String url = "jdbc:mysql://localhost:3306/nombre_de_tu_bd";
String usuario = "tu_usuario";       // Por defecto suele ser "root"
String contrasena = "tu_contrasena"; // Tu contraseña local de MySQL

4. Cargar Librerías e Iniciar la Aplicación
En NetBeans, ve al panel de navegación del proyecto, haz clic derecho sobre la carpeta Libraries y selecciona Add JAR/Folder...
Añade el archivo mysql-connector-j.jar (si no aparece ya vinculado).
Ubica el archivo Pruebamysql.java dentro del paquete pruebamysql.
Haz clic derecho sobre Pruebamysql.java y selecciona Run File (o presiona Shift + F6) para lanzar la pantalla de inicio de sesión.
