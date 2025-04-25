package com.example.taskboard;

import com.example.taskboard.dao.BlockEventDAO;
import com.example.taskboard.dao.BoardDAO;
import com.example.taskboard.dao.CardDAO;
import com.example.taskboard.dao.CardMovementHistoryDAO;
import com.example.taskboard.dao.ColumnDAO;
import com.example.taskboard.model.BlockEvent;
import com.example.taskboard.model.Board;
import com.example.taskboard.model.BoardColumn;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardMovementHistory;
import com.example.taskboard.model.ColumnType;
import com.example.taskboard.service.ReportService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;


public class App {
    private static final Scanner scanner = new Scanner(System.in);
    private static final BoardDAO boardDAO = new BoardDAO();
    private static final ColumnDAO columnDAO = new ColumnDAO();
    private static final CardDAO cardDAO = new CardDAO();
    private static final CardMovementHistoryDAO movementHistoryDAO = new CardMovementHistoryDAO(); // Opcional 1
    private static final BlockEventDAO blockEventDAO = new BlockEventDAO(); // Opcional 3 / Req 7
    private static final ReportService reportService = new ReportService(); // Opcional 2 e 3
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


    public static void main(String[] args) {
        System.out.println("Bem-vindo ao TaskBoard Customizável!");
        while (true) {
            exibirMenuPrincipal();
            String opcao = lerEntrada("Escolha uma opção: ");

            try {
                switch (opcao) {
                    case "1":
                        criarNovoBoard();
                        break;
                    case "2":
                        selecionarBoard();
                        break;
                    case "3":
                        excluirBoard();
                        break;
                    case "4":
                        System.out.println("Saindo...");
                        scanner.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Opção inválida. Tente novamente.");
                }
            } catch (SQLException e) {
                System.err.println("\nErro de Banco de Dados: " + e.getMessage());
                 e.printStackTrace(); // Ajuda a depurar
            } catch (NumberFormatException e) {
                 System.err.println("\nErro: Entrada inválida. Por favor, insira um número quando solicitado.");
            }
             catch (IllegalArgumentException e) {
                 System.err.println("\nErro: " + e.getMessage());
             }
            catch (Exception e) {
                System.err.println("\nOcorreu um erro inesperado: " + e.getMessage());
                e.printStackTrace();
            }
             aguardarEnter(); // Pausa para o usuário ler a mensagem
        }
    }

    private static void exibirMenuPrincipal() {
        System.out.println("\n--- MENU PRINCIPAL ---");
        System.out.println("1 - Criar novo board");
        System.out.println("2 - Selecionar board");
        System.out.println("3 - Excluir board");
        System.out.println("4 - Sair");
    }

     private static void criarNovoBoard() throws SQLException {
        System.out.println("\n--- CRIAR NOVO BOARD ---");
        String nomeBoard;
        while (true) {
            nomeBoard = lerEntrada("Nome do novo board: ");
            if (nomeBoard.trim().isEmpty()) {
                System.out.println("O nome do board não pode ser vazio.");
            } else if (boardDAO.existsByName(nomeBoard)) {
                 System.out.println("Já existe um board com este nome. Escolha outro.");
            }
            else {
                break;
            }
        }


        Board novoBoard = new Board(nomeBoard);
        Board boardCriado = boardDAO.create(novoBoard);
        int boardId = boardCriado.getId();
        System.out.println("Board '" + nomeBoard + "' criado com ID: " + boardId);

        // Criação das colunas obrigatórias e pendentes
         List<BoardColumn> colunasCriadas = criarColunasParaBoard(boardId);
         if (colunasCriadas.size() < 3) {
             // Se falhou em criar as colunas essenciais, deletar o board seria uma opção
             System.err.println("Erro crítico: Não foi possível criar as colunas essenciais. O board pode estar inconsistente.");
             // boardDAO.delete(boardId); // Rollback manual (idealmente seria uma transação)
             return;
         }

        System.out.println("Colunas criadas com sucesso para o board '" + nomeBoard + "'.");
    }

