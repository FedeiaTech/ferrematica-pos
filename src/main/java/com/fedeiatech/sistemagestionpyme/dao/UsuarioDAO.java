package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.model.Usuario.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

public class UsuarioDAO {

    public void inicializarTabla() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS usuarios (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "nombre TEXT NOT NULL UNIQUE, " +
                     "password_hash TEXT NOT NULL, " +
                     "rol TEXT NOT NULL DEFAULT 'CAJERO')";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        crearAdminSiVacio();
    }

    private void crearAdminSiVacio() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM usuarios";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                guardar(new Usuario("admin", BCrypt.hashpw("admin", BCrypt.gensalt()), Rol.ADMIN));
            }
        }
    }

    public void guardar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, password_hash, rol) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getNombre());
            pstmt.setString(2, usuario.getPasswordHash());
            pstmt.setString(3, usuario.getRol().name());
            pstmt.executeUpdate();
        }
    }

    public void actualizar(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuarios SET nombre=?, password_hash=?, rol=? WHERE id=?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getNombre());
            pstmt.setString(2, usuario.getPasswordHash());
            pstmt.setString(3, usuario.getRol().name());
            pstmt.setInt(4, usuario.getId());
            pstmt.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id=?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public Usuario buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE nombre = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY rol, nombre";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Usuario autenticar(String nombre, String password) throws SQLException {
        Usuario usuario = buscarPorNombre(nombre);
        if (usuario == null) return null;
        if (!BCrypt.checkpw(password, usuario.getPasswordHash())) return null;
        return usuario;
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRol(Rol.valueOf(rs.getString("rol")));
        return u;
    }
}
