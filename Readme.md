# JFX-Business-Engine - FedeiaTech

**Sistema de Gestión ERP Modular y Resiliente para Pymes.**
*Diseñado para la transición ARCA (ex-AFIP) y alta performance en escritorio.*

**Estado:** MVP Estable (v0.5) | **Repositorio Público**


![Main](https://github.com/FedeiaTech/JFX-Business-Engine/blob/develop/img/01.jpg)


## Características Clave
* **Arquitectura Hexagonal:** Núcleo de negocio desacoplado de la normativa fiscal.
* **Interfaz Moderna:** Dashboard estilo "Bento Grid" con JavaFX.
* **Punto de Venta (POS):** Facturación rápida con validación de stock y atajos de teclado.
* **Motor de Tickets:** Generación de PDF (iText) con soporte para impresoras térmicas y guardado temporal/fijo.
* **Base de Datos Local:** SQLite para funcionamiento Offline-First robusto.

## Stack Tecnológico
* **Lenguaje:** Java 21 LTS
* **UI:** JavaFX + FXML
* **Build:** Maven
* **DB:** SQLite (JDBC)
* **Reportes:** iText PDF (OpenPDF)

## Estado de Módulos
- [x] **Gestión de Inventario (CRUD)**
- [x] **Dashboard Operativo**
- [x] **Punto de Venta (POS)**
- [x] **Motor de Tickets (PDF)**
- [x] **Configuración y Persistencia**
- [ ] **Métricas y Gráficos (Dashboard)** *(Próximamente)*
- [ ] **Gestión de Clientes** *(Próximamente)*
- [ ] **Importación Masiva (Excel)** *(Próximamente)*
- [ ] **Conexión Fiscal ARCA** *(Planificado)*

## Niveles de Licencia (Planificado)
El sistema implementa *Feature Flags* para gestionar versiones:
* **Community (Free):** POS, Inventario Ilimitado, Tickets PDF Temporales.
* **Professional (Paid):** Dashboard de Métricas, Importación Excel, Cuenta Corriente, Tickets Archivados, Conexión Fiscal.

## Cómo ejecutar (Dev)
1.  Clonar repositorio.
2.  Ejecutar `mvn clean install`.
3.  Correr la clase `Launcher.java`.

## Capturas del programa

![Venta](https://github.com/FedeiaTech/JFX-Business-Engine/blob/develop/img/02.jpg)

![Inventario](https://github.com/FedeiaTech/JFX-Business-Engine/blob/develop/img/03.jpg)
---
© 2026 FedeiaTech - Todos los derechos reservados.