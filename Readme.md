# JFX-Business-Engine - FedeiaTech

**Sistema de Gestión ERP Modular y Resiliente para Pymes.**
*Diseñado para la transición ARCA (ex-AFIP) y alta performance en escritorio.*

🚧 **Estado:** Desarrollo Activo (v0.2-alpha) | 🔒 **Repositorio Privado**

## 🚀 Características Clave
* **Arquitectura Hexagonal:** Núcleo de negocio desacoplado de la normativa fiscal.
* **Interfaz Moderna:** Dashboard estilo "Bento Grid" con JavaFX.
* **Motor Híbrido:** Lógica Java + Sidecar Python para conexión ARCA (en desarrollo).
* **Base de Datos Local:** SQLite para funcionamiento Offline-First.

## 🛠️ Stack Tecnológico
* **Lenguaje:** Java 21 LTS
* **UI:** JavaFX + FXML
* **Build:** Maven
* **DB:** SQLite (JDBC)
* **Fiscal:** PyAfipWs (Integración planificada)

## 📋 Módulos
- [x] **Gestión de Inventario (CRUD)**
- [x] **Dashboard Operativo**
- [ ] **Punto de Venta (POS)** *(En progreso)*
- [ ] **Conexión Fiscal ARCA** *(Planificado)*
- [ ] **Reportes y Métricas** *(Planificado)*

## ⚙️ Cómo ejecutar (Dev)
1.  Clonar repositorio.
2.  Ejecutar `mvn clean install`.
3.  Correr la clase `Launcher.java`.

---
© 2025 FedeiaTech - Todos los derechos reservados.