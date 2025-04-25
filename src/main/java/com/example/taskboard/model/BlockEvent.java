package com.example.taskboard.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BlockEvent {
    private int id;
    private int cardId;
    private EventType eventType;
    private LocalDateTime eventTimestamp;
    private String reason;

    public enum EventType { BLOCK, UNBLOCK }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


    public BlockEvent(int id, int cardId, EventType eventType, LocalDateTime eventTimestamp, String reason) {
        this.id = id;
        this.cardId = cardId;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.reason = reason;
    }

     public BlockEvent(int cardId, EventType eventType, String reason) {
        this.cardId = cardId;
        this.eventType = eventType;
        this.reason = reason;
        this.eventTimestamp = LocalDateTime.now(); // Define automaticamente
    }

    // Getters
    public int getId() { return id; }
    public int getCardId() { return cardId; }
    public EventType getEventType() { return eventType; }
    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public String getReason() { return reason; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setCardId(int cardId) { this.cardId = cardId; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "EventoBloqueio [ID=" + id +
               ", CardID=" + cardId +
               ", Tipo=" + eventType +
               ", Timestamp=" + (eventTimestamp != null ? eventTimestamp.format(formatter) : "N/A") +
               ", Motivo='" + reason + '\'' +
               "]";
    }
}