     private static List<BoardColumn> criarColunasParaBoard(int boardId) throws SQLException {
         List<BoardColumn> colunas = new ArrayList<>();
         int positionCounter = 0;

         // 1. Coluna Inicial (Obrigatória, Posição 0)
         String nomeInicial = lerEntradaComDefault("Nome da coluna INICIAL:", "A Fazer");
         BoardColumn colInicial = new BoardColumn(boardId, nomeInicial, positionCounter++, ColumnType.INICIAL);
         colunas.add(columnDAO.create(colInicial));
         System.out.println(" > Coluna '" + nomeInicial + "' (INICIAL) criada.");

         // 2. Colunas Pendentes (Opcional, Posições intermediárias)
         while (true) {
             String addPendente = lerEntrada("Adicionar coluna PENDENTE? (s/N): ").toLowerCase();
             if (!addPendente.equals("s")) {
                 break;
             }
             String nomePendente = lerEntrada("Nome da coluna PENDENTE:");
              if (nomePendente.trim().isEmpty() || colunas.stream().anyMatch(c -> c.getName().equalsIgnoreCase(nomePendente))) {
                   System.out.println("Nome inválido ou já existente. Tente novamente.");
                   continue;
              }
             BoardColumn colPendente = new BoardColumn(boardId, nomePendente, positionCounter++, ColumnType.PENDENTE);
             colunas.add(columnDAO.create(colPendente));
             System.out.println(" > Coluna '" + nomePendente + "' (PENDENTE) criada.");
         }

         // 3. Coluna Final (Obrigatória, Penúltima Posição)
         String nomeFinal = lerEntradaComDefault("Nome da coluna FINAL:", "Concluído");
         BoardColumn colFinal = new BoardColumn(boardId, nomeFinal, positionCounter++, ColumnType.FINAL);
          colunas.add(columnDAO.create(colFinal));
          System.out.println(" > Coluna '" + nomeFinal + "' (FINAL) criada.");


         // 4. Coluna Cancelamento (Obrigatória, Última Posição)
         String nomeCancel = lerEntradaComDefault("Nome da coluna de CANCELAMENTO:", "Cancelado");
          BoardColumn colCancel = new BoardColumn(boardId, nomeCancel, positionCounter++, ColumnType.CANCELAMENTO);
           colunas.add(columnDAO.create(colCancel));
           System.out.println(" > Coluna '" + nomeCancel + "' (CANCELAMENTO) criada.");

         // Validação final (mínimo 3 colunas)
         if (colunas.size() < 3) {
             throw new IllegalStateException("Um board deve ter pelo menos 3 colunas (Inicial, Final, Cancelamento).");
             // Idealmente, a transação seria revertida aqui.
         }

         return colunas;
     }


     private static void selecionarBoard() throws SQLException {
        System.out.println("\n--- SELECIONAR BOARD ---");
        List<Board> boards = boardDAO.findAll();
        if (boards.isEmpty()) {
            System.out.println("Nenhum board cadastrado.");
            return;
        }

        System.out.println("Boards disponíveis:");
        boards.forEach(b -> System.out.println("  ID: " + b.getId() + " | Nome: " + b.getName()));

        int boardId = lerInt("Digite o ID do board que deseja selecionar: ");

        Optional<Board> boardOpt = boards.stream().filter(b -> b.getId() == boardId).findFirst();
        // Optional<Board> boardOpt = boardDAO.findById(boardId); // Alternativa: buscar no DB novamente

        if (boardOpt.isPresent()) {
            gerenciarBoardSelecionado(boardOpt.get());
        } else {
            System.out.println("Board com ID " + boardId + " não encontrado.");
        }
    }

