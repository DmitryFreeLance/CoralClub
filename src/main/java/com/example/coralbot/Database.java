package com.example.coralbot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private final String url;

    public Database(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) { }

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {

            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        chat_id INTEGER UNIQUE NOT NULL,
                        username TEXT,
                        first_name TEXT,
                        last_name TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS media_cache (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        media_key TEXT UNIQUE NOT NULL,
                        media_type TEXT NOT NULL,
                        file_id TEXT NOT NULL
                    );
                    """);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** true, если пользователь добавлен впервые (защита от двойного /start) */
    public boolean insertUserIfNotExists(long chatId, String username, String firstName, String lastName) {
        String sql = """
                INSERT OR IGNORE INTO users (chat_id, username, first_name, last_name)
                VALUES (?, ?, ?, ?);
                """;
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, chatId);
            ps.setString(2, username);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public long countUsers() {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    public List<Long> getAllChatIds() {
        List<Long> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement("SELECT chat_id FROM users");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getLong("chat_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getMediaFileId(String mediaKey) {
        String sql = "SELECT file_id FROM media_cache WHERE media_key = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mediaKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("file_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveMediaFileId(String mediaKey, String mediaType, String fileId) {
        String sql = """
                INSERT INTO media_cache (media_key, media_type, file_id)
                VALUES (?, ?, ?)
                ON CONFLICT(media_key) DO UPDATE SET
                    media_type = excluded.media_type,
                    file_id = excluded.file_id;
                """;
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mediaKey);
            ps.setString(2, mediaType);
            ps.setString(3, fileId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}