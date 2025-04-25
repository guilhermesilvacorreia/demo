# TaskBoard

TaskBoard é uma aplicação Java para gerenciamento de tarefas em um quadro Kanban. Ele permite criar, mover, bloquear e cancelar tarefas (cards) em colunas personalizadas, além de gerar relatórios de tempo de conclusão e bloqueios.

## Funcionalidades

- **Gerenciamento de Boards**:
  - Criar, selecionar e excluir boards.
  - Adicionar colunas personalizadas a cada board.

- **Gerenciamento de Cards**:
  - Criar novos cards em colunas iniciais.
  - Mover cards entre colunas.
  - Cancelar cards movendo-os para a coluna de cancelamento.
  - Bloquear e desbloquear cards com registro de eventos.

- **Relatórios**:
  - Gerar relatório de tempo de conclusão dos cards.
  - Gerar relatório de bloqueios e desbloqueios.

## Estrutura do Projeto

```
demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── taskboard/
│   │   │               ├── model/
│   │   │               ├── service/
│   │   │               ├── controller/
│   │   │               └── TaskBoardApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── taskboard/
│                       └── TaskBoardApplicationTests.java
├── README.md
├── pom.xml
└── .gitignore
```

## Pré-requisitos

- **Java 11** ou superior.
- **Maven** para gerenciamento de dependências.
- **MySQL** como banco de dados.

## Configuração do Banco de Dados

1. Crie um banco de dados MySQL chamado `taskboard_db`.
2. Configure as credenciais no arquivo           [`DatabaseConnection.java`](src/main/java/com/example/taskboard/config/DatabaseConnection.java):
   ```java
   private static final String URL = "jdbc:mysql://localhost:3306/taskboard_db?useSSL=false&serverTimezone=UTC";
   private static final String USER = "root"; // Substitua pelo seu usuário
   private static final String PASS = "password"; // Substitua pela sua senha
    ```


3. Execute o script SQL para criar as tabelas necessárias:
```
CREATE TABLE board (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE board_column (
    id INT AUTO_INCREMENT PRIMARY KEY,
    board_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    position INT NOT NULL,
    type ENUM('INICIAL', 'PENDENTE', 'FINAL', 'CANCELAMENTO') NOT NULL,
    FOREIGN KEY (board_id) REFERENCES board(id)
);

CREATE TABLE card (
    id INT AUTO_INCREMENT PRIMARY KEY,
    column_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    last_moved_at DATETIME,
    FOREIGN KEY (column_id) REFERENCES board_column(id)
);

CREATE TABLE block_event (
    id INT AUTO_INCREMENT PRIMARY KEY,
    card_id INT NOT NULL,
    event_type ENUM('BLOCK', 'UNBLOCK') NOT NULL,
    event_timestamp DATETIME NOT NULL,
    reason TEXT NOT NULL,
    FOREIGN KEY (card_id) REFERENCES card(id)
);

CREATE TABLE card_movement_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    card_id INT NOT NULL,
    from_column_id INT,
    to_column_id INT NOT NULL,
    moved_at DATETIME NOT NULL,
    FOREIGN KEY (card_id) REFERENCES card(id),
    FOREIGN KEY (from_column_id) REFERENCES board_column(id),
    FOREIGN KEY (to_column_id) REFERENCES board_column(id)
);
```

## Como Executar

1. Clone o repositório:
```
 git clone https://github.com/seu-usuario/taskboard.git
 cd taskboard
```
2. Compile o projeto com Maven:
```
 mvn clean package
```
3. Execute o JAR gerado:
```
 java -jar target/taskboard-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Dependências
As dependências do projeto estão listadas no arquivo pom.xml:
 - MySQL Connector: Para conexão com o banco de dados.
 - SLF4J e Logback: Para logging.
 - JUnit 5: Para testes unitários.