      private static void gerenciarBoardSelecionado(Board board) throws SQLException {
        System.out.println("\n--- GERENCIANDO BOARD: " + board.getName() + " (ID: " + board.getId() + ") ---");
        Board currentBoard = board; // Mantém a referência atualizada

        while (true) {
             // Recarrega as colunas e cards a cada iteração do menu para refletir mudanças
             List<BoardColumn> colunas = columnDAO.findByBoardId(currentBoard.getId());
             if (colunas.isEmpty()) {
                 System.err.println("Erro: Nenhuma coluna encontrada para este board. Board inconsistente?");
                 return; // Sai do gerenciamento
             }
             for(BoardColumn col : colunas) {
                 // Explicitly use your own Card class to avoid ambiguity
                 java.util.List<com.example.taskboard.model.Card> cards = cardDAO.findByColumnId(col.getId());
                 col.setCards(cards);
             }
             currentBoard.setColumns(colunas); // Atualiza o board com dados frescos

            exibirBoard(currentBoard);
            exibirMenuBoard();
            String opcao = lerEntrada("Escolha uma ação: ");

            switch (opcao) {
                case "1":
                    criarNovoCard(currentBoard);
                    break;
                case "2":
                    moverCard(currentBoard);
                    break;
                case "3":
                    cancelarCard(currentBoard);
                    break;
                case "4":
                    bloquearCard(currentBoard);
                    break;
                case "5":
                    desbloquearCard(currentBoard);
                    break;
                 case "6": // Relatório de Tempo (Opcional)
                     System.out.println("\n" + reportService.generateCompletionTimeReport(currentBoard.getId()));
                     aguardarEnter();
                    break;
                 case "7": // Relatório de Bloqueios (Opcional)
                     System.out.println("\n" + reportService.generateBlockReport(currentBoard.getId()));
                     aguardarEnter();
                     break;
                case "8":
                    System.out.println("Fechando board '" + currentBoard.getName() + "'.");
                    return; // Volta para o menu principal
                default:
                    System.out.println("Opção inválida.");
                    aguardarEnter();
            }
        }
    }

    // Exibe o estado atual do board no console
    private static void exibirBoard(Board board) {
        System.out.println("\n=== BOARD: " + board.getName() + " ===");
        if (board.getColumns() == null || board.getColumns().isEmpty()) {
            System.out.println(" (Board vazio ou sem colunas carregadas) ");
            return;
        }

        // Ordena as colunas pela posição para exibição correta
        List<BoardColumn> colunasOrdenadas = board.getColumns().stream()
            .sorted(Comparator.comparingInt(BoardColumn::getPosition))
            .collect(Collectors.toList());

        for (BoardColumn coluna : colunasOrdenadas) {
            System.out.println("\n--- Coluna: " + coluna.getName() + " (" + coluna.getType() + ") ---");
            List<Card> cards = coluna.getCards(); // Usa os cards já carregados
            if (cards == null || cards.isEmpty()) {
                System.out.println("   (Vazia)");
            } else {
                for (Card card : cards) {
                     String blockedStatus = card.isBlocked() ? " [BLOQUEADO]" : "";
                    System.out.println("   * Card ID: " + card.getId() + " | Título: " + card.getTitle() + blockedStatus);
                    // Opcional: Exibir mais detalhes
                     System.out.println("     Descrição: " + (card.getDescription() != null ? card.getDescription() : "(sem descrição)"));
                     System.out.println("     Criado em: " + (card.getCreatedAt() != null ? card.getCreatedAt().format(formatter) : "N/A"));
                }
            }
        }
        System.out.println("\n===============================");
    }

    private static void exibirMenuBoard() {
        System.out.println("\n--- AÇÕES DO BOARD ---");
        System.out.println("1 - Criar novo card");
        System.out.println("2 - Mover card para próxima coluna");
        System.out.println("3 - Cancelar card (mover para coluna de cancelamento)");
        System.out.println("4 - Bloquear card");
        System.out.println("5 - Desbloquear card");
        System.out.println("6 - Gerar Relatório de Tempo de Conclusão"); // Opcional
        System.out.println("7 - Gerar Relatório de Bloqueios"); // Opcional
        System.out.println("8 - Fechar board (Voltar ao menu principal)");
    }


