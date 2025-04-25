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

import com.example.taskboard.config.DatabaseConnection;
import com.example.taskboard.model.BlockEvent;

public class BlockEventDAO extends BaseDAO {

    // Cria um novo evento de bloqueio/desbloqueio
    public BlockEvent create(BlockEvent event) throws SQLException {
        if (event.getEventTimestamp() == null) {
            event.setEventTimestamp(LocalDateTime.now());
        }
        if (event.getReason() == null || event.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Motivo do evento de bloqueio/desbloqueio é obrigatório.");
        }

        String sql = "INSERT INTO block_event(card_id, event_type, event_timestamp, reason) VALUES(?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setInt(1, event.getCardId());
            pst.setString(2, event.getEventType().name());
            pst.setTimestamp(3, Timestamp.valueOf(event.getEventTimestamp()));
            pst.setString(4, event.getReason());

            int affectedRows = pst.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar evento de bloqueio, nenhuma linha afetada.");
            }

            rs = pst.getGeneratedKeys();
            if (rs.next()) {
                event.setId(rs.getInt(1));
                return event;
            } else {
                throw new SQLException("Falha ao criar evento de bloqueio, ID não obtido.");
            }
        } finally {
            close(conn, pst, rs);
        }
    }

    // Busca todos os eventos de bloqueio/desbloqueio para um card específico, ordenados por data
    public List<BlockEvent> findByCardId(int cardId) throws SQLException {
        String sql = "SELECT id, card_id, event_type, event_timestamp, reason " +
                     "FROM block_event WHERE card_id = ? ORDER BY event_timestamp ASC";
        List<BlockEvent> events = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, cardId);
            rs = pst.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToBlockEvent(rs));
            }
        } finally {
            close(conn, pst, rs);
        }
        return events;
    }

    // Mapeia um ResultSet para um objeto BlockEvent
    private BlockEvent mapResultSetToBlockEvent(ResultSet rs) throws SQLException {
        Timestamp eventTimestamp = rs.getTimestamp("event_timestamp");
        LocalDateTime timestamp = (eventTimestamp != null) ? eventTimestamp.toLocalDateTime() : null;

        return new BlockEvent(
                rs.getInt("id"),
                rs.getInt("card_id"),
                BlockEvent.EventType.valueOf(rs.getString("event_type")),
                timestamp,
                rs.getString("reason")
        );
    }
}