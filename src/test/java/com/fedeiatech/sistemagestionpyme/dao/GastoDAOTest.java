package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Gasto;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GastoDAOTest {

    private final GastoDAO gastoDAO = new GastoDAO();

    @BeforeEach
    void setUp(@TempDir File tempDir) throws SQLException {
        System.setProperty("db.path", new File(tempDir, "test.db").getAbsolutePath());
        ConexionDB.resetParaTests();
        SessionService.getInstance().iniciarSesion(new Usuario("admin-test", "hash", Usuario.Rol.ADMIN));
    }

    @AfterEach
    void tearDown() throws SQLException {
        ConexionDB.resetParaTests();
        System.clearProperty("db.path");
        SessionService.getInstance().cerrarSesion();
    }

    private Gasto nuevoGasto(String concepto, double monto, String categoria, String fecha) {
        Gasto g = new Gasto();
        g.setConcepto(concepto);
        g.setMonto(monto);
        g.setCategoria(categoria);
        g.setFecha(fecha);
        g.setUsuario("admin-test");
        return g;
    }

    @Test
    void registrarPersisteYAsignaId() throws SQLException {
        Gasto gasto = nuevoGasto("Nafta", 5000.0, "Combustible", "2026-08-01");

        gastoDAO.registrar(gasto);

        assertTrue(gasto.getId() > 0);
        List<Gasto> todos = gastoDAO.listarTodos();
        assertEquals(1, todos.size());
        assertEquals("Nafta", todos.get(0).getConcepto());
        assertEquals(5000.0, todos.get(0).getMonto());
        assertEquals("Combustible", todos.get(0).getCategoria());
        assertEquals("admin-test", todos.get(0).getUsuario());
    }

    @Test
    void listarTodosDevuelveOrdenadoPorFechaDesc() throws SQLException {
        gastoDAO.registrar(nuevoGasto("Gasto viejo", 100.0, null, "2026-07-01"));
        gastoDAO.registrar(nuevoGasto("Gasto nuevo", 200.0, null, "2026-08-01"));

        List<Gasto> todos = gastoDAO.listarTodos();

        assertEquals(2, todos.size());
        assertEquals("Gasto nuevo", todos.get(0).getConcepto());
        assertEquals("Gasto viejo", todos.get(1).getConcepto());
    }

    @Test
    void eliminarBorraElGasto() throws SQLException {
        Gasto gasto = nuevoGasto("Imprevisto", 300.0, null, "2026-08-01");
        gastoDAO.registrar(gasto);

        gastoDAO.eliminar(gasto.getId());

        assertEquals(0, gastoDAO.listarTodos().size());
    }

    @Test
    void sumarGastosEntreDevuelveSoloElRango() throws SQLException {
        gastoDAO.registrar(nuevoGasto("Fuera de rango antes", 100.0, null, "2026-06-15"));
        gastoDAO.registrar(nuevoGasto("Dentro de rango", 200.0, null, "2026-07-15"));
        gastoDAO.registrar(nuevoGasto("Fuera de rango después", 300.0, null, "2026-08-01"));

        double total = gastoDAO.sumarGastosEntre("2026-07-01", "2026-07-31");

        assertEquals(200.0, total);
    }

    @Test
    void sumarGastosEntreIncluyeLosBordesDelRango() throws SQLException {
        gastoDAO.registrar(nuevoGasto("En el límite desde", 50.0, null, "2026-07-01"));
        gastoDAO.registrar(nuevoGasto("En el límite hasta", 75.0, null, "2026-07-31"));

        double total = gastoDAO.sumarGastosEntre("2026-07-01", "2026-07-31");

        assertEquals(125.0, total);
    }

    @Test
    void sumarGastosEntreConRangoVacioDevuelveCero() throws SQLException {
        gastoDAO.registrar(nuevoGasto("Fuera de rango", 500.0, null, "2026-08-01"));

        double total = gastoDAO.sumarGastosEntre("2026-01-01", "2026-01-31");

        assertEquals(0.0, total);
    }

    @Test
    void listarEntreDevuelveSoloElRango() throws SQLException {
        gastoDAO.registrar(nuevoGasto("Fuera", 100.0, null, "2026-07-01"));
        gastoDAO.registrar(nuevoGasto("Dentro", 200.0, null, "2026-07-15"));

        List<Gasto> lista = gastoDAO.listarEntre("2026-07-10", "2026-07-20");

        assertEquals(1, lista.size());
        assertEquals("Dentro", lista.get(0).getConcepto());
    }

    @Test
    void registrarYEliminarRechazanSiElUsuarioActivoNoEsAdmin() throws SQLException {
        Gasto gastoDeAdmin = nuevoGasto("Gasto de admin", 100.0, null, "2026-08-01");
        gastoDAO.registrar(gastoDeAdmin);

        SessionService.getInstance().iniciarSesion(new Usuario("cajero-test", "hash", Usuario.Rol.CAJERO));
        try {
            Gasto gastoDeCajero = nuevoGasto("Intento no autorizado", 500.0, null, "2026-08-02");
            gastoDAO.registrar(gastoDeCajero);
            assertEquals(0, gastoDeCajero.getId(),
                    "Un CAJERO no debe poder registrar un gasto, aunque invoque el DAO directamente");

            gastoDAO.eliminar(gastoDeAdmin.getId());
        } finally {
            SessionService.getInstance().iniciarSesion(new Usuario("admin-test", "hash", Usuario.Rol.ADMIN));
        }

        List<Gasto> todos = gastoDAO.listarTodos();
        assertEquals(1, todos.size(), "El gasto de admin no debe haberse borrado por un intento de CAJERO");
        assertEquals("Gasto de admin", todos.get(0).getConcepto());
    }
}
