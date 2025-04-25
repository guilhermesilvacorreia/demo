package com.example.taskboard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.taskboard.config.DatabaseConnection;
import com.example.taskboard.model.CardMovementHistory;

public class CardMovementHistoryDAO extends BaseDAO {

    // Cria um novo registro de histórico de movimento
    public CardMovementHistory create(CardMovementHistory history) throws SQLException {
        if (history.getMovedAt() == null) {
            history.setMovedAt(LocalDateTime.now());
        }
        String sql = "INSERT INTO card_movement_history(card_id, from_column_id, to_column_id, moved_at) VALUES(?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setInt(1, history.getCardId());
            // Define explicitamente NULL se from_column_id for null
             if (history.getFromColumnId() == null) {
                pst.setNull(2, Types.INTEGER);
            } else {
                pst.setInt(2, history.getFromColumnId());
            }
            pst.setInt(3, history.getToColumnId());
            pst.setTimestamp(4, Timestamp.valueOf(history.getMovedAt()));

            int affectedRows = pst.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar histórico de movimento, nenhuma linha afetada.");
            }

            rs = pst.getGeneratedKeys();
            if (rs.next()) {
                history.setId(rs.getInt(1));
                return history;
            } else {
                throw new SQLException("Falha ao criar histórico de movimento, ID não obtido.");
            }
        } finally {
            close(conn, pst, rs);
        }
    }

    // Busca todo o histórico de movimento para um card específico, ordenado por data
    public List<CardMovementHistory> findByCardId(int cardId) throws SQLException {
        // Junta com board_column para pegar os nomes das colunas para o relatório
         String sql = "SELECT h.id, h.card_id, h.from_column_id, h.to_column_id, h.moved_at, " +
                     " c_from.name as from_column_name, c_to.name as to_column_name " +
                     " FROM card_movement_history h " +
                     " LEFT JOIN board_column c_from ON h.from_column_id = c_from.id " + // LEFT JOIN para caso from_column_id seja NULL
                     " JOIN board_column c_to ON h.to_column_id = c_to.id " +
                     " WHERE h.card_id = ? ORDER BY h.moved_at ASC"; // Ordena do mais antigo para o mais novo
        List<CardMovementHistory> historyList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, cardId);
            rs = pst.executeQuery();
            while (rs.next()) {
                historyList.add(mapResultSetToHistory(rs, true)); // Pede para mapear nomes
            }
        } finally {
            close(conn, pst, rs);
        }
        return historyList;
    }

     // Mapeia um ResultSet para um objeto CardMovementHistory
    private CardMovementHistory mapResultSetToHistory(ResultSet rs, boolean includeNames) throws SQLException {
        Integer fromColumnId = rs.getObject("from_column_id", Integer.class); // Pega como objeto para aceitar NULL
        Timestamp movedTimestamp = rs.getTimestamp("moved_at");
        LocalDateTime movedAt = (movedTimestamp != null) ? movedTimestamp.toLocalDateTime() : null;

        CardMovementHistory history = new CardMovementHistory(
                rs.getInt("id"),
                rs.getInt("card_id"),
                fromColumnId,
                rs.getInt("to_column_id"),
                movedAt
        );
         if (includeNames) {
             history.setFromColumnName(rs.getString("from_column_name"));
             history.setToColumnName(rs.getString("to_column_name"));
         }
        return history;
    }
}