    private static void criarNovoCard(Board board) throws SQLException {
        System.out.println("\n--- CRIAR NOVO CARD ---");
        Optional<BoardColumn> colunaInicialOpt = board.getColumns().stream()
                .filter(c -> c.getType() == ColumnType.INICIAL)
                .findFirst();

        if (colunaInicialOpt.isEmpty()) {
            System.err.println("Erro: Coluna inicial não encontrada neste board. Não é possível criar cards.");
            return;
        }
        BoardColumn colunaInicial = colunaInicialOpt.get();

        String titulo = lerEntrada("Título do card: ");
         if (titulo.trim().isEmpty()) {
             System.out.println("Título não pode ser vazio.");
             return;
         }
        String descricao = lerEntrada("Descrição do card (opcional): ");

        Card novoCard = new Card(colunaInicial.getId(), titulo, descricao);
        Card cardCriado = cardDAO.create(novoCard);

        // Opcional 1: Registrar movimento inicial no histórico
        CardMovementHistory initialMovement = new CardMovementHistory(cardCriado.getId(), null, colunaInicial.getId());
        movementHistoryDAO.create(initialMovement);


        System.out.println("Card '" + cardCriado.getTitle() + "' (ID: " + cardCriado.getId() + ") criado na coluna '" + colunaInicial.getName() + "'.");
         aguardarEnter();
    }

     private static void moverCard(Board board) throws SQLException {
        System.out.println("\n--- MOVER CARD ---");
        int cardId = lerInt("Digite o ID do card a ser movido: ");

        // 1. Buscar o card
        Optional<Card> cardOpt = findCardInBoard(board, cardId);
        if (cardOpt.isEmpty()) {
            System.out.println("Card com ID " + cardId + " não encontrado neste board.");
            return;
        }
        Card card = cardOpt.get();

        // 2. Verificar se está bloqueado
        if (card.isBlocked()) {
            System.out.println("Card ID " + cardId + " está bloqueado e não pode ser movido. Desbloqueie primeiro.");
            return;
        }

        // 3. Encontrar a coluna atual do card
         Optional<BoardColumn> currentColOpt = findColumnById(board, card.getColumnId());
         if (currentColOpt.isEmpty()) {
             System.err.println("Erro: Coluna atual (ID: " + card.getColumnId() + ") do card não encontrada no board.");
             return;
         }
         BoardColumn currentCol = currentColOpt.get();

        // 4. Verificar se já está na coluna FINAL (não pode mover adiante, exceto cancelar)
        if (currentCol.getType() == ColumnType.FINAL) {
            System.out.println("Card já está na coluna final ('" + currentCol.getName() + "'). Não pode ser movido adiante.");
             System.out.println("Se desejar, use a opção 'Cancelar card'.");
            return;
        }
         // 5. Verificar se está na coluna de CANCELAMENTO (não pode sair de lá)
         if (currentCol.getType() == ColumnType.CANCELAMENTO) {
            System.out.println("Card já está na coluna de cancelamento ('" + currentCol.getName() + "') e não pode ser movido.");
            return;
        }


        // 6. Encontrar a próxima coluna válida na sequência (não pode ser CANCELAMENTO aqui)
         Optional<BoardColumn> nextColOpt = findNextSequentialColumn(board, currentCol.getPosition());

        if (nextColOpt.isEmpty()) {
            System.out.println("Não há próxima coluna válida para mover o card (já está na última coluna antes da Final/Cancelamento?).");
            return;
        }
        BoardColumn nextCol = nextColOpt.get();


        // 7. Executar a movimentação
        boolean moved = cardDAO.updateColumn(card.getId(), nextCol.getId());
        if (moved) {
             // Opcional 1: Registrar no histórico
             CardMovementHistory movement = new CardMovementHistory(card.getId(), currentCol.getId(), nextCol.getId());
             movementHistoryDAO.create(movement);

            System.out.println("Card ID " + cardId + " movido de '" + currentCol.getName() + "' para '" + nextCol.getName() + "'.");
        } else {
            System.err.println("Falha ao atualizar a coluna do card no banco de dados.");
        }
        aguardarEnter();
    }

