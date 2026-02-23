package com.arian.bot;

import java.sql.*;

public class DataBaseManager {

    private static final String DB_URL = "jdbc:sqlite:arian.db";
    private static Connection connection;

    // Inicializa la conexión y crea las tablas si no existen
    public static void initialize() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("✅ Base de datos conectada correctamente.");
        } catch (SQLException e) {
            System.err.println("❌ Error al conectar la base de datos: " + e.getMessage());
        }
    }

    private static void createTables() throws SQLException {
        String pairTable = """
                CREATE TABLE IF NOT EXISTS pair_interactions (
                    user1_id TEXT NOT NULL,
                    user2_id TEXT NOT NULL,
                    action TEXT NOT NULL,
                    count INTEGER DEFAULT 0,
                    PRIMARY KEY (user1_id, user2_id, action)
                );
                """;

        String receivedTable = """
                CREATE TABLE IF NOT EXISTS received_interactions (
                    user_id TEXT NOT NULL,
                    action TEXT NOT NULL,
                    count INTEGER DEFAULT 0,
                    PRIMARY KEY (user_id, action)
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(pairTable);
            stmt.execute(receivedTable);
        }
    }

    // Incrementa y devuelve el contador entre dos personas (para kiss y hit)
    public static int incrementPairCount(String user1Id, String user2Id, String action) {
        // Ordenamos los IDs para que A->B y B->A cuenten igual
        String id1 = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        String id2 = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;

        String upsert = """
                INSERT INTO pair_interactions (user1_id, user2_id, action, count)
                VALUES (?, ?, ?, 1)
                ON CONFLICT(user1_id, user2_id, action)
                DO UPDATE SET count = count + 1;
                """;

        String select = "SELECT count FROM pair_interactions WHERE user1_id = ? AND user2_id = ? AND action = ?";

        try {
            try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                ps.setString(1, id1);
                ps.setString(2, id2);
                ps.setString(3, action);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(select)) {
                ps.setString(1, id1);
                ps.setString(2, id2);
                ps.setString(3, action);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en incrementPairCount: " + e.getMessage());
        }
        return 0;
    }

    // Incrementa y devuelve el contador de lo que ha recibido una persona (para hug y pat)
    public static int incrementReceivedCount(String userId, String action) {
        String upsert = """
                INSERT INTO received_interactions (user_id, action, count)
                VALUES (?, ?, 1)
                ON CONFLICT(user_id, action)
                DO UPDATE SET count = count + 1;
                """;

        String select = "SELECT count FROM received_interactions WHERE user_id = ? AND action = ?";

        try {
            try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                ps.setString(1, userId);
                ps.setString(2, action);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(select)) {
                ps.setString(1, userId);
                ps.setString(2, action);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en incrementReceivedCount: " + e.getMessage());
        }
        return 0;
    }
}