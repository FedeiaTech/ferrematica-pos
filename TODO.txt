# Hoja de Ruta - JFX Business Engine

## 🐛 Deuda Técnica y Mejoras Menores
- [ ] Corregir IDs discontinuos en SQLite al eliminar y crear nuevos items.
- [ ] Mover "Agregar Item" a una ventana modal (Stage) separada para limpiar la UI.
- [ ] Configuración: Permitir definir horarios de sesión/caja.

## 🚀 Rumbo a la Versión 1.0 (Comercial)

### 1️⃣ FASE 1: Dashboard y Métricas (El "Wow" Visual)
*Objetivo: Transformar la tabla de reportes en inteligencia de negocios.*
- [ ] Implementar `ReportesDAO` para consultas agregadas (SUM, COUNT, GROUP BY).
- [ ] Crear Tarjetas KPI: "Venta del Día", "Ganancia Estimada", "Stock Crítico".
- [ ] Integrar Gráficos JavaFX:
    - [ ] Barras: Ventas últimos 7 días.
    - [ ] Torta: Top 5 Productos más vendidos.

### 2️⃣ FASE 2: Gestión de Datos Masiva (Excel)
*Objetivo: Facilitar la migración y actualización de precios.*
- [ ] Integrar librería Apache POI.
- [ ] Botón "Importar Inventario": Carga masiva desde `.xlsx`.
- [ ] Botón "Exportar": Descarga de listado para auditoría o backup manual.

### 3️⃣ FASE 3: Clientes y Cuentas Corrientes (El "Fiado")
*Objetivo: Fidelización y gestión de deuda.*
- [ ] Módulo ABM Clientes (Nombre, DNI, Teléfono, Dirección).
- [ ] POS: Agregar método de pago "Cuenta Corriente".
- [ ] Gestión de Deuda: Registrar pagos parciales y ver saldo deudor.

### 4️⃣ FASE 4: Seguridad y Usuarios
*Objetivo: Control de empleados y roles.*
- [ ] Crear tabla `usuarios` (BCrypt para contraseñas).
- [ ] Pantalla de Login al inicio.
- [ ] Roles:
    - **Admin:** Acceso total (Costos, Eliminación, Configuración).
    - **Cajero:** Solo POS y Cierre de caja (Ciego).

---
### 🇦🇷 FASE 5: Módulo Fiscal ARCA (Post-v1.0)
*Objetivo: Conectar con AFIP/ARCA mediante Sidecar Python.*
- [ ] Definir interfaz `IFiscalProvider` en Java.
- [ ] Desarrollar script Python (PyAfipWs) compilado a .exe.
- [ ] Integrar llamada asíncrona Java -> Python -> Java.