      private static void cancelarCard(Board board) throws SQLException {
        System.out.println("\n--- CANCELAR CARD ---");
        int cardId = lerInt("Digite o ID do card a ser cancelado: ");

        // 1. Buscar o card
        Optional<Card> cardOpt = findCardInBoard(board, cardId);
         if (cardOpt.isEmpty()) {
            System.out.println("Card com ID " + cardId + " não encontrado neste board.");
            return;
        }
        Card card = cardOpt.get();

        // 2. Encontrar a coluna atual do card
         Optional<BoardColumn> currentColOpt = findColumnById(board, card.getColumnId());
         if (currentColOpt.isEmpty()) {
             System.err.println("Erro: Coluna atual (ID: " + card.getColumnId() + ") do card não encontrada.");
             return;
         }
         BoardColumn currentCol = currentColOpt.get();

         // 3. Não pode cancelar se já estiver na coluna FINAL ou CANCELAMENTO
         if (currentCol.getType() == ColumnType.FINAL) {
            System.out.println("Card já está na coluna final. Não pode ser cancelado diretamente daqui.");
             // Poderia permitir mover para cancelado da final? A regra diz não.
            return;
        }
         if (currentCol.getType() == ColumnType.CANCELAMENTO) {
            System.out.println("Card já está cancelado.");
            return;
        }


        // 4. Encontrar a coluna de CANCELAMENTO
        Optional<BoardColumn> cancelColOpt = board.getColumns().stream()
                .filter(c -> c.getType() == ColumnType.CANCELAMENTO)
                .findFirst();

        if (cancelColOpt.isEmpty()) {
            System.err.println("Erro: Coluna de CANCELAMENTO não encontrada neste board.");
            return;
        }
        BoardColumn cancelCol = cancelColOpt.get();

        // 5. Executar a movimentação para Cancelamento
        boolean moved = cardDAO.updateColumn(card.getId(), cancelCol.getId());
        if (moved) {
             // Opcional 1: Registrar no histórico
             CardMovementHistory movement = new CardMovementHistory(card.getId(), currentCol.getId(), cancelCol.getId());
             movementHistoryDAO.create(movement);

            System.out.println("Card ID " + cardId + " movido de '" + currentCol.getName() + "' para '" + cancelCol.getName() + "' (Cancelado).");
        } else {
            System.err.println("Falha ao atualizar a coluna do card no banco de dados para cancelamento.");
        }
         aguardarEnter();
    }


     private static void bloquearCard(Board board) throws SQLException {
        System.out.println("\n--- BLOQUEAR CARD ---");
        int cardId = lerInt("Digite o ID do card a ser bloqueado: ");

        Optional<Card> cardOpt = findCardInBoard(board, cardId);
        if (cardOpt.isEmpty()) {
            System.out.println("Card com ID " + cardId + " não encontrado.");
            return;
        }
        Card card = cardOpt.get();

        if (card.isBlocked()) {
            System.out.println("Card ID " + cardId + " já está bloqueado.");
            return;
        }

        String motivo = lerEntrada("Digite o motivo do bloqueio: ");
         if (motivo.trim().isEmpty()) {
            System.out.println("Motivo do bloqueio é obrigatório.");
            return;
        }


        // Atualiza o status no card
        boolean updated = cardDAO.updateBlockedStatus(card.getId(), true);

        if (updated) {
             // Cria o evento de bloqueio (Req 7 e Opcional 3)
             BlockEvent blockEvent = new BlockEvent(card.getId(), BlockEvent.EventType.BLOCK, motivo);
             blockEventDAO.create(blockEvent);
            System.out.println("Card ID " + cardId + " bloqueado com sucesso.");
        } else {
            System.err.println("Falha ao atualizar o status de bloqueio do card.");
        }
        aguardarEnter();
    }

