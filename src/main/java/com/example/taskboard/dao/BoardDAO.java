package com.example.taskboard.dao;

import com.example.taskboard.config.DatabaseConnection;
import com.example.taskboard.model.Board;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoardDAO extends BaseDAO {

    // Cria um novo board e retorna o objeto Board com o ID gerado
    public Board create(Board board) throws SQLException {
        String sql = "INSERT INTO board(name) VALUES(?)";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, board.getName());
            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar board, nenhuma linha afetada.");
            }

            rs = pst.getGeneratedKeys();
            if (rs.next()) {
                board.setId(rs.getInt(1));
                return board;
            } else {
                throw new SQLException("Falha ao criar board, ID não obtido.");
            }
        } finally {
            close(null, pst, rs); // Só fecha statement e resultset aqui
             // A conexão será fechada pelo método que chamou 'create' se ele abrir um try-with-resources
             // Ou fechar manualmente no finally do App.java, se necessário
             // Mas o ideal é que cada método DAO gerencie sua conexão
             close(conn, null, null); // Fechando a conexão aqui
        }
    }

    // Busca todos os boards
    public List<Board> findAll() throws SQLException {
        String sql = "SELECT id, name FROM board ORDER BY name";
        List<Board> boards = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            rs = pst.executeQuery();
            while (rs.next()) {
                boards.add(new Board(rs.getInt("id"), rs.getString("name")));
            }
        } finally {
            close(conn, pst, rs);
        }
        return boards;
    }

    // Busca um board pelo ID
    public Optional<Board> findById(int id) throws SQLException {
        String sql = "SELECT id, name FROM board WHERE id = ?";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, id);
            rs = pst.executeQuery();
            if (rs.next()) {
                return Optional.of(new Board(rs.getInt("id"), rs.getString("name")));
            }
        } finally {
            close(conn, pst, rs);
        }
        return Optional.empty();
    }

    // Exclui um board pelo ID (Cuidado: ON DELETE CASCADE excluirá colunas e cards)
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM board WHERE id = ?";
         Connection conn = null;
        PreparedStatement pst = null;
        try {
             conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, id);
            int affectedRows = pst.executeUpdate();
            return affectedRows > 0;
        } finally {
            close(conn, pst);
        }
    }

     // Verifica se um nome de board já existe
    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM board WHERE name = ?";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setString(1, name);
            rs = pst.executeQuery();
            return rs.next(); // Retorna true se encontrou alguma linha
        } finally {
            close(conn, pst, rs);
        }
    }
}