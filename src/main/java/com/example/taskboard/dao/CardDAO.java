package com.example.taskboard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.taskboard.config.DatabaseConnection;
import com.example.taskboard.model.Card;

public class CardDAO extends BaseDAO {

    // Cria um novo card e retorna o objeto Card com ID e data de criação
    public Card create(Card card) throws SQLException {
         if (card.getColumnId() <= 0 || card.getTitle() == null || card.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Dados inválidos para criar card.");
        }
        // Define createdAt se ainda não estiver definido
        if (card.getCreatedAt() == null) {
            card.setCreatedAt(LocalDateTime.now());
        }

        String sql = "INSERT INTO card(column_id, title, description, created_at, blocked) VALUES(?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setInt(1, card.getColumnId());
            pst.setString(2, card.getTitle());
            pst.setString(3, card.getDescription());
            pst.setTimestamp(4, Timestamp.valueOf(card.getCreatedAt()));
            pst.setBoolean(5, card.isBlocked());

            int affectedRows = pst.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar card, nenhuma linha afetada.");
            }

            rs = pst.getGeneratedKeys();
            if (rs.next()) {
                card.setId(rs.getInt(1));
                // Buscar o card recém-criado para garantir que temos todos os dados (como created_at default do DB)
                return findById(card.getId()).orElseThrow(() -> new SQLException("Falha ao buscar card recém-criado."));
            } else {
                throw new SQLException("Falha ao criar card, ID não obtido.");
            }
        } finally {
            close(conn, pst, rs);
        }
    }

    // Busca um card pelo ID
    public Optional<Card> findById(int id) throws SQLException {
        String sql = "SELECT id, column_id, title, description, created_at, blocked, last_moved_at FROM card WHERE id = ?";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, id);
            rs = pst.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToCard(rs));
            }
        } finally {
            close(conn, pst, rs);
        }
        return Optional.empty();
    }

    // Busca todos os cards de uma coluna específica
    public List<Card> findByColumnId(int columnId) throws SQLException {
        String sql = "SELECT id, column_id, title, description, created_at, blocked, last_moved_at FROM card WHERE column_id = ? ORDER BY created_at";
        List<Card> cards = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, columnId);
            rs = pst.executeQuery();
            while (rs.next()) {
                cards.add(mapResultSetToCard(rs));
            }
        } finally {
            close(conn, pst, rs);
        }
        return cards;
    }

     // Atualiza a coluna de um card e a data da última movimentação
    public boolean updateColumn(int cardId, int newColumnId) throws SQLException {
        String sql = "UPDATE card SET column_id = ?, last_moved_at = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = DatabaseConnection.getConnection();
             // Iniciar transação se necessário (movimentação + histórico)
            // conn.setAutoCommit(false);

            pst = conn.prepareStatement(sql);
            pst.setInt(1, newColumnId);
            pst.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pst.setInt(3, cardId);
            int affectedRows = pst.executeUpdate();

             // Confirmar transação se necessário
             // conn.commit();
            return affectedRows > 0;
        } catch (SQLException e){
             // Rollback em caso de erro
             // if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
             throw e; // Re-lança a exceção
        } finally {
             // Restaurar autoCommit se foi alterado
             // if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            close(conn, pst);
        }
    }

    // Atualiza o status de bloqueio de um card
    public boolean updateBlockedStatus(int cardId, boolean isBlocked) throws SQLException {
        String sql = "UPDATE card SET blocked = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setBoolean(1, isBlocked);
            pst.setInt(2, cardId);
            int affectedRows = pst.executeUpdate();
            return affectedRows > 0;
        } finally {
            close(conn, pst);
        }
    }

    // Exclui um card pelo ID
     public boolean delete(int id) throws SQLException {
        // Considerar excluir histórico associado ou tratar com ON DELETE CASCADE/SET NULL
        String sql = "DELETE FROM card WHERE id = ?";
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


    // Mapeia um ResultSet para um objeto Card
    private Card mapResultSetToCard(ResultSet rs) throws SQLException {
         Timestamp lastMovedTimestamp = rs.getTimestamp("last_moved_at");
         LocalDateTime lastMovedAt = (lastMovedTimestamp != null) ? lastMovedTimestamp.toLocalDateTime() : null;

          Timestamp createdTimestamp = rs.getTimestamp("created_at");
         LocalDateTime createdAt = (createdTimestamp != null) ? createdTimestamp.toLocalDateTime() : LocalDateTime.now(); // Fallback


        return new Card(
                rs.getInt("id"),
                rs.getInt("column_id"),
                rs.getString("title"),
                rs.getString("description"),
                createdAt,
                rs.getBoolean("blocked"),
                lastMovedAt
        );
    }
}