     private static void desbloquearCard(Board board) throws SQLException {
        System.out.println("\n--- DESBLOQUEAR CARD ---");
        int cardId = lerInt("Digite o ID do card a ser desbloqueado: ");

        Optional<Card> cardOpt = findCardInBoard(board, cardId);
        if (cardOpt.isEmpty()) {
            System.out.println("Card com ID " + cardId + " não encontrado.");
            return;
        }
        Card card = cardOpt.get();

        if (!card.isBlocked()) {
            System.out.println("Card ID " + cardId + " não está bloqueado.");
            return;
        }

        String motivo = lerEntrada("Digite o motivo do desbloqueio: ");
         if (motivo.trim().isEmpty()) {
            System.out.println("Motivo do desbloqueio é obrigatório.");
            return;
        }


        // Atualiza o status no card
        boolean updated = cardDAO.updateBlockedStatus(card.getId(), false);

        if (updated) {
             // Cria o evento de desbloqueio (Req 7 e Opcional 3)
              BlockEvent unblockEvent = new BlockEvent(card.getId(), BlockEvent.EventType.UNBLOCK, motivo);
              blockEventDAO.create(unblockEvent);
            System.out.println("Card ID " + cardId + " desbloqueado com sucesso.");
        } else {
            System.err.println("Falha ao atualizar o status de desbloqueio do card.");
        }
        aguardarEnter();
    }


    private static void excluirBoard() throws SQLException {
        System.out.println("\n--- EXCLUIR BOARD ---");
        List<Board> boards = boardDAO.findAll();
        if (boards.isEmpty()) {
            System.out.println("Nenhum board para excluir.");
            return;
        }

        System.out.println("Boards disponíveis para exclusão:");
        boards.forEach(b -> System.out.println("  ID: " + b.getId() + " | Nome: " + b.getName()));

        int boardId = lerInt("Digite o ID do board a ser excluído (ATENÇÃO: ISSO É IRREVERSÍVEL): ");

        Optional<Board> boardOpt = boards.stream().filter(b -> b.getId() == boardId).findFirst();
        if (boardOpt.isEmpty()) {
            System.out.println("Board com ID " + boardId + " não encontrado.");
            return;
        }

        String confirmacao = lerEntrada("Tem certeza que deseja excluir o board '" + boardOpt.get().getName() + "' e TODOS os seus dados? (s/N): ").toLowerCase();

        if (confirmacao.equals("s")) {
            boolean deleted = boardDAO.delete(boardId);
            if (deleted) {
                System.out.println("Board ID " + boardId + " excluído com sucesso.");
            } else {
                System.err.println("Falha ao excluir o board ID " + boardId + ".");
            }
        } else {
            System.out.println("Exclusão cancelada.");
        }
    }


    // --- Métodos Utilitários ---

     // Encontra um card dentro das colunas carregadas de um board
    private static Optional<Card> findCardInBoard(Board board, int cardId) {
        if (board.getColumns() == null) return Optional.empty();
        return board.getColumns().stream()
                .filter(col -> col.getCards() != null) // Garante que a lista de cards não é nula
                .flatMap(col -> col.getCards().stream()) // Junta os cards de todas as colunas
                .filter(card -> card.getId() == cardId) // Filtra pelo ID
                .findFirst(); // Pega o primeiro encontrado
    }

     // Encontra uma coluna pelo ID dentro das colunas carregadas de um board
    private static Optional<BoardColumn> findColumnById(Board board, int columnId) {
         if (board.getColumns() == null) return Optional.empty();
         return board.getColumns().stream()
                .filter(col -> col.getId() == columnId)
                .findFirst();
    }

     // Encontra a próxima coluna na sequência (maior posição, não sendo CANCELAMENTO)
    private static Optional<BoardColumn> findNextSequentialColumn(Board board, int currentPosition) {
        if (board.getColumns() == null) return Optional.empty();
        return board.getColumns().stream()
                .filter(col -> col.getPosition() > currentPosition && col.getType() != ColumnType.CANCELAMENTO)
                .min(Comparator.comparingInt(BoardColumn::getPosition)); // Pega a de menor posição > atual
    }


    private static String lerEntrada(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

     private static String lerEntradaComDefault(String prompt, String defaultValue) {
        System.out.print(prompt + " (Padrão: " + defaultValue + "): ");
        String input = scanner.nextLine();
        return input.trim().isEmpty() ? defaultValue : input.trim();
    }


    private static int lerInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, digite um número inteiro.");
            }
        }
    }

     private static void aguardarEnter() {
        System.out.print("\nPressione Enter para continuar...");
        scanner.nextLine();
    }
}