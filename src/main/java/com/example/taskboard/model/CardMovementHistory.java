package com.example.taskboard.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CardMovementHistory {
    private int id;
    private int cardId;
    private Integer fromColumnId; // Pode ser null
    private int toColumnId;
    private LocalDateTime movedAt;
    private String fromColumnName; // Campo adicional para relatórios
    private String toColumnName;   // Campo adicional para relatórios


    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Construtor completo
    public CardMovementHistory(int id, int cardId, Integer fromColumnId, int toColumnId, LocalDateTime movedAt) {
        this.id = id;
        this.cardId = cardId;
        this.fromColumnId = fromColumnId;
        this.toColumnId = toColumnId;
        this.movedAt = movedAt;
    }

    // Construtor para inserção
     public CardMovementHistory(int cardId, Integer fromColumnId, int toColumnId) {
        this.cardId = cardId;
        this.fromColumnId = fromColumnId;
        this.toColumnId = toColumnId;
        this.movedAt = LocalDateTime.now(); // Define automaticamente
    }

    // Getters
    public int getId() { return id; }
    public int getCardId() { return cardId; }
    public Integer getFromColumnId() { return fromColumnId; }
    public int getToColumnId() { return toColumnId; }
    public LocalDateTime getMovedAt() { return movedAt; }
    public String getFromColumnName() { return fromColumnName; }
    public String getToColumnName() { return toColumnName; }


    // Setters
    public void setId(int id) { this.id = id; }
    public void setCardId(int cardId) { this.cardId = cardId; }
    public void setFromColumnId(Integer fromColumnId) { this.fromColumnId = fromColumnId; }
    public void setToColumnId(int toColumnId) { this.toColumnId = toColumnId; }
    public void setMovedAt(LocalDateTime movedAt) { this.movedAt = movedAt; }
    public void setFromColumnName(String fromColumnName) { this.fromColumnName = fromColumnName; }
    public void setToColumnName(String toColumnName) { this.toColumnName = toColumnName; }


    @Override
    public String toString() {
         String fromStr = fromColumnName != null ? "'" + fromColumnName + "'" : (fromColumnId != null ? "ID:" + fromColumnId : "Início");
         String toStr = toColumnName != null ? "'" + toColumnName + "'" : "ID:" + toColumnId;

        return "Movimento [ID=" + id +
               ", CardID=" + cardId +
               ", De=" + fromStr +
               ", Para=" + toStr +
               ", Em=" + (movedAt != null ? movedAt.format(formatter) : "N/A") +
               "]";
    }
}