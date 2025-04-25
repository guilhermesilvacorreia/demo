package com.example.taskboard.model;


import java.util.List;
import java.util.Objects;

public class Board {
    private int id;
    private String name;
    private List<BoardColumn> columns; // Pode ser carregado sob demanda

    public Board(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Board(String name) {
        this.name = name;
    }

    public Board() { }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public List<BoardColumn> getColumns() { return columns; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setColumns(List<BoardColumn> columns) { this.columns = columns; }

    @Override
    public String toString() {
        return "Board [ID=" + id + ", Nome='" + name + "']";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return id == board.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}