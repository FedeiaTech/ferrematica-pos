# JFX-Business-Engine — FedeiaTech

**Sistema de Gestión Comercial para PyMEs argentinas.**  
*Offline-First, orientado a comercios minoristas. Preparado para integración fiscal ARCA.*

**Estado:** v0.5.1 — Estable | **Licencia:** Propietaria

![Main](https://github.com/FedeiaTech/JFX-Business-Engine/blob/develop/img/01.jpg)

---

## Características

- **Punto de Venta (POS):** Facturación rápida con búsqueda por código/nombre, validación de stock en tiempo real y atajos de teclado.
- **Motor de Tickets PDF:** Generación de comprobantes con logo, datos de empresa y guardado temporal o permanente. Soporte para impresoras térmicas.
- **Gestión de Inventario:** Alta y baja de productos físicos y servicios con control de stock visual por colores.
- **Reportes:** Historial de ventas con opción de reimprimir cualquier ticket anterior.
- **Configuración de Empresa:** Nombre, CUIT, dirección, logo, mensaje de ticket, backup y restore de base de datos.
- **Base de Datos Local:** SQLite — funciona sin conexión a internet. Sin servidores externos.

---

## Stack Tecnológico

| Componente | Tecnología |
| --- | --- |
| Lenguaje | Java 21 LTS |
| UI | JavaFX 21 + FXML + JFoenix |
| Build | Maven 3.9+ |
| Base de datos | SQLite (xerial JDBC) |
| PDF | OpenPDF |

---

## Estado de Módulos

- [x] **Punto de Venta (POS)** — transacción atómica, stock, ticket PDF
- [x] **Motor de Tickets PDF** — logo, datos empresa, guardado permanente/temporal
- [x] **Dashboard Operativo** — ventas del día, accesos directos
- [x] **Configuración y Persistencia** — empresa, backup/restore
- [ ] **Edición de Productos** *(en desarrollo)*
- [ ] **Métricas y Gráficos Dashboard** *(próximamente)*
- [ ] **Importación Masiva Excel** *(próximamente)*
- [ ] **Gestión de Clientes y Cuenta Corriente** *(próximamente)*
- [ ] **Login y Roles de Usuario** *(próximamente)*
- [ ] **Conexión Fiscal ARCA** *(planificado post v1.0)*

---

## Niveles de Licencia

| Feature | Community (Free) | Professional (Paid) |
| --- | --- | --- |
| POS | ✓ | ✓ |
| Inventario ilimitado | ✓ | ✓ |
| Tickets PDF temporales | ✓ | ✓ |
| Historial de ventas | — | ✓ |
| Tickets archivados permanentes | — | ✓ |
| Dashboard métricas y gráficos | — | ✓ |
| Importación Excel | — | ✓ |
| Cuenta Corriente / Clientes | — | ✓ |
| Conexión Fiscal ARCA | — | ✓ |

---

## Cómo ejecutar (Desarrollo)

**Requisitos:** JDK 21 + Maven 3.9+

```bash
# Clonar y ejecutar
git clone https://github.com/FedeiaTech/JFX-Business-Engine.git
cd JFX-Business-Engine
mvn javafx:run
```

La base de datos `gestion_pyme.db` se crea automáticamente en la raíz del proyecto al primer arranque.

---

## Capturas

![Venta](https://github.com/FedeiaTech/JFX-Business-Engine/blob/develop/img/02.jpg)

![Inventario](https://github.com/FedeiaTech/JFX-Business-Engine/blob/develop/img/03.jpg)

---

## Changelog

### v0.5.1 — 2026-04-15

- **Fix:** `ConfiguracionDAO` — los campos `certificado_ruta` y `ruta_backup` se perdían en cada guardado de configuración. El UPDATE ahora incluye todos los campos del modelo y preserva los valores sin UI.

### v0.5 — 2026-04-13

- Finalización módulo POS y generación de Tickets PDF.
- MVP Estable inicial.

---

*© 2026 FedeiaTech — Todos los derechos reservados.*
