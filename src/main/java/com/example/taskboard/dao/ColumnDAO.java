package com.example.taskboard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.taskboard.config.DatabaseConnection;
import com.example.taskboard.model.BoardColumn;
import com.example.taskboard.model.ColumnType;

public class ColumnDAO extends BaseDAO {

     // Cria uma nova coluna e retorna o objeto BoardColumn com o ID gerado
    public BoardColumn create(BoardColumn column) throws SQLException {
        // Validação básica de tipo e posição (pode ser mais robusta)
        if (column.getBoardId() <= 0 || column.getName() == null || column.getName().trim().isEmpty() || column.getType() == null || column.getPosition() < 0) {
            throw new IllegalArgumentException("Dados inválidos para criar coluna.");
        }

        String sql = "INSERT INTO board_column(board_id, name, position, type) VALUES(?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Adicionar validações de regras de negócio aqui ANTES de inserir
            // Ex: Verificar se já existe coluna INICIAL/FINAL/CANCELAMENTO para este board
            if (column.getType() == ColumnType.INICIAL || column.getType() == ColumnType.FINAL || column.getType() == ColumnType.CANCELAMENTO) {
                if (existsByType(conn, column.getBoardId(), column.getType())) {
                     throw new SQLException("Já existe uma coluna do tipo " + column.getType() + " para este board.");
                }
            }
             if (existsByPosition(conn, column.getBoardId(), column.getPosition())) {
                 throw new SQLException("Já existe uma coluna na posição " + column.getPosition() + " para este board.");
             }
              if (existsByName(conn, column.getBoardId(), column.getName())) {
                 throw new SQLException("Já existe uma coluna com o nome '" + column.getName() + "' para este board.");
             }


            pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setInt(1, column.getBoardId());
            pst.setString(2, column.getName());
            pst.setInt(3, column.getPosition());
            pst.setString(4, column.getType().name()); // Salva o ENUM como String
            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar coluna, nenhuma linha afetada.");
            }

            rs = pst.getGeneratedKeys();
            if (rs.next()) {
                column.setId(rs.getInt(1));
                return column;
            } else {
                throw new SQLException("Falha ao criar coluna, ID não obtido.");
            }
        } finally {
             // Não fechar a conexão se ela foi passada como parâmetro (para transações)
             close(null, pst, rs); // Só fecha statement e resultset
             if(conn != null && !conn.isClosed()) { // Fecha a conexão se este método a abriu
                // Idealmente, a transação de criar board + colunas gerenciaria a conexão
                close(conn, null, null);
             }
        }
    }

    // Busca todas as colunas de um board específico, ordenadas pela posição
    public List<BoardColumn> findByBoardId(int boardId) throws SQLException {
        String sql = "SELECT id, board_id, name, position, type FROM board_column WHERE board_id = ? ORDER BY position";
        List<BoardColumn> columns = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, boardId);
            rs = pst.executeQuery();
            while (rs.next()) {
                columns.add(mapResultSetToColumn(rs));
            }
        } finally {
            close(conn, pst, rs);
        }
        return columns;
    }

     // Busca uma coluna pelo ID
    public Optional<BoardColumn> findById(int id) throws SQLException {
        String sql = "SELECT id, board_id, name, position, type FROM board_column WHERE id = ?";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, id);
            rs = pst.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToColumn(rs));
            }
        } finally {
            close(conn, pst, rs);
        }
        return Optional.empty();
    }

     // Busca a coluna de um tipo específico para um board
    public Optional<BoardColumn> findByType(int boardId, ColumnType type) throws SQLException {
         String sql = "SELECT id, board_id, name, position, type FROM board_column WHERE board_id = ? AND type = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, boardId);
            pst.setString(2, type.name());
            rs = pst.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToColumn(rs));
            }
        } finally {
            close(conn, pst, rs);
        }
        return Optional.empty();
    }

      // Busca a próxima coluna na ordem para um determinado board e posição atual
    public Optional<BoardColumn> findNextColumn(int boardId, int currentPosition) throws SQLException {
        String sql = "SELECT id, board_id, name, position, type FROM board_column " +
                     "WHERE board_id = ? AND position > ? AND type != 'CANCELAMENTO' " + // Não move para cancelamento automaticamente
                     "ORDER BY position ASC LIMIT 1";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, boardId);
            pst.setInt(2, currentPosition);
            rs = pst.executeQuery();
            if (rs.next()) {
                 // Verifica se a próxima coluna encontrada é a final, mesmo que haja outras depois (como a de cancelamento)
                 // A regra diz que a final é a penúltima lógica.
                 BoardColumn nextCol = mapResultSetToColumn(rs);
                 // Se houver colunas PENDENTE depois da FINAL (o que não deveria ocorrer pela regra),
                 // esta lógica pode precisar de ajuste. A validação na criação é crucial.
                 return Optional.of(nextCol);

                 // Lógica alternativa mais segura se a ordem não for garantida:
                 // Pegar todas > currentPosition, order by position, e verificar os tipos
                 // List<BoardColumn> potentialNext = new ArrayList<>();
                 // while(rs.next()) { potentialNext.add(mapResultSetToColumn(rs)); }
                 // if (potentialNext.isEmpty()) return Optional.empty();
                 // // A próxima real é a primeira não-CANCELAMENTO
                 // for (BoardColumn col : potentialNext) {
                 //    if (col.getType() != ColumnType.CANCELAMENTO) return Optional.of(col);
                 // }
                 // return Optional.empty(); // Nenhuma coluna válida encontrada

            }
        } finally {
            close(conn, pst, rs);
        }
        return Optional.empty();
    }


    // Mapeia um ResultSet para um objeto BoardColumn
    private BoardColumn mapResultSetToColumn(ResultSet rs) throws SQLException {
        return new BoardColumn(
                rs.getInt("id"),
                rs.getInt("board_id"),
                rs.getString("name"),
                rs.getInt("position"),
                ColumnType.valueOf(rs.getString("type")) // Converte String de volta para Enum
        );
    }

    // --- Métodos auxiliares para validação (usados internamente ou pelo App) ---

     // Verifica se já existe coluna de um tipo específico (usando conexão existente)
    private boolean existsByType(Connection conn, int boardId, ColumnType type) throws SQLException {
        String sql = "SELECT 1 FROM board_column WHERE board_id = ? AND type = ? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, boardId);
            pst.setString(2, type.name());
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Verifica se já existe coluna em uma posição específica (usando conexão existente)
    private boolean existsByPosition(Connection conn, int boardId, int position) throws SQLException {
        String sql = "SELECT 1 FROM board_column WHERE board_id = ? AND position = ? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, boardId);
            pst.setInt(2, position);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

     // Verifica se já existe coluna com um nome específico (usando conexão existente)
    private boolean existsByName(Connection conn, int boardId, String name) throws SQLException {
        String sql = "SELECT 1 FROM board_column WHERE board_id = ? AND name = ? LIMIT 1";
         try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, boardId);
            pst.setString(2, name);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

     // Conta quantas colunas existem para um board
     public int countColumns(int boardId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM board_column WHERE board_id = ?";
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, boardId);
            rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            close(conn, pst, rs);
        }
    }
}
