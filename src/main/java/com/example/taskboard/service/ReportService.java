package com.example.taskboard.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.sql.SQLException;

import com.example.taskboard.dao.BlockEventDAO;
import com.example.taskboard.dao.CardDAO;
import com.example.taskboard.dao.CardMovementHistoryDAO;
import com.example.taskboard.dao.ColumnDAO;
import com.example.taskboard.model.BlockEvent;
import com.example.taskboard.model.BoardColumn;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardMovementHistory;
import com.example.taskboard.model.ColumnType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final CardDAO cardDAO;
    private final ColumnDAO columnDAO;
    private final CardMovementHistoryDAO movementHistoryDAO;
    private final BlockEventDAO blockEventDAO;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ReportService() {
        this.cardDAO = new CardDAO();
        this.columnDAO = new ColumnDAO();
        this.movementHistoryDAO = new CardMovementHistoryDAO();
        this.blockEventDAO = new BlockEventDAO();
    }

    // Generate completion time report
    public String generateCompletionTimeReport(int boardId) {
        StringBuilder report = new StringBuilder();
        report.append("--- Relatório de Tempo de Conclusão (Board ID: ").append(boardId).append(") ---\n\n");

        try {
            Optional<BoardColumn> finalColumnOpt = columnDAO.findByType(boardId, ColumnType.FINAL);
            if (finalColumnOpt.isEmpty()) {
                return "Erro: Coluna FINAL não encontrada para este board.\n";
            }
            BoardColumn finalColumn = finalColumnOpt.get();

            List<Card> completedCards = cardDAO.findByColumnId(finalColumn.getId());
            if (completedCards.isEmpty()) {
                report.append("Nenhum card concluído encontrado na coluna '").append(finalColumn.getName()).append("'.\n");
                return report.toString();
            }

            report.append("Cards Concluídos:\n");
            for (Card card : completedCards) {
                appendCardCompletionDetails(report, card, finalColumn);
            }

        } catch (SQLException e) {
            logger.error("Erro ao gerar relatório de tempo de conclusão", e);
            report.append("\nErro ao gerar relatório de tempo de conclusão: ").append(e.getMessage()).append("\n");
        }

        return report.toString();
    }

    // Generate block report
    public String generateBlockReport(int boardId) {
        StringBuilder report = new StringBuilder();
        report.append("--- Relatório de Bloqueios (Board ID: ").append(boardId).append(") ---\n\n");

        try {
            List<BoardColumn> columns = columnDAO.findByBoardId(boardId);
            boolean foundBlocks = false;

            for (BoardColumn column : columns) {
                List<Card> cards = cardDAO.findByColumnId(column.getId());
                for (Card card : cards) {
                    List<BlockEvent> blockHistory = blockEventDAO.findByCardId(card.getId());
                    if (!blockHistory.isEmpty()) {
                        foundBlocks = true;
                        appendCardBlockDetails(report, card, column, blockHistory);
                    }
                }
            }

            if (!foundBlocks) {
                report.append("Nenhum histórico de bloqueio encontrado para os cards deste board.\n");
            }

        } catch (SQLException e) {
            logger.error("Erro ao gerar relatório de bloqueios", e);
            report.append("\nErro ao gerar relatório de bloqueios: ").append(e.getMessage()).append("\n");
        }

        return report.toString();
    }

    // Append card completion details to the report
    private void appendCardCompletionDetails(StringBuilder report, Card card, BoardColumn finalColumn) throws SQLException {
        report.append("--------------------------------------------\n");
        report.append("Card ID: ").append(card.getId()).append(" | Título: ").append(card.getTitle()).append("\n");
        report.append("Criado em: ").append(formatDateTime(card.getCreatedAt())).append("\n");

        List<CardMovementHistory> history = movementHistoryDAO.findByCardId(card.getId());
        if (history.isEmpty()) {
            report.append("  Histórico de movimentação não encontrado.\n");
            return;
        }

        LocalDateTime lastMoveTime = card.getCreatedAt();
        LocalDateTime endTime = null;
        Duration totalDuration = Duration.ZERO;

        report.append("  Tempo em cada Coluna:\n");
        for (CardMovementHistory move : history) {
            String fromName = move.getFromColumnName() != null ? move.getFromColumnName() : "(Início)";
            LocalDateTime moveTime = move.getMovedAt();

            if (lastMoveTime != null && moveTime != null) {
                Duration timeInPreviousState = Duration.between(lastMoveTime, moveTime);
                report.append("    - Coluna '").append(fromName).append("': ")
                      .append(formatDuration(timeInPreviousState)).append("\n");
                totalDuration = totalDuration.plus(timeInPreviousState);
            }

            lastMoveTime = moveTime;

            if (move.getToColumnId() == finalColumn.getId()) {
                endTime = moveTime;
            }
        }

        if (endTime != null) {
            report.append("  Concluído em: ").append(formatDateTime(endTime)).append("\n");
            Duration timeToComplete = Duration.between(card.getCreatedAt(), endTime);
            report.append("  Tempo Total para Conclusão: ").append(formatDuration(timeToComplete)).append("\n");
        } else {
            report.append("  Card na coluna final, mas data de movimentação para ela não encontrada no histórico.\n");
        }
    }

    // Append card block details to the report
    private void appendCardBlockDetails(StringBuilder report, Card card, BoardColumn column, List<BlockEvent> blockHistory) {
        report.append("--------------------------------------------\n");
        report.append("Card ID: ").append(card.getId()).append(" | Título: ").append(card.getTitle())
              .append(" | Coluna Atual: '").append(column.getName()).append("'\n");
        report.append("  Histórico de Bloqueios/Desbloqueios:\n");

        LocalDateTime lastBlockTime = null;
        Duration totalBlockedTime = Duration.ZERO;

        for (BlockEvent event : blockHistory) {
            report.append("    - ").append(event.getEventType() == BlockEvent.EventType.BLOCK ? "BLOQUEIO" : "DESBLOQUEIO")
                  .append(" em: ").append(formatDateTime(event.getEventTimestamp()))
                  .append(" | Motivo: ").append(event.getReason()).append("\n");

            if (event.getEventType() == BlockEvent.EventType.BLOCK) {
                lastBlockTime = event.getEventTimestamp();
            } else if (event.getEventType() == BlockEvent.EventType.UNBLOCK && lastBlockTime != null) {
                Duration blockedDuration = Duration.between(lastBlockTime, event.getEventTimestamp());
                report.append("      * Tempo Bloqueado neste período: ").append(formatDuration(blockedDuration)).append("\n");
                totalBlockedTime = totalBlockedTime.plus(blockedDuration);
                lastBlockTime = null;
            }
        }

        if (lastBlockTime != null) {
            Duration currentBlockedDuration = Duration.between(lastBlockTime, LocalDateTime.now());
            report.append("    * Card atualmente BLOQUEADO há: ").append(formatDuration(currentBlockedDuration)).append("\n");
            totalBlockedTime = totalBlockedTime.plus(currentBlockedDuration);
        }

        if (!totalBlockedTime.isZero()) {
            report.append("  Tempo Total Bloqueado (aproximado): ").append(formatDuration(totalBlockedTime)).append("\n");
        }
    }

    // Format a LocalDateTime to a string
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(formatter) : "N/A";
    }

    // Format a Duration to a readable string
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return "0s";
        }

        long days = duration.toDaysPart();
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}