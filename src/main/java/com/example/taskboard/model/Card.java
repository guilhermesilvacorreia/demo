package com.example.taskboard.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Card {
    private int id;
    private int columnId;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private boolean blocked;
    private LocalDateTime lastMovedAt; // Opcional

    // Formatter para exibição
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Card(int id, int columnId, String title, String description, LocalDateTime createdAt, boolean blocked, LocalDateTime lastMovedAt) {
        this.id = id;
        this.columnId = columnId;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.blocked = blocked;
        this.lastMovedAt = lastMovedAt;
    }

    public Card(int columnId, String title, String description) {
        this.columnId = columnId;
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now(); // Define a data de criação automaticamente
        this.blocked = false;
    }

     public Card() { }

    // Getters
    public int getId() { return id; }
    public int getColumnId() { return columnId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isBlocked() { return blocked; }
    public LocalDateTime getLastMovedAt() { return lastMovedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setColumnId(int columnId) { this.columnId = columnId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void setLastMovedAt(LocalDateTime lastMovedAt) { this.lastMovedAt = lastMovedAt; }


    @Override
    public String toString() {
        return "Card [ID=" + id +
               ", Título='" + title + '\'' +
               ", Descrição='" + description + '\'' +
               ", Criado em=" + (createdAt != null ? createdAt.format(formatter) : "N/A") +
               ", Bloqueado=" + (blocked ? "Sim" : "Não") +
               ", ColunaID=" + columnId +
               "]";
    }

     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return id == card.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
