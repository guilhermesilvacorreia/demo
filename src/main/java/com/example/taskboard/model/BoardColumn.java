package com.example.taskboard.model;

import java.util.List;
import java.util.Objects;

 
import com.example.taskboard.model.Card;

public class BoardColumn {
    private int id;
    private int boardId;
    private String name;
    private int position;
    private ColumnType type;
    private List<Card> cards; // Pode ser carregado sob demanda

    public BoardColumn(int id, int boardId, String name, int position, ColumnType type) {
        this.id = id;
        this.boardId = boardId;
        this.name = name;
        this.position = position;
        this.type = type;
    }

    public BoardColumn(int boardId, String name, int position, ColumnType type) {
        this.boardId = boardId;
        this.name = name;
        this.position = position;
        this.type = type;
    }

    public BoardColumn() { }

    // Getters
    public int getId() { return id; }
    public int getBoardId() { return boardId; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public ColumnType getType() { return type; }
    public List<Card> getCards() { return cards; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setBoardId(int boardId) { this.boardId = boardId; }
    public void setName(String name) { this.name = name; }
    public void setPosition(int position) { this.position = position; }
    public void setType(ColumnType type) { this.type = type; }
    public void setCards(List<Card> cards) { this.cards = cards; }

    @Override
    public String toString() {
        return "Coluna [ID=" + id + ", Nome='" + name + "', Posição=" + position + ", Tipo=" + type + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardColumn that = (BoardColumn) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
