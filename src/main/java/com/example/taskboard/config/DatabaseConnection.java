package com.example.taskboard.config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Use variáveis de ambiente ou arquivos de configuração para dados sensíveis
    private static final String URL = "jdbc:mysql://localhost:3306/taskboard_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root"; // Substitua pelo seu usuário
    private static final String PASS = "password"; // Substitua pela sua senha

    // Opcional: Carregar o driver explicitamente (geralmente não necessário com JDBC 4+)
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Erro: Driver MySQL não encontrado. Verifique o classpath.");
            // Considerar lançar uma RuntimeException aqui para parar a aplicação se o driver não for encontrado.
            // throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}